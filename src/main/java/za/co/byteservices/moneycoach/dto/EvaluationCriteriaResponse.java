package za.co.byteservices.moneycoach.dto;

public class EvaluationCriteriaResponse {

    private final String retrievalQuality;
    private final String calculationCorrectness;
    private final String groundedness;
    private final String hallucinationRisk;
    private final String guardrailCompliance;

    public EvaluationCriteriaResponse(String retrievalQuality,
                                      String calculationCorrectness,
                                      String groundedness,
                                      String hallucinationRisk,
                                      String guardrailCompliance) {
        this.retrievalQuality = retrievalQuality;
        this.calculationCorrectness = calculationCorrectness;
        this.groundedness = groundedness;
        this.hallucinationRisk = hallucinationRisk;
        this.guardrailCompliance = guardrailCompliance;
    }

    public String getRetrievalQuality() {
        return retrievalQuality;
    }

    public String getCalculationCorrectness() {
        return calculationCorrectness;
    }

    public String getGroundedness() {
        return groundedness;
    }

    public String getHallucinationRisk() {
        return hallucinationRisk;
    }

    public String getGuardrailCompliance() {
        return guardrailCompliance;
    }
}
