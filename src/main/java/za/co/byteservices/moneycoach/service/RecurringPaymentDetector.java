package za.co.byteservices.moneycoach.service;

import org.springframework.stereotype.Service;
import za.co.byteservices.moneycoach.dto.InvestecTransactionResponse;
import za.co.byteservices.moneycoach.dto.RecurringPayment;
import za.co.byteservices.moneycoach.model.RecurringCadence;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Finds individual recurring items in Investec transaction history: outflows via
 * {@link #detect}, and inflows such as salary via {@link #detectIncome}.
 *
 * <p>Transactions are grouped by a normalised merchant key, then each group is
 * accepted only when its payment dates fall on a recognised cadence and its
 * amounts are stable. The result names the merchant, the expected amount, the
 * cadence, and the next due date, which is what a forward balance projection
 * needs in order to place future debits on a calendar.
 *
 * <p>The detector is deterministic and does no I/O, so callers supply both the
 * transactions and the date to project from.
 */
@Service
public class RecurringPaymentDetector {

    /** A single gap gives no evidence of regularity, so two dates are the floor. */
    private static final int MIN_OCCURRENCES = 2;

    private static final BigDecimal MIN_CONFIDENCE = new BigDecimal("0.50");

    /** Beyond this relative spread the group is variable spending, not a fixed payment. */
    private static final BigDecimal MAX_AMOUNT_DEVIATION = new BigDecimal("0.25");

    /**
     * A payment unseen for more than this multiple of its own cadence is treated as
     * cancelled, so a forecast does not keep charging for a dead subscription.
     */
    private static final int STALE_CADENCE_MULTIPLE = 2;

    private static final int MAX_KEY_TOKENS = 3;

    public List<RecurringPayment> detect(InvestecTransactionResponse response) {
        return detect(response, LocalDate.now());
    }

    public List<RecurringPayment> detect(InvestecTransactionResponse response, LocalDate asOf) {
        return detect(transactionsOf(response), asOf);
    }

    public List<RecurringPayment> detect(List<InvestecTransactionResponse.Transaction> transactions, LocalDate asOf) {
        return detect(transactions, asOf, Direction.OUT);
    }

    /**
     * Finds recurring inflows such as salary, using the same cadence and stability
     * rules as outflows. A forecast needs both sides or every balance trends to zero.
     */
    public List<RecurringPayment> detectIncome(InvestecTransactionResponse response, LocalDate asOf) {
        return detectIncome(transactionsOf(response), asOf);
    }

    public List<RecurringPayment> detectIncome(List<InvestecTransactionResponse.Transaction> transactions, LocalDate asOf) {
        return detect(transactions, asOf, Direction.IN);
    }

    private List<RecurringPayment> detect(List<InvestecTransactionResponse.Transaction> transactions,
                                          LocalDate asOf,
                                          Direction direction) {
        if (transactions == null || transactions.isEmpty()) {
            return List.of();
        }

        LocalDate today = asOf != null ? asOf : LocalDate.now();
        Map<String, List<Charge>> groups = groupByMerchant(transactions, direction);

        List<RecurringPayment> detected = new ArrayList<>();
        for (List<Charge> charges : groups.values()) {
            RecurringPayment payment = evaluateGroup(charges, today);
            if (payment != null) {
                detected.add(payment);
            }
        }

        detected.sort(Comparator.comparing(RecurringPayment::getNextDueDate)
                .thenComparing(RecurringPayment::getMerchant));
        return detected;
    }

    private List<InvestecTransactionResponse.Transaction> transactionsOf(InvestecTransactionResponse response) {
        if (response == null || response.getData() == null || response.getData().getTransactions() == null) {
            return List.of();
        }
        return response.getData().getTransactions();
    }

    private Map<String, List<Charge>> groupByMerchant(List<InvestecTransactionResponse.Transaction> transactions,
                                                      Direction direction) {
        Map<String, List<Charge>> groups = new LinkedHashMap<>();
        for (InvestecTransactionResponse.Transaction transaction : transactions) {
            if (transaction == null) {
                continue;
            }

            BigDecimal amount = direction == Direction.OUT
                    ? TransactionAmounts.debitAmount(transaction)
                    : TransactionAmounts.creditAmount(transaction);
            if (amount.signum() == 0) {
                continue;
            }

            LocalDate date = TransactionDates.parse(transaction);
            if (date == null) {
                continue;
            }

            String key = merchantKey(transaction.getDescription());
            if (key.isBlank()) {
                continue;
            }

            groups.computeIfAbsent(key, unused -> new ArrayList<>())
                    .add(new Charge(date, amount, safeDescription(transaction.getDescription())));
        }
        return groups;
    }

    private RecurringPayment evaluateGroup(List<Charge> charges, LocalDate today) {
        if (charges.size() < MIN_OCCURRENCES) {
            return null;
        }

        charges.sort(Comparator.comparing(charge -> charge.date));

        List<Long> gaps = new ArrayList<>();
        for (int i = 1; i < charges.size(); i++) {
            long gap = ChronoUnit.DAYS.between(charges.get(i - 1).date, charges.get(i).date);
            // Two charges on the same day are one payment split by the bank, not a cadence.
            if (gap > 0) {
                gaps.add(gap);
            }
        }
        if (gaps.isEmpty()) {
            return null;
        }

        long medianGap = medianLong(gaps);
        RecurringCadence cadence = RecurringCadence.fromDays(medianGap);
        if (cadence == null) {
            return null;
        }

        List<BigDecimal> amounts = charges.stream().map(charge -> charge.amount).toList();
        BigDecimal expectedAmount = medianAmount(amounts);
        if (expectedAmount.signum() <= 0) {
            return null;
        }

        BigDecimal amountDeviation = maxRelativeDeviation(amounts, expectedAmount);
        if (amountDeviation.compareTo(MAX_AMOUNT_DEVIATION) > 0) {
            return null;
        }

        LocalDate firstSeen = charges.get(0).date;
        LocalDate lastSeen = charges.get(charges.size() - 1).date;
        if (isStale(lastSeen, cadence, today)) {
            return null;
        }

        BigDecimal confidence = confidence(charges.size(), gaps, medianGap, amountDeviation);
        if (confidence.compareTo(MIN_CONFIDENCE) < 0) {
            return null;
        }

        BigDecimal amountDrift = charges.get(charges.size() - 1).amount
                .subtract(charges.get(0).amount)
                .setScale(2, RoundingMode.HALF_UP);

        return new RecurringPayment(
                dominantDescription(charges),
                expectedAmount,
                cadence,
                charges.size(),
                firstSeen,
                lastSeen,
                nextDueDate(lastSeen, cadence, today),
                (int) medianGap,
                amountDrift,
                confidence
        );
    }

    /**
     * Confidence blends three signals: how many times the payment was seen, how
     * evenly spaced those sightings were, and how stable the amount stayed.
     */
    private BigDecimal confidence(int occurrences, List<Long> gaps, long medianGap, BigDecimal amountDeviation) {
        BigDecimal base = switch (Math.min(occurrences, 5)) {
            case 2 -> new BigDecimal("0.50");
            case 3 -> new BigDecimal("0.70");
            case 4 -> new BigDecimal("0.85");
            default -> new BigDecimal("0.95");
        };

        BigDecimal gapPenalty = BigDecimal.ZERO;
        if (medianGap > 0 && gaps.size() > 1) {
            BigDecimal totalDeviation = BigDecimal.ZERO;
            for (Long gap : gaps) {
                totalDeviation = totalDeviation.add(BigDecimal.valueOf(Math.abs(gap - medianGap)));
            }
            BigDecimal meanDeviation = totalDeviation.divide(BigDecimal.valueOf(gaps.size()), 4, RoundingMode.HALF_UP);
            gapPenalty = meanDeviation.divide(BigDecimal.valueOf(medianGap), 4, RoundingMode.HALF_UP)
                    .min(new BigDecimal("0.40"));
        }

        BigDecimal amountPenalty = amountDeviation.min(new BigDecimal("0.30"));

        BigDecimal score = base
                .multiply(BigDecimal.ONE.subtract(gapPenalty))
                .multiply(BigDecimal.ONE.subtract(amountPenalty));

        return score.max(BigDecimal.ZERO).min(BigDecimal.ONE).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isStale(LocalDate lastSeen, RecurringCadence cadence, LocalDate today) {
        long daysSinceLastSeen = ChronoUnit.DAYS.between(lastSeen, today);
        return daysSinceLastSeen > (long) cadence.getNominalDays() * STALE_CADENCE_MULTIPLE;
    }

    private LocalDate nextDueDate(LocalDate lastSeen, RecurringCadence cadence, LocalDate today) {
        LocalDate next = cadence.next(lastSeen);
        // History can end just short of a full cadence before today; roll forward so
        // the projected date is always one a forecast can actually place ahead.
        while (next.isBefore(today)) {
            next = cadence.next(next);
        }
        return next;
    }

    /**
     * Reduces a bank description to a stable grouping key by dropping punctuation
     * and reference or card numbers, then keeping the leading tokens. This lets
     * "NETFLIX.COM 4392" and "Netflix.com  8811" fall into the same group while
     * keeping genuinely different merchants apart.
     */
    private String merchantKey(String description) {
        if (description == null) {
            return "";
        }

        String cleaned = description.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", " ").trim();
        if (cleaned.isEmpty()) {
            return "";
        }

        List<String> tokens = new ArrayList<>();
        for (String token : cleaned.split(" ")) {
            if (token.isBlank() || token.matches(".*\\d{3,}.*") || token.matches("\\d+")) {
                continue;
            }
            tokens.add(token);
            if (tokens.size() == MAX_KEY_TOKENS) {
                break;
            }
        }

        return String.join(" ", tokens);
    }

    private String dominantDescription(List<Charge> charges) {
        Map<String, Integer> counts = new HashMap<>();
        for (Charge charge : charges) {
            counts.merge(charge.description, 1, Integer::sum);
        }
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(charges.get(0).description);
    }

    private String safeDescription(String description) {
        return description == null ? "" : description.trim();
    }

    private long medianLong(List<Long> values) {
        List<Long> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        return sorted.get(sorted.size() / 2);
    }

    private BigDecimal medianAmount(List<BigDecimal> values) {
        List<BigDecimal> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        return sorted.get(sorted.size() / 2).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal maxRelativeDeviation(List<BigDecimal> amounts, BigDecimal median) {
        BigDecimal worst = BigDecimal.ZERO;
        for (BigDecimal amount : amounts) {
            BigDecimal deviation = amount.subtract(median).abs()
                    .divide(median, 4, RoundingMode.HALF_UP);
            worst = worst.max(deviation);
        }
        return worst;
    }

    /** Which side of the ledger a detection pass looks at. */
    private enum Direction {
        IN,
        OUT
    }

    private static final class Charge {
        private final LocalDate date;
        private final BigDecimal amount;
        private final String description;

        private Charge(LocalDate date, BigDecimal amount, String description) {
            this.date = date;
            this.amount = amount;
            this.description = description;
        }
    }
}
