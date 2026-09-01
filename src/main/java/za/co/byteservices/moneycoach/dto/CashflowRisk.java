package za.co.byteservices.moneycoach.dto;

import za.co.byteservices.moneycoach.model.MoneyCoachRiskLevel;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A stretch of days where the projected balance falls below a threshold.
 *
 * <p>Consecutive at-risk days are reported as one window rather than one entry
 * per day, so a month-end squeeze reads as a single warning with a date range.
 */
public class CashflowRisk {

    private final LocalDate startDate;
    private final LocalDate endDate;
    private final int daysAffected;
    private final BigDecimal lowestBalance;
    private final LocalDate lowestBalanceDate;
    private final MoneyCoachRiskLevel severity;
    private final String message;

    public CashflowRisk(LocalDate startDate,
                        LocalDate endDate,
                        int daysAffected,
                        BigDecimal lowestBalance,
                        LocalDate lowestBalanceDate,
                        MoneyCoachRiskLevel severity,
                        String message) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.daysAffected = daysAffected;
        this.lowestBalance = lowestBalance;
        this.lowestBalanceDate = lowestBalanceDate;
        this.severity = severity;
        this.message = message;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public int getDaysAffected() {
        return daysAffected;
    }

    public BigDecimal getLowestBalance() {
        return lowestBalance;
    }

    public LocalDate getLowestBalanceDate() {
        return lowestBalanceDate;
    }

    public MoneyCoachRiskLevel getSeverity() {
        return severity;
    }

    public String getMessage() {
        return message;
    }
}
