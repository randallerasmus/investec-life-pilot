package za.co.byteservices.moneycoach.service;

import org.junit.jupiter.api.Test;
import za.co.byteservices.moneycoach.config.CardGuardrailProperties;
import za.co.byteservices.moneycoach.dto.CardAuthorizationRequest;
import za.co.byteservices.moneycoach.dto.CardAuthorizationResponse;
import za.co.byteservices.moneycoach.dto.SpendingSnapshot;
import za.co.byteservices.moneycoach.model.CardAssessment;
import za.co.byteservices.moneycoach.model.CardDecision;
import za.co.byteservices.moneycoach.model.MoneyCoachRiskLevel;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class CardAuthorizationServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-02T10:00:00Z");

    private final SpendingSnapshotService snapshotService = mock(SpendingSnapshotService.class);
    private final CardGuardrailProperties properties = new CardGuardrailProperties();
    private final CardAuthorizationService service = new CardAuthorizationService(snapshotService, properties);

    @Test
    void approvesWhenTheAmountFitsTheHeadroomLeftBeforePayday() {
        properties.setMonitorOnly(false);
        whenSnapshotIs(new BigDecimal("2000.00"), NOW.minusSeconds(30));

        CardAuthorizationResponse response = service.authorize(authorization(34999, "5942"), NOW);

        assertThat(response.getDecision()).isEqualTo(CardDecision.APPROVE);
        assertThat(response.getAssessment()).isEqualTo(CardAssessment.WITHIN_HEADROOM);
        assertThat(response.getAmount()).isEqualByComparingTo("349.99");
        assertThat(response.getHeadroomAfterTransaction()).isEqualByComparingTo("1650.01");
        assertThat(response.getSnapshotAgeSeconds()).isEqualTo(30L);
        assertThat(response.getReason()).contains("fits").contains("before income lands on 2026-09-25");
    }

    @Test
    void declinesWhenTheAmountReachesIntoMoneyCommittedToUpcomingDebits() {
        properties.setMonitorOnly(false);
        whenSnapshotIs(new BigDecimal("300.00"), NOW);

        CardAuthorizationResponse response = service.authorize(authorization(120000, "5942"), NOW);

        assertThat(response.getDecision()).isEqualTo(CardDecision.DECLINE);
        assertThat(response.getAssessment()).isEqualTo(CardAssessment.EXCEEDS_HEADROOM);
        assertThat(response.getHeadroomAfterTransaction()).isEqualByComparingTo("-900.00");
        assertThat(response.getReason())
                .contains("exceeds")
                .contains("already committed to 12400.00 of upcoming debits");
    }

    @Test
    void approvesAnOverspendWhileMonitorModeIsOn() {
        // Monitor mode is the default, so this is the out-of-the-box behaviour.
        whenSnapshotIs(new BigDecimal("300.00"), NOW);

        CardAuthorizationResponse response = service.authorize(authorization(120000, "5942"), NOW);

        assertThat(response.getDecision()).isEqualTo(CardDecision.APPROVE);
        assertThat(response.isMonitorOnly()).isTrue();
        // The verdict is still recorded, which is the point of monitoring.
        assertThat(response.getAssessment()).isEqualTo(CardAssessment.EXCEEDS_HEADROOM);
    }

    @Test
    void neverDeclinesAnEssentialCategoryAndDoesNotEvenConsultTheSnapshot() {
        properties.setMonitorOnly(false);

        // 5411 is groceries: no forecast justifies declining it.
        CardAuthorizationResponse response = service.authorize(authorization(500000, "5411"), NOW);

        assertThat(response.getDecision()).isEqualTo(CardDecision.APPROVE);
        assertThat(response.getAssessment()).isEqualTo(CardAssessment.ESSENTIAL_CATEGORY);
        verifyNoMoreInteractions(snapshotService);
    }

    @Test
    void standsAsideWhenThereIsNoCurrentSnapshot() {
        properties.setMonitorOnly(false);
        when(snapshotService.current(eq("acc-123"), any())).thenReturn(Optional.empty());

        CardAuthorizationResponse response = service.authorize(authorization(9999999, "5942"), NOW);

        assertThat(response.getDecision()).isEqualTo(CardDecision.APPROVE);
        assertThat(response.getAssessment()).isEqualTo(CardAssessment.NO_FORECAST_DATA);
        assertThat(response.getDiscretionaryHeadroom()).isNull();
        verify(snapshotService).current("acc-123", NOW);
    }

    @Test
    void treatsAnExactlyExhaustingAmountAsFitting() {
        properties.setMonitorOnly(false);
        whenSnapshotIs(new BigDecimal("349.99"), NOW);

        CardAuthorizationResponse response = service.authorize(authorization(34999, "5942"), NOW);

        assertThat(response.getDecision()).isEqualTo(CardDecision.APPROVE);
        assertThat(response.getAssessment()).isEqualTo(CardAssessment.WITHIN_HEADROOM);
        assertThat(response.getHeadroomAfterTransaction()).isEqualByComparingTo("0.00");
    }

    @Test
    void approvesWhenTheCategoryCodeIsMissing() {
        properties.setMonitorOnly(false);
        whenSnapshotIs(new BigDecimal("2000.00"), NOW);

        CardAuthorizationResponse response = service.authorize(
                new CardAuthorizationRequest("acc-123", 5000, "ZAR", "Unknown", null, "card-1", "ref"),
                NOW
        );

        // An absent category is not an essential one, so the normal rule applies.
        assertThat(response.getAssessment()).isEqualTo(CardAssessment.WITHIN_HEADROOM);
        assertThat(response.getDecision()).isEqualTo(CardDecision.APPROVE);
    }

    private CardAuthorizationRequest authorization(long centsAmount, String merchantCategoryCode) {
        return new CardAuthorizationRequest(
                "acc-123",
                centsAmount,
                "ZAR",
                "Takealot",
                merchantCategoryCode,
                "card-1",
                "ref"
        );
    }

    private void whenSnapshotIs(BigDecimal headroom, Instant generatedAt) {
        when(snapshotService.current(eq("acc-123"), any())).thenReturn(Optional.of(new SpendingSnapshot(
                "acc-123",
                generatedAt,
                new BigDecimal("13200.00"),
                new BigDecimal("12400.00"),
                new BigDecimal("500.00"),
                headroom,
                LocalDate.of(2026, 9, 25),
                MoneyCoachRiskLevel.TIGHT,
                LocalDate.of(2026, 9, 20),
                false
        )));
    }
}
