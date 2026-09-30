package za.co.byteservices.moneycoach.service;

import org.springframework.stereotype.Service;
import za.co.byteservices.moneycoach.dto.BalanceForecastRequest;
import za.co.byteservices.moneycoach.dto.BalanceForecastResponse;
import za.co.byteservices.moneycoach.dto.ForecastComparison;
import za.co.byteservices.moneycoach.dto.LifePilotScenarioRequest;
import za.co.byteservices.moneycoach.dto.LifePilotScenarioResponse;
import za.co.byteservices.moneycoach.dto.PlannedCashflow;
import za.co.byteservices.moneycoach.model.LifePilotScenarioType;
import za.co.byteservices.moneycoach.model.MoneyCoachRiskLevel;
import za.co.byteservices.moneycoach.model.SurvivalStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Tests a life decision against the account's own forecast.
 *
 * <p>The account is projected twice from one read of its history: as it stands,
 * and with the decision's costs placed on their dates. The verdict is read from
 * the lowest point of the second curve, because that is where a decision fails:
 * not on average across a month, but on the day before payday when the debit
 * orders have gone and the salary has not arrived.
 *
 * <p>The lowest projected balance doubles as safe-to-spend. It is the most that
 * could leave the account today without the balance going below zero at any
 * point in the forecast.
 */
@Service
public class LifePilotScenarioService {

    static final String DISCLAIMER = "Educational planning guidance only. This is not financial advice.";

    private static final int MIN_HORIZON_DAYS = 90;
    private static final int MAX_HORIZON_DAYS = 365;

    /** The forecast shows a month past the decision's end, so the recovery is visible too. */
    private static final int DAYS_AFTER_DURATION = 31;

    private static final BigDecimal TIGHT_SHARE_OF_BALANCE = new BigDecimal("0.10");

    private final BalanceForecastService balanceForecastService;

    public LifePilotScenarioService(BalanceForecastService balanceForecastService) {
        this.balanceForecastService = balanceForecastService;
    }

    public LifePilotScenarioResponse simulate(LifePilotScenarioRequest request) {
        return simulate(request, LocalDate.now());
    }

    public LifePilotScenarioResponse simulate(LifePilotScenarioRequest request, LocalDate asOf) {
        LocalDate today = asOf != null ? asOf : LocalDate.now();
        BigDecimal monthlyImpact = money(request.getMonthlyCost());
        BigDecimal onceOffImpact = money(request.getOnceOffCost());
        LocalDate startDate = request.getStartDate() != null
                ? request.getStartDate()
                : today.plusMonths(1).withDayOfMonth(1);
        int horizonDays = horizonDays(today, startDate, request.getDurationMonths());
        String name = scenarioName(request);

        ForecastComparison comparison = balanceForecastService.compare(
                request.getAccountId(),
                new BalanceForecastRequest(horizonDays, null, null, null, true),
                today,
                plan(name, monthlyImpact, onceOffImpact, startDate, request.getDurationMonths(), horizonDays)
        );

        BalanceForecastResponse baseline = comparison.getBaseline();
        BalanceForecastResponse withScenario = comparison.getWithPlan();
        String currency = comparison.getCurrency();

        BigDecimal currentSafeToSpend = money(baseline.getLowestProjectedBalance());
        BigDecimal projectedSafeToSpend = money(withScenario.getLowestProjectedBalance());
        LocalDate lowestDate = withScenario.getLowestProjectedBalanceDate();
        LocalDate firstShortfallDate = withScenario.getRisks().isEmpty()
                ? null
                : withScenario.getRisks().get(0).getStartDate();
        LocalDate baselineShortfallDate = baseline.getRisks().isEmpty()
                ? null
                : baseline.getRisks().get(0).getStartDate();
        boolean alreadyShort = currentSafeToSpend.signum() < 0;

        MoneyCoachRiskLevel riskLevel = riskLevel(
                projectedSafeToSpend, cushion(monthlyImpact, baseline.getOpeningBalance()));
        SurvivalStatus survivalStatus = SurvivalStatus.from(riskLevel);

        // Exactly one of these carries a figure: either the low point stays above
        // zero or it goes below. Splitting them lets the caller show the one that
        // applies instead of a signed number the reader has to decode.
        BigDecimal buffer = money(projectedSafeToSpend.max(BigDecimal.ZERO));
        BigDecimal shortfall = money(projectedSafeToSpend.negate().max(BigDecimal.ZERO));

        Verdict verdict = new Verdict(
                currency, horizonDays, monthlyImpact, onceOffImpact, startDate,
                currentSafeToSpend, projectedSafeToSpend, lowestDate,
                firstShortfallDate, baselineShortfallDate, alreadyShort, buffer, shortfall);

        return new LifePilotScenarioResponse(
                request.getAccountId(),
                scenarioType(request.getScenarioType()),
                name,
                money(baseline.getOpeningBalance()),
                currentSafeToSpend,
                projectedSafeToSpend,
                monthlyImpact,
                onceOffImpact,
                request.getDurationMonths(),
                currency,
                riskLevel,
                survivalStatus,
                buffer,
                shortfall,
                safeToSpendDropPercent(currentSafeToSpend, projectedSafeToSpend),
                startDate,
                lowestDate,
                firstShortfallDate,
                alreadyShort,
                baseline,
                withScenario,
                summary(verdict),
                survivalMessage(survivalStatus, verdict),
                recommendations(riskLevel, verdict),
                DISCLAIMER
        );
    }

