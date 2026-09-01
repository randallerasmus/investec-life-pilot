package za.co.byteservices.moneycoach.dto;

import za.co.byteservices.moneycoach.model.MoneyCoachRiskLevel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * A forward balance projection: the day-by-day curve, the dated points where it
 * runs into trouble, and the recurring items that shaped it.
 */
public class BalanceForecastResponse {

    private final String accountId;
    private final LocalDate generatedOn;
    private final int horizonDays;
    private final BigDecimal openingBalance;
    private final BigDecimal projectedClosingBalance;
    private final BigDecimal lowestProjectedBalance;
    private final LocalDate lowestProjectedBalanceDate;
    private final MoneyCoachRiskLevel riskLevel;
    private final BigDecimal detectedMonthlyIncome;
    private final BigDecimal detectedMonthlyRecurringExpenses;
    private final BigDecimal averageDailyDiscretionarySpend;
    private final List<RecurringPayment> recurringExpenses;
    private final List<RecurringPayment> recurringIncome;
    private final List<CashflowRisk> risks;
    private final List<ForecastDay> timeline;
    private final List<String> assumptions;
    private final String summary;
    private final boolean fallbackUsed;

    public BalanceForecastResponse(String accountId,
                                   LocalDate generatedOn,
                                   int horizonDays,
                                   BigDecimal openingBalance,
                                   BigDecimal projectedClosingBalance,
                                   BigDecimal lowestProjectedBalance,
                                   LocalDate lowestProjectedBalanceDate,
                                   MoneyCoachRiskLevel riskLevel,
                                   BigDecimal detectedMonthlyIncome,
                                   BigDecimal detectedMonthlyRecurringExpenses,
                                   BigDecimal averageDailyDiscretionarySpend,
                                   List<RecurringPayment> recurringExpenses,
                                   List<RecurringPayment> recurringIncome,
                                   List<CashflowRisk> risks,
                                   List<ForecastDay> timeline,
                                   List<String> assumptions,
                                   String summary,
                                   boolean fallbackUsed) {
        this.accountId = accountId;
        this.generatedOn = generatedOn;
        this.horizonDays = horizonDays;
        this.openingBalance = openingBalance;
        this.projectedClosingBalance = projectedClosingBalance;
        this.lowestProjectedBalance = lowestProjectedBalance;
        this.lowestProjectedBalanceDate = lowestProjectedBalanceDate;
        this.riskLevel = riskLevel;
        this.detectedMonthlyIncome = detectedMonthlyIncome;
        this.detectedMonthlyRecurringExpenses = detectedMonthlyRecurringExpenses;
        this.averageDailyDiscretionarySpend = averageDailyDiscretionarySpend;
        this.recurringExpenses = recurringExpenses;
        this.recurringIncome = recurringIncome;
        this.risks = risks;
        this.timeline = timeline;
        this.assumptions = assumptions;
        this.summary = summary;
        this.fallbackUsed = fallbackUsed;
    }

    public String getAccountId() {
        return accountId;
    }

    public LocalDate getGeneratedOn() {
        return generatedOn;
    }

    public int getHorizonDays() {
        return horizonDays;
    }

    public BigDecimal getOpeningBalance() {
        return openingBalance;
    }

    public BigDecimal getProjectedClosingBalance() {
        return projectedClosingBalance;
    }

    public BigDecimal getLowestProjectedBalance() {
        return lowestProjectedBalance;
    }

    public LocalDate getLowestProjectedBalanceDate() {
        return lowestProjectedBalanceDate;
    }

    public MoneyCoachRiskLevel getRiskLevel() {
        return riskLevel;
    }

    /** Detected recurring income normalised to a monthly figure. */
    public BigDecimal getDetectedMonthlyIncome() {
        return detectedMonthlyIncome;
    }

    /** Detected recurring expenses normalised to a monthly figure. */
    public BigDecimal getDetectedMonthlyRecurringExpenses() {
        return detectedMonthlyRecurringExpenses;
    }

    public BigDecimal getAverageDailyDiscretionarySpend() {
        return averageDailyDiscretionarySpend;
    }

    public List<RecurringPayment> getRecurringExpenses() {
        return recurringExpenses;
    }

    public List<RecurringPayment> getRecurringIncome() {
        return recurringIncome;
    }

    public List<CashflowRisk> getRisks() {
        return risks;
    }

    public List<ForecastDay> getTimeline() {
        return timeline;
    }

    /** Plain-language statement of what the projection assumed, for disclosure. */
    public List<String> getAssumptions() {
        return assumptions;
    }

    public String getSummary() {
        return summary;
    }

    public boolean isFallbackUsed() {
        return fallbackUsed;
    }
}
