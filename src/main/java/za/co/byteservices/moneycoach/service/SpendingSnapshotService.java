package za.co.byteservices.moneycoach.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import za.co.byteservices.moneycoach.config.CardGuardrailProperties;
import za.co.byteservices.moneycoach.dto.BalanceForecastRequest;
import za.co.byteservices.moneycoach.dto.BalanceForecastResponse;
import za.co.byteservices.moneycoach.dto.RecurringPayment;
import za.co.byteservices.moneycoach.dto.SpendingSnapshot;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Keeps a current {@link SpendingSnapshot} per account.
 *
 * <p>A card authorisation cannot wait for a forecast, so it never triggers one.
 * Snapshots are built off the swipe path — by the refresh endpoint, or by the
 * card {@code afterTransaction} hook, which has a fifteen second window instead
 * of two — and authorisation reads whatever is cached.
 *
 * <p>A miss returns empty rather than building on demand. Answering late is
 * worse than not answering: the card runtime would time out and the guardrail
 * would have made the decision it was trying to avoid.
 *
 * <p>Refreshing only after a swipe is not enough on its own: the next swipe is
 * usually hours later, long after the snapshot has expired, so the guardrail
 * would stand aside on almost every purchase. Every account seen once is
 * therefore rebuilt on a schedule inside its time to live.
 */
@Service
public class SpendingSnapshotService {

    /** Enough horizon to see the next payday and the debits before it. */
    private static final int SNAPSHOT_HORIZON_DAYS = 45;

    private final BalanceForecastService balanceForecastService;
    private final CardGuardrailProperties properties;
    private final Map<String, SpendingSnapshot> snapshots = new ConcurrentHashMap<>();

    public SpendingSnapshotService(BalanceForecastService balanceForecastService,
                                   CardGuardrailProperties properties) {
        this.balanceForecastService = balanceForecastService;
        this.properties = properties;
    }

    /**
     * The cached snapshot for an account, if one is present and still inside its
     * time to live. Never builds one.
     */
    public Optional<SpendingSnapshot> current(String accountId, Instant now) {
        SpendingSnapshot snapshot = snapshots.get(accountId);
        if (snapshot == null) {
            return Optional.empty();
        }

        long age = Duration.between(snapshot.getGeneratedAt(), now).getSeconds();
        if (age > properties.getSnapshotTtlSeconds()) {
            return Optional.empty();
        }

        return Optional.of(snapshot);
    }

    /** Builds a snapshot from a fresh forecast and caches it. Slow by nature. */
    public SpendingSnapshot refresh(String accountId, Instant now) {
        SpendingSnapshot snapshot = build(accountId, now);
        snapshots.put(accountId, snapshot);
        return snapshot;
    }

    @Scheduled(
            initialDelayString = "${lifepilot.card.snapshot-refresh-seconds:240}",
            fixedDelayString = "${lifepilot.card.snapshot-refresh-seconds:240}",
            timeUnit = TimeUnit.SECONDS)
    public void refreshKnownAccounts() {
        refreshKnownAccounts(Instant.now());
    }

    /**
     * Rebuilds every cached account so a swipe finds a live snapshot however long
     * it has been since the last one.
     *
     * <p>A build that fell back because Investec was unreachable reports an opening
     * balance of zero, and zero headroom declines everything. It never replaces a
     * good snapshot: the good one is kept and allowed to age out, after which the
     * guardrail stands aside instead of declining on data it does not have.
     */
    public void refreshKnownAccounts(Instant now) {
        for (String accountId : List.copyOf(snapshots.keySet())) {
            try {
                SpendingSnapshot rebuilt = build(accountId, now);
                SpendingSnapshot previous = snapshots.get(accountId);
                if (!rebuilt.isFallbackUsed() || previous == null || previous.isFallbackUsed()) {
                    snapshots.put(accountId, rebuilt);
                }
            } catch (RuntimeException ex) {
                // One account failing must not stop the others being kept warm.
            }
        }
    }

    private SpendingSnapshot build(String accountId, Instant now) {
        LocalDate today = LocalDate.ofInstant(now, java.time.ZoneOffset.UTC);

        BalanceForecastResponse forecast = balanceForecastService.forecast(
                accountId,
                new BalanceForecastRequest(SNAPSHOT_HORIZON_DAYS, null, null, null, true),
                today
        );

        LocalDate nextIncomeDate = forecast.getRecurringIncome().stream()
                .map(RecurringPayment::getNextDueDate)
                .min(Comparator.naturalOrder())
                .orElse(null);

        // With no detected payday there is nothing to count towards, so fall back
        // to a fixed horizon rather than treating every future debit as committed.
        LocalDate commitmentCutoff = nextIncomeDate != null
                ? nextIncomeDate
                : today.plusDays(properties.getFallbackHorizonDays());

        BigDecimal committed = totalDueBy(forecast.getRecurringExpenses(), commitmentCutoff);
        BigDecimal buffer = money(properties.getEmergencyBuffer());
        BigDecimal headroom = money(forecast.getOpeningBalance()
                .subtract(committed)
                .subtract(buffer)
                .max(BigDecimal.ZERO));

        LocalDate nextRiskDate = forecast.getRisks().isEmpty()
                ? null
                : forecast.getRisks().get(0).getStartDate();

        return new SpendingSnapshot(
                accountId,
                now,
                money(forecast.getOpeningBalance()),
                committed,
                buffer,
                headroom,
                nextIncomeDate,
                forecast.getRiskLevel(),
                nextRiskDate,
                forecast.isFallbackUsed()
        );
    }

    private BigDecimal totalDueBy(List<RecurringPayment> payments, LocalDate cutoff) {
        BigDecimal total = BigDecimal.ZERO;
        for (RecurringPayment payment : payments) {
            if (!payment.getNextDueDate().isAfter(cutoff)) {
                total = total.add(payment.getExpectedAmount());
            }
        }
        return money(total);
    }

    private BigDecimal money(BigDecimal value) {
        return (value != null ? value : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