    private List<PlannedCashflow> plan(String name,
                                       BigDecimal monthlyImpact,
                                       BigDecimal onceOffImpact,
                                       LocalDate startDate,
                                       Integer durationMonths,
                                       int horizonDays) {
        List<PlannedCashflow> plan = new ArrayList<>();
        if (onceOffImpact.signum() > 0) {
            plan.add(new PlannedCashflow(name + " (once-off)", onceOffImpact, startDate, 1));
        }
        if (monthlyImpact.signum() > 0) {
            // Without a duration the cost runs for as long as the forecast looks.
            int months = durationMonths != null && durationMonths > 0
                    ? durationMonths
                    : horizonDays / 28 + 1;
            plan.add(new PlannedCashflow(name, monthlyImpact, startDate, months));
        }
        return plan;
    }

    /** Long enough to cover the decision and a month after it, within what the forecast allows. */
    private int horizonDays(LocalDate today, LocalDate startDate, Integer durationMonths) {
        if (durationMonths == null || durationMonths <= 0) {
            return MAX_HORIZON_DAYS;
        }
        LocalDate end = startDate.plusMonths(durationMonths).plusDays(DAYS_AFTER_DURATION);
        long days = ChronoUnit.DAYS.between(today, end);
        return (int) Math.max(MIN_HORIZON_DAYS, Math.min(MAX_HORIZON_DAYS, days));
    }

    /**
     * How low the lowest point can go before a decision counts as tight: one month
     * of the new cost, or a tenth of today's balance for a decision with no
     * monthly cost. Less than that and one bad month undoes it.
     */
    private BigDecimal cushion(BigDecimal monthlyImpact, BigDecimal openingBalance) {
        BigDecimal shareOfBalance = valueOrZero(openingBalance).max(BigDecimal.ZERO).multiply(TIGHT_SHARE_OF_BALANCE);
        return monthlyImpact.max(shareOfBalance);
    }

    private MoneyCoachRiskLevel riskLevel(BigDecimal projectedSafeToSpend, BigDecimal cushion) {
        if (projectedSafeToSpend.signum() < 0) {
            return MoneyCoachRiskLevel.CRITICAL;
        }
        if (projectedSafeToSpend.compareTo(cushion) < 0) {
            return MoneyCoachRiskLevel.TIGHT;
        }
        return MoneyCoachRiskLevel.HEALTHY;
    }

    /**
     * Share of the current safe-to-spend that the decision consumes.
     *
     * <p>Null when the current safe-to-spend is not positive: a percentage of
     * nothing, or of a shortfall, would read as precision the number does not have.
     */
    private BigDecimal safeToSpendDropPercent(BigDecimal currentSafeToSpend, BigDecimal projectedSafeToSpend) {
        if (currentSafeToSpend.signum() <= 0) {
            return null;
        }
        return currentSafeToSpend.subtract(projectedSafeToSpend)
                .multiply(new BigDecimal("100"))
                .divide(currentSafeToSpend, 1, RoundingMode.HALF_UP);
    }

    private String summary(Verdict v) {
        return String.format(Locale.US,
                "Over the next %d days this decision moves the lowest projected balance from %s %.2f to %s %.2f.",
                v.horizonDays, v.currency, v.currentSafeToSpend, v.currency, v.projectedSafeToSpend);
    }

