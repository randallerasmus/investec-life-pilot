package za.co.byteservices.moneycoach.service;

import org.springframework.stereotype.Service;
import za.co.byteservices.moneycoach.dto.LifePilotScenarioRequest;
import za.co.byteservices.moneycoach.dto.LifePilotScenarioResponse;
import za.co.byteservices.moneycoach.dto.SafeToSpendResponse;
import za.co.byteservices.moneycoach.model.LifePilotScenarioType;
import za.co.byteservices.moneycoach.model.MoneyCoachRiskLevel;
import za.co.byteservices.moneycoach.model.SurvivalStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

@Service
public class LifePilotScenarioService {

    static final String DISCLAIMER = "Educational planning guidance only. This is not financial advice.";

    private final MoneyCoachService moneyCoachService;

    public LifePilotScenarioService(MoneyCoachService moneyCoachService) {
        this.moneyCoachService = moneyCoachService;
    }

    public LifePilotScenarioResponse simulate(LifePilotScenarioRequest request) {
        SafeToSpendResponse current = moneyCoachService.calculateSafeToSpend(
                request.getAccountId(),
                request.getBondOrRent(),
                request.getSchoolFees(),
                request.getInsurance(),
                request.getGroceries(),
                request.getFuel(),
                request.getSubscriptions(),
                request.getOtherBills(),
                request.getGoalSavingAmount()
        );

        BigDecimal monthlyImpact = money(request.getMonthlyCost());
        BigDecimal onceOffImpact = money(request.getOnceOffCost());
        BigDecimal projectedSafeToSpend = money(current.getSafeToSpend().subtract(monthlyImpact));
        MoneyCoachRiskLevel riskLevel = riskLevel(current.getAvailableBalance(), projectedSafeToSpend);
        SurvivalStatus survivalStatus = SurvivalStatus.from(riskLevel);

        // Exactly one of these carries a figure: either the decision leaves room
        // each month or it opens a gap. Splitting them lets the caller show the
        // one that applies instead of a signed number the reader has to decode.
        BigDecimal monthlyBuffer = money(projectedSafeToSpend.max(BigDecimal.ZERO));
        BigDecimal monthlyShortfall = money(projectedSafeToSpend.negate().max(BigDecimal.ZERO));

        return new LifePilotScenarioResponse(
                request.getAccountId(),
                scenarioType(request.getScenarioType()),
                scenarioName(request),
                money(current.getAvailableBalance()),
                money(current.getSafeToSpend()),
                projectedSafeToSpend,
                monthlyImpact,
                onceOffImpact,
                request.getDurationMonths(),
                current.getCurrency(),
                riskLevel,
                survivalStatus,
                monthlyBuffer,
                monthlyShortfall,
                safeToSpendDropPercent(current.getSafeToSpend(), monthlyImpact),
                summary(current.getCurrency(), monthlyImpact, projectedSafeToSpend),
                survivalMessage(
                        current.getCurrency(),
                        survivalStatus,
                        monthlyBuffer,
                        monthlyShortfall,
                        onceOffImpact,
                        request.getDurationMonths()
                ),
                recommendations(riskLevel),
                DISCLAIMER
        );
    }

    /**
     * Share of the current safe-to-spend that the scenario consumes.
     *
     * <p>Null when the current safe-to-spend is not positive: a percentage of
     * nothing, or of a shortfall, would read as precision the number does not have.
     */
    private BigDecimal safeToSpendDropPercent(BigDecimal currentSafeToSpend, BigDecimal monthlyImpact) {
        BigDecimal base = valueOrZero(currentSafeToSpend);
        if (base.signum() <= 0) {
            return null;
        }
        return monthlyImpact
                .multiply(new BigDecimal("100"))
                .divide(base, 1, RoundingMode.HALF_UP);
    }

