package za.co.byteservices.moneycoach.controller;

import org.junit.jupiter.api.Test;
import za.co.byteservices.moneycoach.dto.MoneyCoachAdviceResponse;
import za.co.byteservices.moneycoach.model.MoneyCoachRiskLevel;
import za.co.byteservices.moneycoach.service.MoneyCoachAdviceService;
import za.co.byteservices.moneycoach.service.MoneyCoachService;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MoneyCoachControllerTest {

    private final MoneyCoachService moneyCoachService = mock(MoneyCoachService.class);
    private final MoneyCoachAdviceService adviceService = mock(MoneyCoachAdviceService.class);
    private final MoneyCoachController controller = new MoneyCoachController(moneyCoachService, adviceService);

    @Test
    void returnsMoneyCoachAdviceForAccount() {
        MoneyCoachAdviceResponse expected = new MoneyCoachAdviceResponse(
                "acc-123",
                new BigDecimal("10000.00"),
                new BigDecimal("6000.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("3000.00"),
                "ZAR",
                MoneyCoachRiskLevel.HEALTHY,
                "ZAR 3000.00 is safe to spend.",
                List.of("Keep your bills protected."),
                false,
                "Educational budgeting guidance only. This is not financial advice."
        );

        when(adviceService.getAdvice(
                "acc-123",
                new BigDecimal("3000.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("500.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("300.00"),
                new BigDecimal("200.00"),
                BigDecimal.ZERO,
                new BigDecimal("1000.00")
        )).thenReturn(expected);

        MoneyCoachAdviceResponse response = controller.getAdvice(
                "acc-123",
                new BigDecimal("3000.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("500.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("300.00"),
                new BigDecimal("200.00"),
                BigDecimal.ZERO,
                new BigDecimal("1000.00")
        );

        assertThat(response).isSameAs(expected);
    }
}
