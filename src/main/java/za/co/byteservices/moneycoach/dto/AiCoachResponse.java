package za.co.byteservices.moneycoach.dto;

import za.co.byteservices.moneycoach.model.MoneyCoachRiskLevel;

import java.util.List;

public class AiCoachResponse {

    private final String answer;
    private final String accountId;
    private final MoneyCoachRiskLevel riskLevel;
    private final int confidence;
    private final List<String> basedOn;
    private final List<KnowledgeSearchResult> retrievedKnowledge;
    private final List<String> guardrailWarnings;

    public AiCoachResponse(String answer,
                           String accountId,
                           MoneyCoachRiskLevel riskLevel,
                           int confidence,
                           List<String> basedOn,
                           List<KnowledgeSearchResult> retrievedKnowledge,
                           List<String> guardrailWarnings) {
        this.answer = answer;
        this.accountId = accountId;
        this.riskLevel = riskLevel;
        this.confidence = confidence;
        this.basedOn = basedOn;
        this.retrievedKnowledge = retrievedKnowledge;
        this.guardrailWarnings = guardrailWarnings;
    }

    public String getAnswer() {
        return answer;
    }

    public String getAccountId() {
        return accountId;
    }

    public MoneyCoachRiskLevel getRiskLevel() {
        return riskLevel;
    }

    public int getConfidence() {
        return confidence;
    }

    public List<String> getBasedOn() {
        return basedOn;
    }

    public List<KnowledgeSearchResult> getRetrievedKnowledge() {
        return retrievedKnowledge;
    }

    public List<String> getGuardrailWarnings() {
        return guardrailWarnings;
    }
}
