package za.co.byteservices.moneycoach.dto;

import za.co.byteservices.moneycoach.model.LifePilotScenarioType;
import za.co.byteservices.moneycoach.model.MoneyCoachRiskLevel;
import za.co.byteservices.moneycoach.model.SurvivalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * A life decision measured against the account's forecast.
 *
 * <p>Carries both projections, without and with the decision, so a client can
 * draw the two curves the verdict was read from.
 */
public class LifePilotScenarioResponse {
    private final String accountId;
    private final LifePilotScenarioType scenarioType;
    private final String scenarioName;
    private final BigDecimal availableBalance;

    /**
     * Lowest projected balance without the decision: what could be spent today
     * without the account ever going below zero over the forecast.
     */
    private final BigDecimal currentSafeToSpend;

    /** The same figure with the decision in place. */
    private final BigDecimal projectedSafeToSpend;
    private final BigDecimal monthlyImpact;
    private final BigDecimal onceOffImpact;
    private final Integer durationMonths;
    private final String currency;
    private final MoneyCoachRiskLevel riskLevel;
    private final SurvivalStatus survivalStatus;

    /**
     * How far above zero the balance stays at its lowest point with the decision.
     * Exactly one of this and {@link #shortfallAtLowestPoint} carries a figure.
     */
    private final BigDecimal bufferAtLowestPoint;

    /** How far below zero the balance goes at its lowest point with the decision. */
    private final BigDecimal shortfallAtLowestPoint;

    /** Null when there is no positive safe-to-spend to measure the drop against. */
    private final BigDecimal safeToSpendDropPercent;
    private final LocalDate startDate;
    private final LocalDate lowestBalanceDate;

    /** First day the balance goes below zero with the decision, or null if it never does. */
    private final LocalDate firstShortfallDate;

    /**
     * True when the account goes below zero even without the decision, so the
     * decision deepens a shortfall rather than causing one.
     */
    private final boolean alreadyShortWithoutScenario;
    private final BalanceForecastResponse baselineForecast;
    private final BalanceForecastResponse scenarioForecast;
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
                                     BigDecimal bufferAtLowestPoint,
                                     BigDecimal shortfallAtLowestPoint,
                                     BigDecimal safeToSpendDropPercent,
                                     LocalDate startDate,
                                     LocalDate lowestBalanceDate,
                                     LocalDate firstShortfallDate,
                                     boolean alreadyShortWithoutScenario,
                                     BalanceForecastResponse baselineForecast,
                                     BalanceForecastResponse scenarioForecast,
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
        this.bufferAtLowestPoint = bufferAtLowestPoint;
        this.shortfallAtLowestPoint = shortfallAtLowestPoint;
        this.safeToSpendDropPercent = safeToSpendDropPercent;
        this.startDate = startDate;
        this.lowestBalanceDate = lowestBalanceDate;
        this.firstShortfallDate = firstShortfallDate;
        this.alreadyShortWithoutScenario = alreadyShortWithoutScenario;
        this.baselineForecast = baselineForecast;
        this.scenarioForecast = scenarioForecast;
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

    public BigDecimal getBufferAtLowestPoint() {
        return bufferAtLowestPoint;
    }

    public BigDecimal getShortfallAtLowestPoint() {
        return shortfallAtLowestPoint;
    }

    public BigDecimal getSafeToSpendDropPercent() {
        return safeToSpendDropPercent;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getLowestBalanceDate() {
        return lowestBalanceDate;
    }

    public LocalDate getFirstShortfallDate() {
        return firstShortfallDate;
    }

    public boolean isAlreadyShortWithoutScenario() {
        return alreadyShortWithoutScenario;
    }

    public BalanceForecastResponse getBaselineForecast() {
        return baselineForecast;
    }

    public BalanceForecastResponse getScenarioForecast() {
        return scenarioForecast;
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
