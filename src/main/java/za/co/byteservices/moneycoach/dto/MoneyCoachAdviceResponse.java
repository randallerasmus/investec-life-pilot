package za.co.byteservices.moneycoach.dto;

import za.co.byteservices.moneycoach.model.MoneyCoachRiskLevel;

import java.math.BigDecimal;
import java.util.List;

public class MoneyCoachAdviceResponse {

    private final String accountId;
    private final BigDecimal availableBalance;
    private final BigDecimal estimatedBills;
    private final BigDecimal goalSavingAmount;
    private final BigDecimal safeToSpend;
    private final String currency;
    private final MoneyCoachRiskLevel riskLevel;
    private final String summary;
    private final List<String> recommendations;
    private final boolean aiGenerated;
    private final String disclaimer;

    public MoneyCoachAdviceResponse(String accountId,
                                    BigDecimal availableBalance,
                                    BigDecimal estimatedBills,
                                    BigDecimal goalSavingAmount,
                                    BigDecimal safeToSpend,
                                    String currency,
                                    MoneyCoachRiskLevel riskLevel,
                                    String summary,
                                    List<String> recommendations,
                                    boolean aiGenerated,
                                    String disclaimer) {
        this.accountId = accountId;
        this.availableBalance = availableBalance;
        this.estimatedBills = estimatedBills;
        this.goalSavingAmount = goalSavingAmount;
        this.safeToSpend = safeToSpend;
        this.currency = currency;
        this.riskLevel = riskLevel;
        this.summary = summary;
        this.recommendations = recommendations;
        this.aiGenerated = aiGenerated;
        this.disclaimer = disclaimer;
    }

    public MoneyCoachAdviceResponse withAiSummary(String aiSummary) {
        return new MoneyCoachAdviceResponse(
                accountId,
                availableBalance,
                estimatedBills,
                goalSavingAmount,
                safeToSpend,
                currency,
                riskLevel,
                aiSummary,
                recommendations,
                true,
                disclaimer
        );
    }

    public String getAccountId() {
        return accountId;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public BigDecimal getEstimatedBills() {
        return estimatedBills;
    }

    public BigDecimal getGoalSavingAmount() {
        return goalSavingAmount;
    }

    public BigDecimal getSafeToSpend() {
        return safeToSpend;
    }

    public String getCurrency() {
        return currency;
    }

    public MoneyCoachRiskLevel getRiskLevel() {
        return riskLevel;
    }

    public String getSummary() {
        return summary;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public boolean isAiGenerated() {
        return aiGenerated;
    }

    public String getDisclaimer() {
        return disclaimer;
    }
}
