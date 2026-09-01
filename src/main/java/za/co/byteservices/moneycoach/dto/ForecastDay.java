package za.co.byteservices.moneycoach.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * One day on a projected balance curve.
 *
 * <p>{@code events} names the recurring items that landed on this day, so a chart
 * tooltip can explain a step in the curve rather than showing an unexplained drop.
 */
public class ForecastDay {

    private final LocalDate date;
    private final BigDecimal openingBalance;
    private final BigDecimal inflows;
    private final BigDecimal outflows;
    private final BigDecimal closingBalance;
    private final List<String> events;

    public ForecastDay(LocalDate date,
                       BigDecimal openingBalance,
                       BigDecimal inflows,
                       BigDecimal outflows,
                       BigDecimal closingBalance,
                       List<String> events) {
        this.date = date;
        this.openingBalance = openingBalance;
        this.inflows = inflows;
        this.outflows = outflows;
        this.closingBalance = closingBalance;
        this.events = events;
    }

    public LocalDate getDate() {
        return date;
    }

    public BigDecimal getOpeningBalance() {
        return openingBalance;
    }

    public BigDecimal getInflows() {
        return inflows;
    }

    public BigDecimal getOutflows() {
        return outflows;
    }

    public BigDecimal getClosingBalance() {
        return closingBalance;
    }

    public List<String> getEvents() {
        return events;
    }
}
