package za.co.byteservices.moneycoach.service;

import org.junit.jupiter.api.Test;
import za.co.byteservices.moneycoach.dto.LifePilotScenarioRequest;
import za.co.byteservices.moneycoach.dto.LifePilotScenarioResponse;
import za.co.byteservices.moneycoach.dto.SafeToSpendResponse;
import za.co.byteservices.moneycoach.model.LifePilotScenarioType;
import za.co.byteservices.moneycoach.model.MoneyCoachRiskLevel;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LifePilotScenarioServiceTest {

    private final MoneyCoachService moneyCoachService = mock(MoneyCoachService.class);
    private final LifePilotScenarioService scenarioService = new LifePilotScenarioService(moneyCoachService);

    @Test
    void returnsCriticalScenarioWhenProjectedSafeToSpendIsNegative() {
        whenSafeToSpendIs(new BigDecimal("8764.11"), new BigDecimal("16700.00"), new BigDecimal("500.00"), new BigDecimal("-8435.89"));

        LifePilotScenarioResponse response = scenarioService.simulate(new LifePilotScenarioRequest(
                "acc-123",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("500.00"),
                LifePilotScenarioType.PRIVATE_SCHOOL,
                "Send child to private school",
                new BigDecimal("6500.00"),
                new BigDecimal("15000.00"),
                18
        ));

        assertThat(response.getScenarioName()).isEqualTo("Send child to private school");
        assertThat(response.getCurrentSafeToSpend()).isEqualByComparingTo("-8435.89");
        assertThat(response.getProjectedSafeToSpend()).isEqualByComparingTo("-14935.89");
        assertThat(response.getMonthlyImpact()).isEqualByComparingTo("6500.00");
        assertThat(response.getOnceOffImpact()).isEqualByComparingTo("15000.00");
        assertThat(response.getDurationMonths()).isEqualTo(18);
        assertThat(response.getRiskLevel()).isEqualTo(MoneyCoachRiskLevel.CRITICAL);
        assertThat(response.getSummary()).contains("reduce your monthly safe-to-spend by ZAR 6500.00");
        assertThat(response.getRecommendations()).contains("Delay this scenario until your current safe-to-spend is positive.");
        assertThat(response.getDisclaimer()).isEqualTo("Educational planning guidance only. This is not financial advice.");
    }

    @Test
    void returnsTightScenarioWhenProjectedSafeToSpendIsBelowTenPercentOfAvailableBalance() {
        whenSafeToSpendIs(new BigDecimal("10000.00"), new BigDecimal("6000.00"), new BigDecimal("500.00"), new BigDecimal("3500.00"));

        LifePilotScenarioResponse response = scenarioService.simulate(new LifePilotScenarioRequest(
                "acc-123",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("500.00"),
                LifePilotScenarioType.SECOND_CAR,
                "Buy a second car",
                new BigDecimal("2800.00"),
                new BigDecimal("0.00"),
                60
        ));

        assertThat(response.getProjectedSafeToSpend()).isEqualByComparingTo("700.00");
        assertThat(response.getRiskLevel()).isEqualTo(MoneyCoachRiskLevel.TIGHT);
        assertThat(response.getRecommendations()).contains("Keep a larger monthly buffer before committing to this scenario.");
    }

    @Test
    void returnsHealthyScenarioWhenProjectedSafeToSpendHasEnoughBuffer() {
        whenSafeToSpendIs(new BigDecimal("30000.00"), new BigDecimal("12000.00"), new BigDecimal("3000.00"), new BigDecimal("15000.00"));

        LifePilotScenarioResponse response = scenarioService.simulate(new LifePilotScenarioRequest(
                "acc-123",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("3000.00"),
                LifePilotScenarioType.OVERSEAS_HOLIDAY,
                "Family overseas holiday",
                new BigDecimal("5000.00"),
                new BigDecimal("10000.00"),
                12
        ));

        assertThat(response.getProjectedSafeToSpend()).isEqualByComparingTo("10000.00");
        assertThat(response.getRiskLevel()).isEqualTo(MoneyCoachRiskLevel.HEALTHY);
        assertThat(response.getRecommendations()).contains("This scenario appears affordable on the supplied monthly numbers, but keep bills and emergency savings protected.");
    }

    private void whenSafeToSpendIs(BigDecimal availableBalance,
                                   BigDecimal estimatedBills,
                                   BigDecimal goalSavingAmount,
                                   BigDecimal safeToSpend) {
        when(moneyCoachService.calculateSafeToSpend(
                eq("acc-123"),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        )).thenReturn(new SafeToSpendResponse(
                "acc-123",
                availableBalance,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                estimatedBills,
                goalSavingAmount,
                safeToSpend,
                "ZAR",
                "safe-to-spend message"
        ));
    }
}
