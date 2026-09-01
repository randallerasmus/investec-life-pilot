package za.co.byteservices.moneycoach.dto;

import za.co.byteservices.moneycoach.model.LifePilotScenarioType;
import za.co.byteservices.moneycoach.model.MoneyCoachRiskLevel;
import za.co.byteservices.moneycoach.model.SurvivalStatus;

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
    private final SurvivalStatus survivalStatus;
    private final BigDecimal monthlyBufferAfterScenario;
    private final BigDecimal monthlyShortfall;
    private final BigDecimal safeToSpendDropPercent;
    private final String summary;
    private final String survivalMessage;
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
                                     SurvivalStatus survivalStatus,
                                     BigDecimal monthlyBufferAfterScenario,
                                     BigDecimal monthlyShortfall,
                                     BigDecimal safeToSpendDropPercent,
                                     String summary,
                                     String survivalMessage,
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
        this.survivalStatus = survivalStatus;
        this.monthlyBufferAfterScenario = monthlyBufferAfterScenario;
        this.monthlyShortfall = monthlyShortfall;
        this.safeToSpendDropPercent = safeToSpendDropPercent;
        this.summary = summary;
        this.survivalMessage = survivalMessage;
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

    public SurvivalStatus getSurvivalStatus() {
        return survivalStatus;
    }

    /** Monthly room left over once the scenario is paid for, or zero when it does not fit. */
    public BigDecimal getMonthlyBufferAfterScenario() {
        return monthlyBufferAfterScenario;
    }

    /** Monthly gap the scenario opens up, or zero when it fits. */
    public BigDecimal getMonthlyShortfall() {
        return monthlyShortfall;
    }

    /**
     * How much of the current safe-to-spend the scenario consumes, as a percentage.
     * Null when there is no positive safe-to-spend to measure against.
     */
    public BigDecimal getSafeToSpendDropPercent() {
        return safeToSpendDropPercent;
    }

    public String getSummary() {
        return summary;
    }

    public String getSurvivalMessage() {
        return survivalMessage;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public String getDisclaimer() {
        return disclaimer;
    }
}
