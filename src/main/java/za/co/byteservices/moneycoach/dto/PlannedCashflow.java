package za.co.byteservices.moneycoach.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A debit that has not happened yet and is not in the history: the cost of a
 * decision being weighed. It repeats monthly from {@code firstDate} for
 * {@code occurrences} months, so a once-off cost is simply one occurrence.
 */
public class PlannedCashflow {

    private final String label;
    private final BigDecimal amount;
    private final LocalDate firstDate;
    private final int occurrences;

    public PlannedCashflow(String label, BigDecimal amount, LocalDate firstDate, int occurrences) {
        this.label = label;
        this.amount = amount;
        this.firstDate = firstDate;
        this.occurrences = occurrences;
    }

    public String getLabel() {
        return label;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getFirstDate() {
        return firstDate;
    }

    public int getOccurrences() {
        return occurrences;
    }
}
