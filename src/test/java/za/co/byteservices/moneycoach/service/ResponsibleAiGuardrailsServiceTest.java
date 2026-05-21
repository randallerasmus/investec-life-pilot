package za.co.byteservices.moneycoach.service;

import org.junit.jupiter.api.Test;
import za.co.byteservices.moneycoach.dto.GuardrailCheckResponse;

import static org.assertj.core.api.Assertions.assertThat;

class ResponsibleAiGuardrailsServiceTest {

    private final ResponsibleAiGuardrailsService service = new ResponsibleAiGuardrailsService();

    @Test
    void rewritesRiskyFinancialClaimsWithDisclaimer() {
        GuardrailCheckResponse response = service.checkAnswer(
                "You should invest all your money in one stock because it will definitely go up."
        );

        assertThat(response.getWarnings()).isNotEmpty();
        assertThat(response.getWarnings()).anyMatch(warning -> warning.contains("guaranteed") || warning.contains("certainty"));
        assertThat(response.getSafeAnswer()).contains("not regulated financial advice");
        assertThat(response.isRewritten()).isTrue();
    }

    @Test
    void leavesLowRiskEducationalAnswerUntouched() {
        GuardrailCheckResponse response = service.checkAnswer(
                "Build a buffer before large purchases and review your recurring expenses first."
        );

        assertThat(response.getWarnings()).isEmpty();
        assertThat(response.getSafeAnswer()).contains("Build a buffer");
        assertThat(response.isRewritten()).isFalse();
    }
}