    /**
     * States whether the decision survives the forecast, in the terms someone
     * weighing that decision would use, and names the day it would fail.
     */
    private String survivalMessage(SurvivalStatus status, Verdict v) {
        String message = switch (status) {
            case UNAFFORDABLE -> v.alreadyShort
                    ? String.format(Locale.US,
                    "The account is already projected to go below zero on %s without this decision. "
                            + "Adding it deepens the lowest point from %s %.2f to %s %.2f on %s.",
                    v.baselineShortfallDate, v.currency, v.currentSafeToSpend,
                    v.currency, v.projectedSafeToSpend, v.lowestDate)
                    : String.format(Locale.US,
                    "On this forecast the decision does not fit. The balance would go below zero on %s "
                            + "and reach %s %.2f on %s. Without it, the lowest point is %s %.2f.",
                    v.firstShortfallDate, v.currency, v.projectedSafeToSpend, v.lowestDate,
                    v.currency, v.currentSafeToSpend);
            case TIGHT -> String.format(Locale.US,
                    "This decision fits, but only just. The balance would bottom out at %s %.2f on %s, "
                            + "which one unexpected expense could erase.",
                    v.currency, v.buffer, v.lowestDate);
            case AFFORDABLE -> String.format(Locale.US,
                    "This decision fits the forecast. Even at its lowest point, on %s, the balance stays "
                            + "%s %.2f above zero.",
                    v.lowestDate, v.currency, v.buffer);
        };

        if (v.onceOffImpact.signum() > 0) {
            message += String.format(Locale.US,
                    " This includes the once-off cost of %s %.2f on %s.",
                    v.currency, v.onceOffImpact, v.startDate);
        }
        return message;
    }

    private List<String> recommendations(MoneyCoachRiskLevel riskLevel, Verdict v) {
        if (riskLevel == MoneyCoachRiskLevel.CRITICAL && v.alreadyShort) {
            return List.of(
                    String.format(Locale.US,
                            "Close the existing gap first: the account is projected below zero on %s even without this decision.",
                            v.baselineShortfallDate),
                    String.format(Locale.US,
                            "A buffer of at least %s %.2f, or a debit order moved to after payday, would keep the current forecast above zero.",
                            v.currency, v.currentSafeToSpend.negate()),
                    "Run this scenario again with a later start date once the forecast stays above zero."
            );
        }

        if (riskLevel == MoneyCoachRiskLevel.CRITICAL) {
            List<String> recommendations = new ArrayList<>();
            recommendations.add(String.format(Locale.US,
                    "Delay the start: on this forecast the first shortfall would land on %s.", v.firstShortfallDate));
            recommendations.add(String.format(Locale.US,
                    "Setting aside %s %.2f before starting would keep the lowest point above zero.",
                    v.currency, v.shortfall));
            if (v.onceOffImpact.signum() > 0) {
                recommendations.add("Fund the once-off cost separately, so it does not land on top of the month's debit orders.");
            } else {
                recommendations.add("Look for a lower monthly cost: every rand off it lifts the lowest point by the same amount each month.");
            }
            return List.copyOf(recommendations);
        }

        if (riskLevel == MoneyCoachRiskLevel.TIGHT) {
            return List.of(
                    String.format(Locale.US,
                            "Build a buffer before committing: the lowest point of %s %.2f on %s leaves little room.",
                            v.currency, v.buffer, v.lowestDate),
                    String.format(Locale.US,
                            "Test it first: move %s %.2f into a separate pocket each month and check the account still reaches payday.",
                            v.currency, v.monthlyImpact),
                    "Avoid adding other recurring commitments while this scenario is active."
            );
        }

        return List.of(
                "The forecast absorbs this decision, but keep bills and emergency savings protected.",
                "Move the monthly amount into a separate pocket before spending it.",
                "Run the forecast again if income or debit orders change."
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

    /** The figures every piece of wording draws on, gathered so each sentence reads from one place. */
    private record Verdict(String currency,
                           int horizonDays,
                           BigDecimal monthlyImpact,
                           BigDecimal onceOffImpact,
                           LocalDate startDate,
                           BigDecimal currentSafeToSpend,
                           BigDecimal projectedSafeToSpend,
                           LocalDate lowestDate,
                           LocalDate firstShortfallDate,
                           LocalDate baselineShortfallDate,
                           boolean alreadyShort,
                           BigDecimal buffer,
                           BigDecimal shortfall) {
    }
}
