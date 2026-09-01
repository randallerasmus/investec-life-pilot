package za.co.byteservices.moneycoach.model;

import java.time.LocalDate;

/**
 * How often a detected recurring payment repeats.
 *
 * <p>Each cadence carries the nominal gap in days used to classify observed
 * intervals, plus the tolerance band around it. A gap outside every band is
 * treated as irregular spending rather than a recurring payment.
 */
public enum RecurringCadence {

    WEEKLY(7, 5, 9),
    FORTNIGHTLY(14, 12, 17),
    MONTHLY(30, 25, 35),
    QUARTERLY(91, 80, 100),
    ANNUAL(365, 330, 400);

    private final int nominalDays;
    private final int minDays;
    private final int maxDays;

    RecurringCadence(int nominalDays, int minDays, int maxDays) {
        this.nominalDays = nominalDays;
        this.minDays = minDays;
        this.maxDays = maxDays;
    }

    public int getNominalDays() {
        return nominalDays;
    }

    /**
     * Advances a date by one cadence step.
     *
     * <p>Steps are calendar-aware rather than a fixed day count, so a debit order
     * on the 31st stays on month ends instead of drifting backwards.
     */
    public LocalDate next(LocalDate from) {
        return switch (this) {
            case WEEKLY -> from.plusWeeks(1);
            case FORTNIGHTLY -> from.plusWeeks(2);
            case MONTHLY -> from.plusMonths(1);
            case QUARTERLY -> from.plusMonths(3);
            case ANNUAL -> from.plusYears(1);
        };
    }

    /**
     * Classifies an observed median gap in days, or returns {@code null} when the
     * gap does not match any known cadence.
     */
    public static RecurringCadence fromDays(long days) {
        for (RecurringCadence cadence : values()) {
            if (days >= cadence.minDays && days <= cadence.maxDays) {
                return cadence;
            }
        }
        return null;
    }
}