    private MoneyCoachRiskLevel riskLevel(BigDecimal availableBalance, BigDecimal projectedSafeToSpend) {
        if (projectedSafeToSpend.compareTo(BigDecimal.ZERO) < 0) {
            return MoneyCoachRiskLevel.CRITICAL;
        }

        BigDecimal tenPercentBuffer = valueOrZero(availableBalance).multiply(new BigDecimal("0.10"));
        if (projectedSafeToSpend.compareTo(BigDecimal.ZERO) == 0
                || projectedSafeToSpend.compareTo(tenPercentBuffer) < 0) {
            return MoneyCoachRiskLevel.TIGHT;
        }

        return MoneyCoachRiskLevel.HEALTHY;
    }

    private String summary(String currency, BigDecimal monthlyImpact, BigDecimal projectedSafeToSpend) {
        return String.format(
                Locale.US,
                "This life event would reduce your monthly safe-to-spend by %s %.2f, leaving a projected safe-to-spend amount of %s %.2f.",
                currency,
                monthlyImpact,
                currency,
                projectedSafeToSpend
        );
    }

    /**
     * States whether the decision survives the monthly position, in the terms
     * someone weighing that decision would use.
     */
    private String survivalMessage(String currency,
                                   SurvivalStatus survivalStatus,
                                   BigDecimal monthlyBuffer,
                                   BigDecimal monthlyShortfall,
                                   BigDecimal onceOffImpact,
                                   Integer durationMonths) {
        String period = durationMonths != null && durationMonths > 0
                ? String.format(Locale.US, " for the next %d %s", durationMonths, durationMonths == 1 ? "month" : "months")
                : "";

        String verdict = switch (survivalStatus) {
            case UNAFFORDABLE -> String.format(
                    Locale.US,
                    "On these numbers this decision does not fit. It would leave you %s %.2f short every month%s.",
                    currency, monthlyShortfall, period);
            case TIGHT -> String.format(
                    Locale.US,
                    "This decision fits, but only just. It would leave %s %.2f of monthly room%s, which an unexpected expense could erase.",
                    currency, monthlyBuffer, period);
            case AFFORDABLE -> String.format(
                    Locale.US,
                    "This decision fits your monthly position. It would leave %s %.2f of room each month%s.",
                    currency, monthlyBuffer, period);
        };

        if (onceOffImpact.signum() > 0) {
            return verdict + String.format(
                    Locale.US,
                    " The once-off cost of %s %.2f still needs to come from savings rather than this monthly figure.",
                    currency, onceOffImpact);
        }

        return verdict;
    }

    private List<String> recommendations(MoneyCoachRiskLevel riskLevel) {
        if (riskLevel == MoneyCoachRiskLevel.CRITICAL) {
            return List.of(
                    "Delay this scenario until your current safe-to-spend is positive.",
                    "Reduce existing monthly commitments before adding this cost.",
                    "Build a separate buffer for the once-off cost before committing."
            );
        }

        if (riskLevel == MoneyCoachRiskLevel.TIGHT) {
            return List.of(
                    "Keep a larger monthly buffer before committing to this scenario.",
                    "Test this payment amount for one month by moving it into savings first.",
                    "Avoid adding other recurring commitments while this scenario is active."
            );
        }

        return List.of(
                "This scenario appears affordable on the supplied monthly numbers, but keep bills and emergency savings protected.",
                "Move the monthly scenario amount into a separate pocket before spending it.",
                "Review the impact again if income, bills, or savings goals change."
        );
    }

    private LifePilotScenarioType scenarioType(LifePilotScenarioType scenarioType) {
        return scenarioType != null ? scenarioType : LifePilotScenarioType.CUSTOM;
    }

    private String scenarioName(LifePilotScenarioRequest request) {
        if (request.getScenarioName() != null && !request.getScenarioName().isBlank()) {
            return request.getScenarioName();
        }
        return scenarioType(request.getScenarioType()).name();
    }

    private BigDecimal money(BigDecimal value) {
        return valueOrZero(value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
