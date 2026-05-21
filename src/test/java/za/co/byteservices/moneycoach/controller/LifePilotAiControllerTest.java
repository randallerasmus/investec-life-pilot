package za.co.byteservices.moneycoach.controller;

import org.junit.jupiter.api.Test;
import za.co.byteservices.moneycoach.dto.AdvancedSafeToSpendRequest;
import za.co.byteservices.moneycoach.dto.AdvancedSafeToSpendResponse;
import za.co.byteservices.moneycoach.dto.AiCoachQuestionRequest;
import za.co.byteservices.moneycoach.dto.AiCoachResponse;
import za.co.byteservices.moneycoach.dto.EvaluationScenarioResponse;
import za.co.byteservices.moneycoach.dto.GuardrailCheckRequest;
import za.co.byteservices.moneycoach.dto.GuardrailCheckResponse;
import za.co.byteservices.moneycoach.dto.KnowledgeDocumentRequest;
import za.co.byteservices.moneycoach.dto.KnowledgeDocumentResponse;
import za.co.byteservices.moneycoach.dto.KnowledgeSearchRequest;
import za.co.byteservices.moneycoach.dto.KnowledgeSearchResult;
import za.co.byteservices.moneycoach.dto.EvaluationCriteriaResponse;
import za.co.byteservices.moneycoach.model.MoneyCoachRiskLevel;
import za.co.byteservices.moneycoach.service.AiCoachService;
import za.co.byteservices.moneycoach.service.EvaluationService;
import za.co.byteservices.moneycoach.service.KnowledgeRagService;
import za.co.byteservices.moneycoach.service.ResponsibleAiGuardrailsService;
import za.co.byteservices.moneycoach.service.SafeToSpendEngineService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LifePilotAiControllerTest {

    private final KnowledgeRagService knowledgeRagService = mock(KnowledgeRagService.class);
    private final SafeToSpendEngineService safeToSpendEngineService = mock(SafeToSpendEngineService.class);
    private final AiCoachService aiCoachService = mock(AiCoachService.class);
    private final ResponsibleAiGuardrailsService guardrailsService = mock(ResponsibleAiGuardrailsService.class);
    private final EvaluationService evaluationService = mock(EvaluationService.class);
    private final LifePilotAiController controller = new LifePilotAiController(
            knowledgeRagService,
            safeToSpendEngineService,
            aiCoachService,
            guardrailsService,
            evaluationService
    );

    @Test
    void delegatesKnowledgeDocumentCreation() {
        KnowledgeDocumentRequest request = new KnowledgeDocumentRequest(
                "Emergency Fund Basics",
                "An emergency fund helps cover unexpected expenses...",
                "internal-financial-education"
        );
        KnowledgeDocumentResponse expected = new KnowledgeDocumentResponse(
                "doc-1",
                "Emergency Fund Basics",
                "An emergency fund helps cover unexpected expenses...",
                "internal-financial-education"
        );

        when(knowledgeRagService.addDocument(request)).thenReturn(expected);

        KnowledgeDocumentResponse response = controller.addKnowledgeDocument(request);

        assertThat(response).isSameAs(expected);
    }

    @Test
    void delegatesKnowledgeSearch() {
        KnowledgeSearchRequest request = new KnowledgeSearchRequest("Why do I need an emergency fund?");
        List<KnowledgeSearchResult> expected = List.of(
                new KnowledgeSearchResult("Emergency Fund Basics", "internal-financial-education", "Emergency funds reduce debt risk.", 4)
        );

        when(knowledgeRagService.search(request.getQuestion())).thenReturn(expected);

        List<KnowledgeSearchResult> response = controller.searchKnowledge(request);

        assertThat(response).isSameAs(expected);
    }

    @Test
    void delegatesAdvancedSafeToSpendCalculation() {
        AdvancedSafeToSpendRequest request = new AdvancedSafeToSpendRequest(
                LocalDate.of(2026, 5, 31),
                new BigDecimal("1000.00"),
                new BigDecimal("2500.00"),
                "Golf clubs"
        );
        AdvancedSafeToSpendResponse expected = new AdvancedSafeToSpendResponse(
                "acc-123",
                new BigDecimal("12500.00"),
                new BigDecimal("7200.00"),
                new BigDecimal("1000.00"),
                10,
                new BigDecimal("2500.00"),
                new BigDecimal("3800.00"),
                "AFFORDABLE",
                MoneyCoachRiskLevel.TIGHT,
                "Tight but manageable."
        );

        when(safeToSpendEngineService.calculateAdvancedSafeToSpend("acc-123", request)).thenReturn(expected);

        AdvancedSafeToSpendResponse response = controller.calculateAdvancedSafeToSpend("acc-123", request);

        assertThat(response).isSameAs(expected);
    }

    @Test
    void delegatesAiCoachQuestion() {
        AiCoachQuestionRequest request = new AiCoachQuestionRequest(
                "Can I afford to spend R2500 on golf clubs this month?",
                LocalDate.of(2026, 5, 31),
                new BigDecimal("1000.00")
        );
        AiCoachResponse expected = new AiCoachResponse(
                "This is educational guidance based on available transaction data, not regulated financial advice.",
                "acc-123",
                MoneyCoachRiskLevel.TIGHT,
                82,
                List.of("Available balance", "Recent outgoing transactions"),
                List.of(),
                List.of()
        );

        when(aiCoachService.askQuestion("acc-123", request)).thenReturn(expected);

        AiCoachResponse response = controller.askAiCoach("acc-123", request);

        assertThat(response).isSameAs(expected);
    }

    @Test
    void delegatesGuardrailCheck() {
        GuardrailCheckRequest request = new GuardrailCheckRequest(
                "You should invest all your money in one stock because it will definitely go up."
        );
        GuardrailCheckResponse expected = new GuardrailCheckResponse(
                List.of("Detected guaranteed-return language."),
                "This is educational guidance based on available transaction data, not regulated financial advice.",
                true
        );

        when(guardrailsService.checkAnswer(request.getAnswer())).thenReturn(expected);

        GuardrailCheckResponse response = controller.checkGuardrails(request);

        assertThat(response).isSameAs(expected);
    }

    @Test
    void returnsDefaultEvaluations() {
        List<EvaluationScenarioResponse> expected = List.of(
                new EvaluationScenarioResponse(
                        "Can I afford this purchase?",
                        new EvaluationCriteriaResponse(
                                "Uses relevant knowledge snippets.",
                                "Safe-to-spend math is correct.",
                                "Answer stays tied to available data.",
                                "Avoids unsupported claims.",
                                "Includes disclaimer when needed."
                        )
                )
        );

        when(evaluationService.getDefaultEvaluations()).thenReturn(expected);

        List<EvaluationScenarioResponse> response = controller.getDefaultEvaluations();

        assertThat(response).isSameAs(expected);
    }
}
