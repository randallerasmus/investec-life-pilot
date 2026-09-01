package za.co.byteservices.moneycoach.dto;

import za.co.byteservices.moneycoach.model.RecurringCadence;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One recurring outflow inferred from transaction history: a debit order,
 * subscription, or any other payment that repeats on a predictable cadence.
 *
 * <p>Amounts are positive magnitudes, not signed transaction amounts.
 */
public class RecurringPayment {

    private final String merchant;
    private final BigDecimal expectedAmount;
    private final RecurringCadence cadence;
    private final int occurrences;
    private final LocalDate firstSeen;
    private final LocalDate lastSeen;
    private final LocalDate nextDueDate;
    private final int averageDaysBetween;
    private final BigDecimal amountDrift;
    private final BigDecimal confidence;

    public RecurringPayment(String merchant,
                            BigDecimal expectedAmount,
                            RecurringCadence cadence,
                            int occurrences,
                            LocalDate firstSeen,
                            LocalDate lastSeen,
                            LocalDate nextDueDate,
                            int averageDaysBetween,
                            BigDecimal amountDrift,
                            BigDecimal confidence) {
        this.merchant = merchant;
        this.expectedAmount = expectedAmount;
        this.cadence = cadence;
        this.occurrences = occurrences;
        this.firstSeen = firstSeen;
        this.lastSeen = lastSeen;
        this.nextDueDate = nextDueDate;
        this.averageDaysBetween = averageDaysBetween;
        this.amountDrift = amountDrift;
        this.confidence = confidence;
    }

    /** Display label, taken from the most common raw description in the group. */
    public String getMerchant() {
        return merchant;
    }

    /** Median observed amount, as a positive value. */
    public BigDecimal getExpectedAmount() {
        return expectedAmount;
    }

    public RecurringCadence getCadence() {
        return cadence;
    }

    public int getOccurrences() {
        return occurrences;
    }

    public LocalDate getFirstSeen() {
        return firstSeen;
    }

    public LocalDate getLastSeen() {
        return lastSeen;
    }

    /** Projected date of the next charge, always on or after the detection date. */
    public LocalDate getNextDueDate() {
        return nextDueDate;
    }

    public int getAverageDaysBetween() {
        return averageDaysBetween;
    }

    /**
     * Most recent amount minus the earliest amount. Positive means the payment has
     * increased over the observed window, for example a subscription price rise.
     */
    public BigDecimal getAmountDrift() {
        return amountDrift;
    }

    /** Detection confidence from 0.00 to 1.00. */
    public BigDecimal getConfidence() {
        return confidence;
    }
}
