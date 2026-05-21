package za.co.byteservices.moneycoach.dto;

import za.co.byteservices.moneycoach.model.MoneyCoachRiskLevel;

import java.math.BigDecimal;

public class AdvancedSafeToSpendResponse {

    private final String accountId;
    private final BigDecimal currentBalance;
    private final BigDecimal estimatedRecurringExpenses;
    private final BigDecimal emergencyBuffer;
    private final int daysUntilPayday;
    private final BigDecimal plannedPurchaseAmount;
    private final BigDecimal safeToSpend;
    private final String affordabilityStatus;
    private final MoneyCoachRiskLevel riskLevel;
    private final String explanation;

    public AdvancedSafeToSpendResponse(String accountId,
                                       BigDecimal currentBalance,
                                       BigDecimal estimatedRecurringExpenses,
                                       BigDecimal emergencyBuffer,
                                       int daysUntilPayday,
                                       BigDecimal plannedPurchaseAmount,
                                       BigDecimal safeToSpend,
                                       String affordabilityStatus,
                                       MoneyCoachRiskLevel riskLevel,
                                       String explanation) {
        this.accountId = accountId;
        this.currentBalance = currentBalance;
        this.estimatedRecurringExpenses = estimatedRecurringExpenses;
        this.emergencyBuffer = emergencyBuffer;
        this.daysUntilPayday = daysUntilPayday;
        this.plannedPurchaseAmount = plannedPurchaseAmount;
        this.safeToSpend = safeToSpend;
        this.affordabilityStatus = affordabilityStatus;
        this.riskLevel = riskLevel;
        this.explanation = explanation;
    }

    public String getAccountId() {
        return accountId;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public BigDecimal getEstimatedRecurringExpenses() {
        return estimatedRecurringExpenses;
    }

    public BigDecimal getEmergencyBuffer() {
        return emergencyBuffer;
    }

    public int getDaysUntilPayday() {
        return daysUntilPayday;
    }

    public BigDecimal getPlannedPurchaseAmount() {
        return plannedPurchaseAmount;
    }

    public BigDecimal getSafeToSpend() {
        return safeToSpend;
    }

    public String getAffordabilityStatus() {
        return affordabilityStatus;
    }

    public MoneyCoachRiskLevel getRiskLevel() {
        return riskLevel;
    }

    public String getExplanation() {
        return explanation;
    }
}
