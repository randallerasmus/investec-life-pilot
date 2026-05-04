package za.co.byteservices.moneycoach.controller;

import org.junit.jupiter.api.Test;
import za.co.byteservices.moneycoach.dto.LifePilotScenarioRequest;
import za.co.byteservices.moneycoach.dto.LifePilotScenarioResponse;
import za.co.byteservices.moneycoach.model.LifePilotScenarioType;
import za.co.byteservices.moneycoach.model.MoneyCoachRiskLevel;
import za.co.byteservices.moneycoach.service.LifePilotScenarioService;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LifePilotControllerTest {

    private final LifePilotScenarioService scenarioService = mock(LifePilotScenarioService.class);
    private final LifePilotController controller = new LifePilotController(scenarioService);

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
                "This life event would reduce your monthly safe-to-spend by ZAR 6500.00.",
                List.of("Delay this scenario until your current safe-to-spend is positive."),
                "Educational planning guidance only. This is not financial advice."
        );

        when(scenarioService.simulate(request)).thenReturn(expected);

        LifePilotScenarioResponse response = controller.simulateScenario(request);

        assertThat(response).isSameAs(expected);
    }
}
