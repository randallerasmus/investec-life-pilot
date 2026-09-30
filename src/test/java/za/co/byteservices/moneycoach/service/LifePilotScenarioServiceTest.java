package za.co.byteservices.moneycoach.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import za.co.byteservices.moneycoach.dto.BalanceForecastRequest;
import za.co.byteservices.moneycoach.dto.BalanceForecastResponse;
import za.co.byteservices.moneycoach.dto.CashflowRisk;
import za.co.byteservices.moneycoach.dto.ForecastComparison;
import za.co.byteservices.moneycoach.dto.LifePilotScenarioRequest;
import za.co.byteservices.moneycoach.dto.LifePilotScenarioResponse;
import za.co.byteservices.moneycoach.dto.PlannedCashflow;
import za.co.byteservices.moneycoach.model.LifePilotScenarioType;
import za.co.byteservices.moneycoach.model.MoneyCoachRiskLevel;
import za.co.byteservices.moneycoach.model.SurvivalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LifePilotScenarioServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 30);
    private static final LocalDate LOW_DAY = LocalDate.of(2026, 10, 24);

    private final BalanceForecastService forecastService = mock(BalanceForecastService.class);
    private final LifePilotScenarioService scenarioService = new LifePilotScenarioService(forecastService);

    @Test
    void isUnaffordableWhenTheDecisionTakesTheBalanceBelowZero() {
        whenForecastsAre(
                forecast("35196.00", "4000.00", List.of()),
                forecast("35196.00", "-2500.00", List.of(overdraft(LocalDate.of(2026, 10, 21), "-2500.00")))
        );

        LifePilotScenarioResponse response = scenarioService.simulate(
                scenario("6500.00", "15000.00", 18), TODAY);

        assertThat(response.getScenarioName()).isEqualTo("Send child to private school");
        assertThat(response.getAvailableBalance()).isEqualByComparingTo("35196.00");
        assertThat(response.getCurrentSafeToSpend()).isEqualByComparingTo("4000.00");
        assertThat(response.getProjectedSafeToSpend()).isEqualByComparingTo("-2500.00");
        assertThat(response.getRiskLevel()).isEqualTo(MoneyCoachRiskLevel.CRITICAL);
        assertThat(response.getSurvivalStatus()).isEqualTo(SurvivalStatus.UNAFFORDABLE);
        assertThat(response.getBufferAtLowestPoint()).isEqualByComparingTo("0.00");
        assertThat(response.getShortfallAtLowestPoint()).isEqualByComparingTo("2500.00");
        assertThat(response.getFirstShortfallDate()).isEqualTo(LocalDate.of(2026, 10, 21));
        assertThat(response.getLowestBalanceDate()).isEqualTo(LOW_DAY);
        assertThat(response.isAlreadyShortWithoutScenario()).isFalse();
        // (4000 - -2500) / 4000
        assertThat(response.getSafeToSpendDropPercent()).isEqualByComparingTo("162.5");
        assertThat(response.getSurvivalMessage())
                .contains("does not fit")
                .contains("go below zero on 2026-10-21")
                .contains("Without it, the lowest point is ZAR 4000.00")
                .contains("once-off cost of ZAR 15000.00 on 2026-10-01");
        assertThat(response.getRecommendations())
                .contains("Delay the start: on this forecast the first shortfall would land on 2026-10-21.")
                .contains("Setting aside ZAR 2500.00 before starting would keep the lowest point above zero.");
        assertThat(response.getBaselineForecast()).isNotNull();
        assertThat(response.getScenarioForecast()).isNotNull();
        assertThat(response.getDisclaimer()).isEqualTo("Educational planning guidance only. This is not financial advice.");
    }

    @Test
    void saysSoWhenTheAccountIsAlreadyShortWithoutTheDecision() {
        whenForecastsAre(
                forecast("35196.00", "-480.00", List.of(overdraft(LocalDate.of(2026, 10, 21), "-480.00"))),
                forecast("35196.00", "-6980.00", List.of(overdraft(LocalDate.of(2026, 10, 3), "-6980.00")))
        );

        LifePilotScenarioResponse response = scenarioService.simulate(
                scenario("6500.00", "0.00", 18), TODAY);

        assertThat(response.getSurvivalStatus()).isEqualTo(SurvivalStatus.UNAFFORDABLE);
        assertThat(response.isAlreadyShortWithoutScenario()).isTrue();
        // A percentage of a shortfall would read as precision the number does not have.
        assertThat(response.getSafeToSpendDropPercent()).isNull();
        assertThat(response.getSurvivalMessage())
                .contains("already projected to go below zero on 2026-10-21 without this decision")
                .contains("from ZAR -480.00 to ZAR -6980.00")
                .doesNotContain("once-off");
        assertThat(response.getRecommendations().get(0)).startsWith("Close the existing gap first");
        assertThat(response.getRecommendations().get(1)).contains("ZAR 480.00");
    }

    @Test
    void isTightWhenTheLowestPointLeavesLessThanAMonthOfTheNewCost() {
        whenForecastsAre(
                forecast("20000.00", "9000.00", List.of()),
                forecast("20000.00", "2500.00", List.of())
        );

        LifePilotScenarioResponse response = scenarioService.simulate(
                scenario("6500.00", "0.00", 12), TODAY);

        assertThat(response.getRiskLevel()).isEqualTo(MoneyCoachRiskLevel.TIGHT);
        assertThat(response.getSurvivalStatus()).isEqualTo(SurvivalStatus.TIGHT);
        assertThat(response.getBufferAtLowestPoint()).isEqualByComparingTo("2500.00");
        assertThat(response.getShortfallAtLowestPoint()).isEqualByComparingTo("0.00");
        assertThat(response.getFirstShortfallDate()).isNull();
        assertThat(response.getSurvivalMessage())
                .contains("fits, but only just")
                .contains("bottom out at ZAR 2500.00 on 2026-10-24");
    }

    @Test
    void isAffordableWhenTheForecastAbsorbsTheDecision() {
        whenForecastsAre(
                forecast("50000.00", "40000.00", List.of()),
                forecast("50000.00", "30000.00", List.of())
        );

        LifePilotScenarioResponse response = scenarioService.simulate(
                scenario("6500.00", "0.00", 12), TODAY);

        assertThat(response.getRiskLevel()).isEqualTo(MoneyCoachRiskLevel.HEALTHY);
        assertThat(response.getSurvivalStatus()).isEqualTo(SurvivalStatus.AFFORDABLE);
        assertThat(response.getBufferAtLowestPoint()).isEqualByComparingTo("30000.00");
        assertThat(response.getSafeToSpendDropPercent()).isEqualByComparingTo("25.0");
        assertThat(response.getSummary())
                .isEqualTo("Over the next 365 days this decision moves the lowest projected balance from ZAR 40000.00 to ZAR 30000.00.");
        assertThat(response.getSurvivalMessage()).contains("stays ZAR 30000.00 above zero");
    }

    @Test
    void placesTheDecisionOnTheForecastFromTheFirstOfNextMonth() {
        whenForecastsAre(forecast("20000.00", "9000.00", List.of()), forecast("20000.00", "2500.00", List.of()));

        scenarioService.simulate(scenario("6500.00", "15000.00", 2), TODAY);

        Captured captured = captured();
        // 1 Oct + 2 months + 31 days = 1 Jan 2027, 93 days after 30 Sep.
        assertThat(captured.request.getHorizonDays()).isEqualTo(93);
        assertThat(captured.plan).hasSize(2);
        assertThat(captured.plan).allMatch(p -> p.getFirstDate().equals(LocalDate.of(2026, 10, 1)));
        assertThat(captured.plan).anyMatch(p -> p.getOccurrences() == 1
                && p.getAmount().compareTo(new BigDecimal("15000.00")) == 0);
        assertThat(captured.plan).anyMatch(p -> p.getOccurrences() == 2
                && p.getAmount().compareTo(new BigDecimal("6500.00")) == 0);
    }

    @Test
    void runsAnOpenEndedDecisionForTheWholeYearAndHonoursAGivenStartDate() {
        whenForecastsAre(forecast("20000.00", "9000.00", List.of()), forecast("20000.00", "2500.00", List.of()));
        LocalDate start = LocalDate.of(2026, 11, 15);

        scenarioService.simulate(new LifePilotScenarioRequest(
                "acc-123", LifePilotScenarioType.UNPAID_LEAVE, "Take unpaid leave",
                new BigDecimal("39000.00"), null, null, start), TODAY);

        Captured captured = captured();
        assertThat(captured.request.getHorizonDays()).isEqualTo(365);
        assertThat(captured.plan).singleElement().satisfies(p -> {
            assertThat(p.getFirstDate()).isEqualTo(start);
            assertThat(p.getOccurrences()).isGreaterThanOrEqualTo(12);
        });
    }

    private LifePilotScenarioRequest scenario(String monthlyCost, String onceOffCost, Integer durationMonths) {
        return new LifePilotScenarioRequest(
                "acc-123",
                LifePilotScenarioType.PRIVATE_SCHOOL,
                "Send child to private school",
                new BigDecimal(monthlyCost),
                new BigDecimal(onceOffCost),
                durationMonths,
                null
        );
    }

    private void whenForecastsAre(BalanceForecastResponse baseline, BalanceForecastResponse withScenario) {
        when(forecastService.compare(eq("acc-123"), any(BalanceForecastRequest.class), eq(TODAY), any()))
                .thenReturn(new ForecastComparison(baseline, withScenario, "ZAR"));
    }

    @SuppressWarnings("unchecked")
    private Captured captured() {
        ArgumentCaptor<BalanceForecastRequest> request = ArgumentCaptor.forClass(BalanceForecastRequest.class);
        ArgumentCaptor<List<PlannedCashflow>> plan = ArgumentCaptor.forClass(List.class);
        verify(forecastService).compare(eq("acc-123"), request.capture(), eq(TODAY), plan.capture());
        return new Captured(request.getValue(), plan.getValue());
    }

    private record Captured(BalanceForecastRequest request, List<PlannedCashflow> plan) {
    }

    private CashflowRisk overdraft(LocalDate start, String lowest) {
        return new CashflowRisk(
                start,
                LOW_DAY,
                (int) (LOW_DAY.toEpochDay() - start.toEpochDay() + 1),
                new BigDecimal(lowest),
                LOW_DAY,
                MoneyCoachRiskLevel.CRITICAL,
                "message"
        );
    }

    /** Only the fields the simulator reads carry meaning; the rest are placeholders. */
    private BalanceForecastResponse forecast(String openingBalance, String lowestBalance, List<CashflowRisk> risks) {
        return new BalanceForecastResponse(
                "acc-123",
                TODAY,
                120,
                new BigDecimal(openingBalance),
                new BigDecimal("20000.00"),
                new BigDecimal(lowestBalance),
                LOW_DAY,
                risks.isEmpty() ? MoneyCoachRiskLevel.HEALTHY : MoneyCoachRiskLevel.CRITICAL,
                new BigDecimal("39000.00"),
                new BigDecimal("23976.00"),
                new BigDecimal("300.00"),
                List.of(),
                List.of(),
                risks,
                List.of(),
                List.of(),
                "summary",
                false
        );
    }
}
