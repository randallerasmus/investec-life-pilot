package za.co.byteservices.moneycoach.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import za.co.byteservices.moneycoach.config.CardGuardrailProperties;
import za.co.byteservices.moneycoach.dto.CardAuthorizationRequest;
import za.co.byteservices.moneycoach.dto.CardAuthorizationResponse;
import za.co.byteservices.moneycoach.model.CardAssessment;
import za.co.byteservices.moneycoach.model.CardDecision;
import za.co.byteservices.moneycoach.service.CardAuthorizationService;
import za.co.byteservices.moneycoach.service.SpendingSnapshotService;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CardControllerTest {

    private final CardAuthorizationService authorizationService = mock(CardAuthorizationService.class);
    private final SpendingSnapshotService snapshotService = mock(SpendingSnapshotService.class);
    private final CardGuardrailProperties properties = new CardGuardrailProperties();
    private final CardController controller =
            new CardController(authorizationService, snapshotService, properties);

    private final CardAuthorizationRequest request =
            new CardAuthorizationRequest("acc-123", 34999, "ZAR", "Takealot", "5942", "card-1", "ref");

    @Test
    void authorizesWhenTheCardKeyMatches() {
        properties.setSharedSecret("s3cret");
        when(authorizationService.authorize(any())).thenReturn(response());

        CardAuthorizationResponse result = controller.authorize("s3cret", request);

        assertThat(result.getDecision()).isEqualTo(CardDecision.APPROVE);
    }

    @Test
    void rejectsAWrongCardKeyWithoutConsultingTheGuardrail() {
        properties.setSharedSecret("s3cret");

        assertThatThrownBy(() -> controller.authorize("wrong", request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        verifyNoInteractions(authorizationService);
    }

    @Test
    void rejectsAMissingCardKey() {
        properties.setSharedSecret("s3cret");

        assertThatThrownBy(() -> controller.authorize(null, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void refusesToServeAtAllWhileNoSecretIsConfigured() {
        // An unconfigured deployment must not answer with balances just because
        // nobody set a key.
        assertThatThrownBy(() -> controller.authorize("anything", request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

        verifyNoInteractions(authorizationService);
    }

    @Test
    void guardsTheSnapshotRefreshWithTheSameKey() {
        properties.setSharedSecret("s3cret");

        assertThatThrownBy(() -> controller.refreshSnapshot("wrong", "acc-123"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        verifyNoInteractions(snapshotService);
    }

    private CardAuthorizationResponse response() {
        return new CardAuthorizationResponse(
                CardDecision.APPROVE,
                CardAssessment.WITHIN_HEADROOM,
                true,
                new BigDecimal("349.99"),
                new BigDecimal("2000.00"),
                new BigDecimal("1650.01"),
                null,
                null,
                null,
                30L,
                false,
                "reason"
        );
    }
}
