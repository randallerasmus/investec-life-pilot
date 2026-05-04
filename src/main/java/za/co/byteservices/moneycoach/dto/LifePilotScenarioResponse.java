package za.co.byteservices.moneycoach.dto;

import za.co.byteservices.moneycoach.model.LifePilotScenarioType;
import za.co.byteservices.moneycoach.model.MoneyCoachRiskLevel;

import java.math.BigDecimal;
import java.util.List;

public class LifePilotScenarioResponse {

    private final String accountId;
    private final LifePilotScenarioType scenarioType;
    private final String scenarioName;
    private final BigDecimal availableBalance;
    private final BigDecimal currentSafeToSpend;
    private final BigDecimal projectedSafeToSpend;
    private final BigDecimal monthlyImpact;
    private final BigDecimal onceOffImpact;
    private final Integer durationMonths;
    private final String currency;
    private final MoneyCoachRiskLevel riskLevel;
    private final String summary;
    private final List<String> recommendations;
    private final String disclaimer;

    public LifePilotScenarioResponse(String accountId,
                                     LifePilotScenarioType scenarioType,
                                     String scenarioName,
                                     BigDecimal availableBalance,
                                     BigDecimal currentSafeToSpend,
                                     BigDecimal projectedSafeToSpend,
                                     BigDecimal monthlyImpact,
                                     BigDecimal onceOffImpact,
                                     Integer durationMonths,
                                     String currency,
                                     MoneyCoachRiskLevel riskLevel,
                                     String summary,
                                     List<String> recommendations,
                                     String disclaimer) {
        this.accountId = accountId;
        this.scenarioType = scenarioType;
        this.scenarioName = scenarioName;
        this.availableBalance = availableBalance;
        this.currentSafeToSpend = currentSafeToSpend;
        this.projectedSafeToSpend = projectedSafeToSpend;
        this.monthlyImpact = monthlyImpact;
        this.onceOffImpact = onceOffImpact;
        this.durationMonths = durationMonths;
        this.currency = currency;
        this.riskLevel = riskLevel;
        this.summary = summary;
        this.recommendations = recommendations;
        this.disclaimer = disclaimer;
    }

    public String getAccountId() {
        return accountId;
    }

    public LifePilotScenarioType getScenarioType() {
        return scenarioType;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public BigDecimal getCurrentSafeToSpend() {
        return currentSafeToSpend;
    }

    public BigDecimal getProjectedSafeToSpend() {
        return projectedSafeToSpend;
    }

    public BigDecimal getMonthlyImpact() {
        return monthlyImpact;
    }

    public BigDecimal getOnceOffImpact() {
        return onceOffImpact;
    }

    public Integer getDurationMonths() {
        return durationMonths;
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

    public String getDisclaimer() {
        return disclaimer;
    }
}
