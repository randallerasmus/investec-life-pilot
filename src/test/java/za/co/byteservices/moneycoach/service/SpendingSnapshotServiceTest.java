package za.co.byteservices.moneycoach.service;

import org.junit.jupiter.api.Test;
import za.co.byteservices.moneycoach.config.CardGuardrailProperties;
import za.co.byteservices.moneycoach.dto.BalanceForecastRequest;
import za.co.byteservices.moneycoach.dto.BalanceForecastResponse;
import za.co.byteservices.moneycoach.dto.CashflowRisk;
import za.co.byteservices.moneycoach.dto.RecurringPayment;
import za.co.byteservices.moneycoach.dto.SpendingSnapshot;
import za.co.byteservices.moneycoach.model.MoneyCoachRiskLevel;
import za.co.byteservices.moneycoach.model.RecurringCadence;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpendingSnapshotServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-02T10:00:00Z");

    private final BalanceForecastService forecastService = mock(BalanceForecastService.class);
    private final CardGuardrailProperties properties = new CardGuardrailProperties();
    private final SpendingSnapshotService service = new SpendingSnapshotService(forecastService, properties);

    @Test
    void countsOnlyTheDebitsDueBeforeTheNextPaydayAsCommitted() {
        whenForecastHas(
                new BigDecimal("13200.00"),
                List.of(
                        recurring("Bond", "8500.00", LocalDate.of(2026, 9, 5)),
                        recurring("Car finance", "2400.00", LocalDate.of(2026, 9, 20)),
                        // Falls after payday, so it is next month's problem.
                        recurring("School fees", "6000.00", LocalDate.of(2026, 10, 1))
                ),
                List.of(recurring("Salary", "34000.00", LocalDate.of(2026, 9, 25)))
        );

        SpendingSnapshot snapshot = service.refresh("acc-123", NOW);

        assertThat(snapshot.getCommittedBeforeNextIncome()).isEqualByComparingTo("10900.00");
        assertThat(snapshot.getNextIncomeDate()).isEqualTo(LocalDate.of(2026, 9, 25));
        // 13200 - 10900 committed - 500 buffer
        assertThat(snapshot.getDiscretionaryHeadroom()).isEqualByComparingTo("1800.00");
    }

    @Test
    void fallsBackToAFixedHorizonWhenNoIncomeWasDetected() {
        properties.setFallbackHorizonDays(30);
        whenForecastHas(
                new BigDecimal("5000.00"),
                List.of(
                        recurring("Rent", "3000.00", LocalDate.of(2026, 9, 20)),
                        // Beyond the 30-day fallback window from 2 September.
                        recurring("Insurance", "900.00", LocalDate.of(2026, 10, 15))
                ),
                List.of()
        );

        SpendingSnapshot snapshot = service.refresh("acc-123", NOW);

        assertThat(snapshot.getNextIncomeDate()).isNull();
        assertThat(snapshot.getCommittedBeforeNextIncome()).isEqualByComparingTo("3000.00");
        assertThat(snapshot.getDiscretionaryHeadroom()).isEqualByComparingTo("1500.00");
    }

    @Test
    void clampsHeadroomAtZeroWhenCommitmentsAlreadyExceedTheBalance() {
        whenForecastHas(
                new BigDecimal("1000.00"),
                List.of(recurring("Bond", "8500.00", LocalDate.of(2026, 9, 5))),
                List.of(recurring("Salary", "34000.00", LocalDate.of(2026, 9, 25)))
        );

        SpendingSnapshot snapshot = service.refresh("acc-123", NOW);

        // Negative headroom would let a later subtraction read as affordable.
        assertThat(snapshot.getDiscretionaryHeadroom()).isEqualByComparingTo("0.00");
    }

    @Test
    void servesACachedSnapshotUntilItsTimeToLiveExpires() {
        whenForecastHas(new BigDecimal("13200.00"), List.of(), List.of());
        properties.setSnapshotTtlSeconds(300);

        service.refresh("acc-123", NOW);

        assertThat(service.current("acc-123", NOW.plusSeconds(299))).isPresent();
        assertThat(service.current("acc-123", NOW.plusSeconds(301))).isEmpty();
    }

    @Test
    void returnsEmptyForAnAccountThatWasNeverRefreshed() {
        assertThat(service.current("never-seen", NOW)).isEmpty();
    }

    private RecurringPayment recurring(String merchant, String amount, LocalDate nextDueDate) {
        return new RecurringPayment(
                merchant,
                new BigDecimal(amount),
                RecurringCadence.MONTHLY,
                6,
                nextDueDate.minusMonths(6),
                nextDueDate.minusMonths(1),
                nextDueDate,
                30,
                new BigDecimal("0.01"),
                new BigDecimal("0.90")
        );
    }

    private void whenForecastHas(BigDecimal openingBalance,
                                 List<RecurringPayment> expenses,
                                 List<RecurringPayment> income) {
        when(forecastService.forecast(eq("acc-123"), any(BalanceForecastRequest.class), any(LocalDate.class)))
                .thenReturn(new BalanceForecastResponse(
                        "acc-123",
                        LocalDate.of(2026, 9, 2),
                        45,
                        openingBalance,
                        new BigDecimal("20000.00"),
                        new BigDecimal("1200.00"),
                        LocalDate.of(2026, 9, 24),
                        MoneyCoachRiskLevel.TIGHT,
                        new BigDecimal("34000.00"),
                        new BigDecimal("12400.00"),
                        new BigDecimal("385.00"),
                        expenses,
                        income,
                        List.of(new CashflowRisk(
                                LocalDate.of(2026, 9, 20),
                                LocalDate.of(2026, 9, 24),
                                5,
                                new BigDecimal("1200.00"),
                                LocalDate.of(2026, 9, 24),
                                MoneyCoachRiskLevel.TIGHT,
                                "message"
                        )),
                        List.of(),
                        List.of(),
                        "summary",
                        false
                ));
    }
}
