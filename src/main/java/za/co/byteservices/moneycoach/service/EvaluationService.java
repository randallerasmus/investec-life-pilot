package za.co.byteservices.moneycoach.service;

import org.springframework.stereotype.Service;
import za.co.byteservices.moneycoach.dto.EvaluationCriteriaResponse;
import za.co.byteservices.moneycoach.dto.EvaluationScenarioResponse;

import java.util.List;

@Service
public class EvaluationService {

    public List<EvaluationScenarioResponse> getDefaultEvaluations() {
        return List.of(
                scenario("Can I afford this purchase?"),
                scenario("Why is my spending high?"),
                scenario("Will I run out of money before payday?"),
                scenario("Which expenses can I reduce?"),
                scenario("What changed compared to last month?")
        );
    }

    private EvaluationScenarioResponse scenario(String question) {
        return new EvaluationScenarioResponse(
                question,
                new EvaluationCriteriaResponse(
                        "Retrieved snippets should directly match the financial question and mention the relevant budgeting concept.",
                        "Balance, buffers, and purchase impacts must reconcile with the deterministic safe-to-spend calculation.",
                        "The answer should only cite account balances, transactions, and retrieved knowledge that are actually available.",
                        "The answer should avoid invented categories, future certainty, and unsupported product recommendations.",
                        "The answer should include educational framing and avoid regulated advice phrased as certainty."
                )
        );
    }
}
