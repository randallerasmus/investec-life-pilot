package za.co.byteservices.moneycoach.service;

import org.springframework.stereotype.Service;
import za.co.byteservices.moneycoach.config.CardGuardrailProperties;
import za.co.byteservices.moneycoach.dto.CardAuthorizationRequest;
import za.co.byteservices.moneycoach.dto.CardAuthorizationResponse;
import za.co.byteservices.moneycoach.dto.SpendingSnapshot;
import za.co.byteservices.moneycoach.model.CardAssessment;
import za.co.byteservices.moneycoach.model.CardDecision;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

/**
 * Decides whether a card transaction fits the money that is actually free.
 *
 * <p>The question is not whether the balance covers the amount — a balance
 * covers plenty of things it has already been promised to. It is whether the
 * amount fits once the debit orders due before the next payday are set aside.
 * That is the forecast applied at the till rather than reported afterwards.
 *
 * <p>Every uncertain path approves. A guardrail that declines when it is unsure
 * fails in the direction that strands someone at a checkout, so absent data, a
 * stale snapshot, or an unrecognised account all stand aside.
 */
@Service
public class CardAuthorizationService {

    private final SpendingSnapshotService snapshotService;
    private final CardGuardrailProperties properties;

    public CardAuthorizationService(SpendingSnapshotService snapshotService,
                                    CardGuardrailProperties properties) {
        this.snapshotService = snapshotService;
        this.properties = properties;
    }

    public CardAuthorizationResponse authorize(CardAuthorizationRequest request) {
        return authorize(request, Instant.now());
    }

    public CardAuthorizationResponse authorize(CardAuthorizationRequest request, Instant now) {
        BigDecimal amount = amountOf(request);

        if (isEssential(request.getMerchantCategoryCode())) {
            return standAside(
                    CardAssessment.ESSENTIAL_CATEGORY,
                    amount,
                    String.format(Locale.US,
                            "Merchant category %s is treated as essential and is never declined.",
                            request.getMerchantCategoryCode())
            );
        }

        Optional<SpendingSnapshot> cached = snapshotService.current(request.getAccountId(), now);
        if (cached.isEmpty()) {
            return standAside(
                    CardAssessment.NO_FORECAST_DATA,
                    amount,
                    "No current spending snapshot for this account, so the guardrail stood aside."
            );
        }

        SpendingSnapshot snapshot = cached.get();
        BigDecimal headroom = snapshot.getDiscretionaryHeadroom();
        BigDecimal remaining = money(headroom.subtract(amount));
        long snapshotAge = Duration.between(snapshot.getGeneratedAt(), now).getSeconds();

        boolean fits = remaining.signum() >= 0;
        CardAssessment assessment = fits ? CardAssessment.WITHIN_HEADROOM : CardAssessment.EXCEEDS_HEADROOM;

        // Monitor mode records the verdict without acting on it, so the rule can
        // be watched against real spending before it is allowed to decline.
        boolean declining = !fits && !properties.isMonitorOnly();

        return new CardAuthorizationResponse(
                declining ? CardDecision.DECLINE : CardDecision.APPROVE,
                assessment,
                properties.isMonitorOnly(),
                amount,
                headroom,
                remaining,
                snapshot.getNextIncomeDate(),
                snapshot.getNextRiskDate(),
                snapshot.getRiskLevel(),
                snapshotAge,
                snapshot.isFallbackUsed(),
                reason(fits, amount, headroom, remaining, snapshot)
        );
    }

    private String reason(boolean fits,
                          BigDecimal amount,
                          BigDecimal headroom,
                          BigDecimal remaining,
                          SpendingSnapshot snapshot) {
        String payday = snapshot.getNextIncomeDate() != null
                ? String.format(Locale.US, " before income lands on %s", snapshot.getNextIncomeDate())
                : "";

        if (fits) {
            return String.format(Locale.US,
                    "%.2f fits the %.2f free to spend%s, leaving %.2f.",
                    amount, headroom, payday, remaining);
        }

        return String.format(Locale.US,
                "%.2f exceeds the %.2f free to spend%s by %.2f. The balance of %.2f is already committed to %.2f of upcoming debits.",
                amount, headroom, payday, remaining.abs(),
                snapshot.getAvailableBalance(), snapshot.getCommittedBeforeNextIncome());
    }

    private CardAuthorizationResponse standAside(CardAssessment assessment, BigDecimal amount, String reason) {
        return new CardAuthorizationResponse(
                CardDecision.APPROVE,
                assessment,
                properties.isMonitorOnly(),
                amount,
                null,
                null,
                null,
                null,
                null,
                0L,
                false,
                reason
        );
    }

    private boolean isEssential(String merchantCategoryCode) {
        return merchantCategoryCode != null
                && properties.getEssentialMerchantCategoryCodes().contains(merchantCategoryCode.trim());
    }

    private BigDecimal amountOf(CardAuthorizationRequest request) {
        return BigDecimal.valueOf(request.getCentsAmount())
                .movePointLeft(2)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal value) {
        return (value != null ? value : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
