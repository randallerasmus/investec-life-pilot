package za.co.byteservices.moneycoach.dto;

public class EvaluationScenarioResponse {

    private final String question;
    private final EvaluationCriteriaResponse criteria;

    public EvaluationScenarioResponse(String question, EvaluationCriteriaResponse criteria) {
        this.question = question;
        this.criteria = criteria;
    }

    public String getQuestion() {
        return question;
    }

    public EvaluationCriteriaResponse getCriteria() {
        return criteria;
    }
}
