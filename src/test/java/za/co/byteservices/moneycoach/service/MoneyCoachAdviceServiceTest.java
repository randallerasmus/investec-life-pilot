package za.co.byteservices.moneycoach.service;

import org.junit.jupiter.api.Test;
import za.co.byteservices.moneycoach.dto.MoneyCoachAdviceResponse;
import za.co.byteservices.moneycoach.dto.SafeToSpendResponse;
import za.co.byteservices.moneycoach.model.MoneyCoachRiskLevel;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MoneyCoachAdviceServiceTest {

    private final MoneyCoachService moneyCoachService = mock(MoneyCoachService.class);
    private final AiAdviceClient aiAdviceClient = mock(AiAdviceClient.class);
    private final MoneyCoachAdviceService adviceService = new MoneyCoachAdviceService(moneyCoachService, Optional.of(aiAdviceClient));

    @Test
    void returnsCriticalAdviceWhenSafeToSpendIsNegative() {
        whenSafeToSpendIs(new BigDecimal("10000.00"), new BigDecimal("9000.00"), new BigDecimal("2000.00"), new BigDecimal("-1000.00"));
        when(aiAdviceClient.generateAdvice(any())).thenReturn(Optional.empty());

        MoneyCoachAdviceResponse response = adviceService.getAdvice(
                "acc-123",
                new BigDecimal("5000.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("500.00"),
                new BigDecimal("500.00"),
                BigDecimal.ZERO,
                new BigDecimal("2000.00")
        );

        assertThat(response.getRiskLevel()).isEqualTo(MoneyCoachRiskLevel.CRITICAL);
        assertThat(response.getSummary()).contains("short by ZAR 1000.00");
        assertThat(response.getRecommendations()).contains("Reduce or delay non-essential spending until your bills and savings target are covered.");
        assertThat(response.isAiGenerated()).isFalse();
        assertThat(response.getDisclaimer()).isEqualTo("Educational budgeting guidance only. This is not financial advice.");
    }

    @Test
    void returnsTightAdviceWhenSafeToSpendIsLessThanTenPercentOfAvailableBalance() {
        whenSafeToSpendIs(new BigDecimal("10000.00"), new BigDecimal("8700.00"), new BigDecimal("500.00"), new BigDecimal("800.00"));
        when(aiAdviceClient.generateAdvice(any())).thenReturn(Optional.empty());

        MoneyCoachAdviceResponse response = adviceService.getAdvice(
                "acc-123",
                new BigDecimal("5000.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("500.00"),
                new BigDecimal("200.00"),
                BigDecimal.ZERO,
                new BigDecimal("500.00")
        );

        assertThat(response.getRiskLevel()).isEqualTo(MoneyCoachRiskLevel.TIGHT);
        assertThat(response.getSummary()).contains("small buffer of ZAR 800.00");
        assertThat(response.getRecommendations()).contains("Keep discretionary spending low until more income arrives or bills reduce.");
        assertThat(response.isAiGenerated()).isFalse();
    }

    @Test
    void returnsHealthyAdviceWhenSafeToSpendIsAtLeastTenPercentOfAvailableBalance() {
        whenSafeToSpendIs(new BigDecimal("10000.00"), new BigDecimal("6000.00"), new BigDecimal("1000.00"), new BigDecimal("3000.00"));
        when(aiAdviceClient.generateAdvice(any())).thenReturn(Optional.empty());

        MoneyCoachAdviceResponse response = adviceService.getAdvice(
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

        assertThat(response.getRiskLevel()).isEqualTo(MoneyCoachRiskLevel.HEALTHY);
        assertThat(response.getSummary()).contains("ZAR 3000.00 is safe to spend");
        assertThat(response.getRecommendations()).contains("Keep your planned bills and savings protected before increasing flexible spending.");
        assertThat(response.isAiGenerated()).isFalse();
    }

    @Test
    void usesAiSummaryWhenAiClientReturnsAdvice() {
        whenSafeToSpendIs(new BigDecimal("10000.00"), new BigDecimal("6000.00"), new BigDecimal("1000.00"), new BigDecimal("3000.00"));
        when(aiAdviceClient.generateAdvice(any())).thenReturn(Optional.of("AI rewritten coaching summary."));

        MoneyCoachAdviceResponse response = adviceService.getAdvice(
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

        assertThat(response.getSummary()).isEqualTo("AI rewritten coaching summary.");
        assertThat(response.isAiGenerated()).isTrue();
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
