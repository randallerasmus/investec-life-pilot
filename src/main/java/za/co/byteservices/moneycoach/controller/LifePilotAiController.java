package za.co.byteservices.moneycoach.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
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
import za.co.byteservices.moneycoach.service.AiCoachService;
import za.co.byteservices.moneycoach.service.EvaluationService;
import za.co.byteservices.moneycoach.service.KnowledgeRagService;
import za.co.byteservices.moneycoach.service.ResponsibleAiGuardrailsService;
import za.co.byteservices.moneycoach.service.SafeToSpendEngineService;

import java.util.List;

@RestController
public class LifePilotAiController {

    private final KnowledgeRagService knowledgeRagService;
    private final SafeToSpendEngineService safeToSpendEngineService;
    private final AiCoachService aiCoachService;
    private final ResponsibleAiGuardrailsService responsibleAiGuardrailsService;
    private final EvaluationService evaluationService;

    public LifePilotAiController(KnowledgeRagService knowledgeRagService,
                                 SafeToSpendEngineService safeToSpendEngineService,
                                 AiCoachService aiCoachService,
                                 ResponsibleAiGuardrailsService responsibleAiGuardrailsService,
                                 EvaluationService evaluationService) {
        this.knowledgeRagService = knowledgeRagService;
        this.safeToSpendEngineService = safeToSpendEngineService;
        this.aiCoachService = aiCoachService;
        this.responsibleAiGuardrailsService = responsibleAiGuardrailsService;
        this.evaluationService = evaluationService;
    }

    @GetMapping("/api/lifepilot/knowledge/documents")
    public List<KnowledgeDocumentResponse> getKnowledgeDocuments() {
        return knowledgeRagService.getDocuments();
    }

    @PostMapping("/api/lifepilot/knowledge/documents")
    public KnowledgeDocumentResponse addKnowledgeDocument(@Valid @RequestBody KnowledgeDocumentRequest request) {
        return knowledgeRagService.addDocument(request);
    }

    @PostMapping("/api/lifepilot/knowledge/search")
    public List<KnowledgeSearchResult> searchKnowledge(@Valid @RequestBody KnowledgeSearchRequest request) {
        return knowledgeRagService.search(request.getQuestion());
    }

    @PostMapping("/api/lifepilot/safe-to-spend/accounts/{accountId}/advanced")
    public AdvancedSafeToSpendResponse calculateAdvancedSafeToSpend(@PathVariable String accountId,
                                                                    @Valid @RequestBody AdvancedSafeToSpendRequest request) {
        return safeToSpendEngineService.calculateAdvancedSafeToSpend(accountId, request);
    }

    @PostMapping("/api/lifepilot/ai-coach/accounts/{accountId}/ask")
    public AiCoachResponse askAiCoach(@PathVariable String accountId,
                                      @Valid @RequestBody AiCoachQuestionRequest request ) {
        return aiCoachService.askQuestion(accountId, request);
    }

    @PostMapping("/api/lifepilot/guardrails/check")
    public GuardrailCheckResponse checkGuardrails(@Valid @RequestBody GuardrailCheckRequest request) {
        return responsibleAiGuardrailsService.checkAnswer(request.getAnswer());
    }

    @GetMapping("/api/lifepilot/evaluations/default")
    public List<EvaluationScenarioResponse> getDefaultEvaluations() {
        return evaluationService.getDefaultEvaluations();
    }
}
