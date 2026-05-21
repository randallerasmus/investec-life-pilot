package za.co.byteservices.moneycoach.service;

import org.junit.jupiter.api.Test;
import za.co.byteservices.moneycoach.dto.AdvancedSafeToSpendRequest;
import za.co.byteservices.moneycoach.dto.AdvancedSafeToSpendResponse;
import za.co.byteservices.moneycoach.dto.AiCoachQuestionRequest;
import za.co.byteservices.moneycoach.dto.AiCoachResponse;
import za.co.byteservices.moneycoach.dto.KnowledgeSearchResult;
import za.co.byteservices.moneycoach.dto.MoneyCoachAdviceResponse;
import za.co.byteservices.moneycoach.model.MoneyCoachRiskLevel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiCoachServiceTest {

    private final SafeToSpendEngineService safeToSpendEngineService = mock(SafeToSpendEngineService.class);
    private final KnowledgeRagService knowledgeRagService = mock(KnowledgeRagService.class);
    private final ResponsibleAiGuardrailsService guardrailsService = new ResponsibleAiGuardrailsService();
    private final AiAdviceClient aiAdviceClient = mock(AiAdviceClient.class);
    private final AiCoachService service = new AiCoachService(
            safeToSpendEngineService,
            knowledgeRagService,
            guardrailsService,
            Optional.of(aiAdviceClient)
    );

    @Test
    void buildsGroundedCoachingAnswerWithGuardrails() {
        when(safeToSpendEngineService.calculateAdvancedSafeToSpend(any(), any())).thenReturn(new AdvancedSafeToSpendResponse(
                "acc-123",
                new BigDecimal("12500.00"),
                new BigDecimal("7200.00"),
                new BigDecimal("1000.00"),
                10,
                new BigDecimal("2500.00"),
                new BigDecimal("3800.00"),
                "AFFORDABLE",
                MoneyCoachRiskLevel.TIGHT,
                "You can afford the purchase, but the buffer is tighter before payday."
        ));
        when(knowledgeRagService.search("Can I afford golf clubs?")).thenReturn(List.of(
                new KnowledgeSearchResult(
                        "Emergency Fund Basics",
                        "internal-financial-education",
                        "An emergency fund helps cover unexpected expenses without using debt.",
                        5
                )
        ));
        when(aiAdviceClient.generateAdvice(any(MoneyCoachAdviceResponse.class)))
                .thenReturn(Optional.of("Based on your current buffer, the purchase looks manageable but leaves less room for surprises."));

        AiCoachResponse response = service.askQuestion("acc-123", new AiCoachQuestionRequest(
                "Can I afford golf clubs?",
                LocalDate.now().plusDays(10),
                new BigDecimal("1000.00")
        ));

        assertThat(response.getAccountId()).isEqualTo("acc-123");
        assertThat(response.getRiskLevel()).isEqualTo(MoneyCoachRiskLevel.TIGHT);
        assertThat(response.getConfidence()).isGreaterThan(0);
        assertThat(response.getBasedOn()).isNotEmpty();
        assertThat(response.getRetrievedKnowledge()).hasSize(1);
        assertThat(response.getAnswer()).contains("not regulated financial advice");
        assertThat(response.getGuardrailWarnings()).isEmpty();
    }

    @Test
    void addsGuardrailWarningsForOverconfidentAdvice() {
        when(safeToSpendEngineService.calculateAdvancedSafeToSpend(any(), any())).thenReturn(new AdvancedSafeToSpendResponse(
                "acc-123",
                new BigDecimal("5000.00"),
                new BigDecimal("3000.00"),
                new BigDecimal("500.00"),
                5,
                new BigDecimal("4500.00"),
                new BigDecimal("1000.00"),
                "NOT_AFFORDABLE",
                MoneyCoachRiskLevel.CRITICAL,
                "The planned purchase is too large for the remaining buffer."
        ));
        when(knowledgeRagService.search("Should I buy this now?")).thenReturn(List.of());
        when(aiAdviceClient.generateAdvice(any(MoneyCoachAdviceResponse.class)))
                .thenReturn(Optional.of("You should definitely buy this now because you will be fine."));

        AiCoachResponse response = service.askQuestion("acc-123", new AiCoachQuestionRequest(
                "Should I buy this now?",
                LocalDate.now().plusDays(5),
                new BigDecimal("500.00")
        ));

        assertThat(response.getRiskLevel()).isEqualTo(MoneyCoachRiskLevel.CRITICAL);
        assertThat(response.getGuardrailWarnings()).isNotEmpty();
        assertThat(response.getAnswer()).contains("not regulated financial advice");
    }
}
