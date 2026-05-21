package za.co.byteservices.moneycoach.service;

import org.springframework.stereotype.Service;
import za.co.byteservices.moneycoach.dto.AdvancedSafeToSpendRequest;
import za.co.byteservices.moneycoach.dto.AdvancedSafeToSpendResponse;
import za.co.byteservices.moneycoach.dto.AiCoachQuestionRequest;
import za.co.byteservices.moneycoach.dto.AiCoachResponse;
import za.co.byteservices.moneycoach.dto.GuardrailCheckResponse;
import za.co.byteservices.moneycoach.dto.KnowledgeSearchResult;
import za.co.byteservices.moneycoach.dto.MoneyCoachAdviceResponse;
import za.co.byteservices.moneycoach.model.MoneyCoachRiskLevel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiCoachService {

    private static final Pattern MONEY_PATTERN = Pattern.compile("(?i)(?:r|zar)?\\s*(\\d+[\\d,]*(?:\\.\\d{1,2})?)");

    private final SafeToSpendEngineService safeToSpendEngineService;
    private final KnowledgeRagService knowledgeRagService;
    private final ResponsibleAiGuardrailsService guardrailsService;
    private final Optional<AiAdviceClient> aiAdviceClient;

    public AiCoachService(SafeToSpendEngineService safeToSpendEngineService,
                          KnowledgeRagService knowledgeRagService,
                          ResponsibleAiGuardrailsService guardrailsService,
                          Optional<AiAdviceClient> aiAdviceClient) {
        this.safeToSpendEngineService = safeToSpendEngineService;
        this.knowledgeRagService = knowledgeRagService;
        this.guardrailsService = guardrailsService;
        this.aiAdviceClient = aiAdviceClient;
    }

    public AiCoachResponse askQuestion(String accountId, AiCoachQuestionRequest request) {
        AdvancedSafeToSpendResponse affordability = safeToSpendEngineService.calculateAdvancedSafeToSpend(
                accountId,
                new AdvancedSafeToSpendRequest(
                        request.getPayday(),
                        request.getEmergencyBuffer(),
                        extractPlannedPurchaseAmount(request.getQuestion()),
                        request.getQuestion()
                )
        );

        List<KnowledgeSearchResult> retrievedKnowledge = knowledgeRagService.search(request.getQuestion());
        List<String> basedOn = new ArrayList<>();
        basedOn.add(String.format(Locale.US, "Current balance %.2f", affordability.getCurrentBalance()));
        basedOn.add(String.format(Locale.US, "Estimated recurring expenses %.2f", affordability.getEstimatedRecurringExpenses()));
        basedOn.add(String.format(Locale.US, "Safe to spend before purchase %.2f", affordability.getSafeToSpend()));
        retrievedKnowledge.stream()
                .map(result -> "Knowledge: " + result.getTitle() + " (" + result.getSource() + ")")
                .forEach(basedOn::add);

        String deterministicAnswer = deterministicAnswer(request.getQuestion(), affordability, retrievedKnowledge);
        String candidateAnswer = aiRewrite(accountId, affordability, deterministicAnswer).orElse(deterministicAnswer);
        GuardrailCheckResponse guardrailCheck = guardrailsService.checkAnswer(candidateAnswer);

        return new AiCoachResponse(
                guardrailCheck.getSafeAnswer(),
                accountId,
                affordability.getRiskLevel(),
                confidenceScore(affordability, retrievedKnowledge),
                basedOn,
                retrievedKnowledge,
                guardrailCheck.getWarnings()
        );
    }

    private Optional<String> aiRewrite(String accountId,
                                       AdvancedSafeToSpendResponse affordability,
                                       String deterministicAnswer) {
        MoneyCoachAdviceResponse bridgeResponse = new MoneyCoachAdviceResponse(
                accountId,
                money(affordability.getCurrentBalance()),
                money(affordability.getEstimatedRecurringExpenses()),
                money(affordability.getEmergencyBuffer()),
                money(affordability.getSafeToSpend()),
                "ZAR",
                affordability.getRiskLevel(),
                deterministicAnswer,
                List.of(affordability.getExplanation()),
                false,
                ResponsibleAiGuardrailsService.EDUCATIONAL_DISCLAIMER
        );

        return aiAdviceClient
                .flatMap(client -> client.generateAdvice(bridgeResponse))
                .filter(text -> !text.isBlank());
    }

    private String deterministicAnswer(String question,
                                       AdvancedSafeToSpendResponse affordability,
                                       List<KnowledgeSearchResult> retrievedKnowledge) {
        BigDecimal remainingAfterPurchase = affordability.getSafeToSpend().subtract(affordability.getPlannedPurchaseAmount());
        String knowledgeSummary = retrievedKnowledge.isEmpty()
                ? "No extra knowledge snippets matched strongly enough, so this answer leans on the deterministic account calculation."
                : "Relevant knowledge points to " + retrievedKnowledge.get(0).getTitle().toLowerCase(Locale.US) + ".";

        if (affordability.getRiskLevel() == MoneyCoachRiskLevel.CRITICAL) {
            return String.format(
                    Locale.US,
                    "For the question '%s', the current numbers do not support this comfortably. After recurring expenses and your emergency buffer, the remaining amount after the planned purchase is %.2f. %s %s",
                    question,
                    remainingAfterPurchase,
                    knowledgeSummary,
                    ResponsibleAiGuardrailsService.EDUCATIONAL_DISCLAIMER
            );
        }

        if (affordability.getRiskLevel() == MoneyCoachRiskLevel.TIGHT) {
            return String.format(
                    Locale.US,
                    "For the question '%s', the purchase may be possible but the buffer is tight before payday. After recurring expenses and your emergency buffer, the remaining amount after the planned purchase is %.2f. %s %s",
                    question,
                    remainingAfterPurchase,
                    knowledgeSummary,
                    ResponsibleAiGuardrailsService.EDUCATIONAL_DISCLAIMER
            );
        }

        return String.format(
                Locale.US,
                "For the question '%s', the purchase looks manageable on the available data. After recurring expenses and your emergency buffer, the remaining amount after the planned purchase is %.2f. %s %s",
                question,
                remainingAfterPurchase,
                knowledgeSummary,
                ResponsibleAiGuardrailsService.EDUCATIONAL_DISCLAIMER
        );
    }

    private int confidenceScore(AdvancedSafeToSpendResponse affordability, List<KnowledgeSearchResult> retrievedKnowledge) {
        int score = 55;
        if (affordability.getCurrentBalance().compareTo(BigDecimal.ZERO) > 0) {
            score += 15;
        }
        if (affordability.getEstimatedRecurringExpenses().compareTo(BigDecimal.ZERO) > 0) {
            score += 10;
        }
        if (!retrievedKnowledge.isEmpty()) {
            score += Math.min(15, retrievedKnowledge.get(0).getRelevanceScore() * 3);
        }
        return Math.min(score, 100);
    }

    private BigDecimal extractPlannedPurchaseAmount(String question) {
        if (question == null) {
            return BigDecimal.ZERO;
        }
        Matcher matcher = MONEY_PATTERN.matcher(question);
        if (!matcher.find()) {
            return BigDecimal.ZERO;
        }
        String normalized = matcher.group(1).replace(",", "");
        return money(new BigDecimal(normalized));
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value.setScale(2, RoundingMode.HALF_UP);
    }
}
