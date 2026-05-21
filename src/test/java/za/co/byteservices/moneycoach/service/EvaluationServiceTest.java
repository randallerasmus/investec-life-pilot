package za.co.byteservices.moneycoach.service;

import org.junit.jupiter.api.Test;
import za.co.byteservices.moneycoach.dto.EvaluationScenarioResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EvaluationServiceTest {

    private final EvaluationService service = new EvaluationService();

    @Test
    void returnsDefaultEvaluationScenarios() {
        List<EvaluationScenarioResponse> responses = service.getDefaultEvaluations();

        assertThat(responses).hasSize(5);
        assertThat(responses)
                .extracting(EvaluationScenarioResponse::getQuestion)
                .contains("Can I afford this purchase?",
                        "Why is my spending high?",
                        "Will I run out of money before payday?",
                        "Which expenses can I reduce?",
                        "What changed compared to last month?");
        assertThat(responses.get(0).getCriteria().getGroundedness()).isNotBlank();
    }
}
