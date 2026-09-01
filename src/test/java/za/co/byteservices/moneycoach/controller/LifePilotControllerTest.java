package za.co.byteservices.moneycoach.controller;

import org.junit.jupiter.api.Test;
import za.co.byteservices.moneycoach.dto.BalanceForecastRequest;
import za.co.byteservices.moneycoach.dto.BalanceForecastResponse;
import za.co.byteservices.moneycoach.dto.LifePilotScenarioRequest;
import za.co.byteservices.moneycoach.dto.LifePilotScenarioResponse;
import za.co.byteservices.moneycoach.model.LifePilotScenarioType;
import za.co.byteservices.moneycoach.model.MoneyCoachRiskLevel;
import za.co.byteservices.moneycoach.model.SurvivalStatus;
import za.co.byteservices.moneycoach.service.BalanceForecastService;
import za.co.byteservices.moneycoach.service.LifePilotScenarioService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LifePilotControllerTest {

    private final LifePilotScenarioService scenarioService = mock(LifePilotScenarioService.class);
    private final BalanceForecastService balanceForecastService = mock(BalanceForecastService.class);
    private final LifePilotController controller = new LifePilotController(scenarioService, balanceForecastService);

    @Test
    void simulatesLifePilotScenario() {
        LifePilotScenarioRequest request = new LifePilotScenarioRequest(
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
        );

        LifePilotScenarioResponse expected = new LifePilotScenarioResponse(
                "acc-123",
                LifePilotScenarioType.PRIVATE_SCHOOL,
                "Send child to private school",
                new BigDecimal("8764.11"),
                new BigDecimal("-8435.89"),
                new BigDecimal("-14935.89"),
                new BigDecimal("6500.00"),
                new BigDecimal("15000.00"),
                18,
                "ZAR",
                MoneyCoachRiskLevel.CRITICAL,
                SurvivalStatus.UNAFFORDABLE,
                new BigDecimal("0.00"),
                new BigDecimal("14935.89"),
                null,
                "This life event would reduce your monthly safe-to-spend by ZAR 6500.00.",
                "On these numbers this decision does not fit.",
                List.of("Delay this scenario until your current safe-to-spend is positive."),
                "Educational planning guidance only. This is not financial advice."
        );

        when(scenarioService.simulate(request)).thenReturn(expected);

        LifePilotScenarioResponse response = controller.simulateScenario(request);

        assertThat(response).isSameAs(expected);
    }

    @Test
    void delegatesForecastRequestBody() {
        BalanceForecastRequest request = new BalanceForecastRequest(
                60, new BigDecimal("2000.00"), null, null, true);
        BalanceForecastResponse expected = forecastResponse();

        when(balanceForecastService.forecast("acc-123", request)).thenReturn(expected);

        BalanceForecastResponse response = controller.forecastBalance("acc-123", request);

        assertThat(response).isSameAs(expected);
    }

    @Test
    void acceptsForecastRequestWithoutBody() {
        BalanceForecastResponse expected = forecastResponse();

        when(balanceForecastService.forecast("acc-123", null)).thenReturn(expected);

        BalanceForecastResponse response = controller.forecastBalance("acc-123", (BalanceForecastRequest) null);

        assertThat(response).isSameAs(expected);
    }

    @Test
    void buildsForecastRequestFromQueryParameters() {
        BalanceForecastResponse expected = forecastResponse();
        BalanceForecastRequest translated = new BalanceForecastRequest(
                60, new BigDecimal("2000.00"), new BigDecimal("34000.00"), LocalDate.of(2026, 9, 25), false);

        when(balanceForecastService.forecast("acc-123", translated)).thenReturn(expected);

        BalanceForecastResponse response = controller.forecastBalance(
                "acc-123",
                60,
                new BigDecimal("2000.00"),
                new BigDecimal("34000.00"),
                LocalDate.of(2026, 9, 25),
                false
        );

        assertThat(response).isSameAs(expected);
    }

    private BalanceForecastResponse forecastResponse() {
        return new BalanceForecastResponse(
                "acc-123",
                LocalDate.of(2026, 9, 1),
                60,
                new BigDecimal("20000.00"),
                new BigDecimal("41600.00"),
                new BigDecimal("7600.00"),
                LocalDate.of(2026, 9, 24),
                MoneyCoachRiskLevel.HEALTHY,
                new BigDecimal("34000.00"),
                new BigDecimal("12400.00"),
                new BigDecimal("0.00"),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of("Projections are estimates based on past behaviour, not a guarantee or financial advice."),
                "Balance is projected to stay above 0.00 for the whole period.",
                false
        );
    }
}
