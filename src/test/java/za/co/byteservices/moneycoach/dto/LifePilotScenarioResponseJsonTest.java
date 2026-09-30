package za.co.byteservices.moneycoach.dto;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import za.co.byteservices.moneycoach.model.LifePilotScenarioType;
import za.co.byteservices.moneycoach.model.MoneyCoachRiskLevel;
import za.co.byteservices.moneycoach.model.SurvivalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the JSON key set of the scenario response.
 *
 * <p>The frontend reads these names directly. When they drifted apart the UI
 * silently rendered blanks rather than failing, so the contract is asserted
 * here instead of being discovered on screen.
 */
class LifePilotScenarioResponseJsonTest {

    // The mapper Spring Boot serialises responses with, so dates render as the client sees them.
    private final JsonMapper objectMapper = JsonMapper.builder().build();

    @Test
    void serialisesEveryFieldTheClientReads() throws Exception {
        LifePilotScenarioResponse response = new LifePilotScenarioResponse(
                "acc-123",
                LifePilotScenarioType.HOME_RENOVATION,
                "Home renovation",
                new BigDecimal("30000.00"),
                new BigDecimal("15000.00"),
                new BigDecimal("10000.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("10000.00"),
                12,
                "ZAR",
                MoneyCoachRiskLevel.HEALTHY,
                SurvivalStatus.AFFORDABLE,
                new BigDecimal("10000.00"),
                new BigDecimal("0.00"),
                new BigDecimal("33.3"),
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 24),
                null,
                false,
                null,
                null,
                "summary",
                "survival message",
                List.of("recommendation"),
                "disclaimer"
        );

        ObjectNode json = (ObjectNode) objectMapper.valueToTree(response);

        assertThat(json.propertyNames()).containsExactlyInAnyOrder(
                "accountId",
                "scenarioType",
                "scenarioName",
                "availableBalance",
                "currentSafeToSpend",
                "projectedSafeToSpend",
                "monthlyImpact",
                "onceOffImpact",
                "durationMonths",
                "currency",
                "riskLevel",
                "survivalStatus",
                "bufferAtLowestPoint",
                "shortfallAtLowestPoint",
                "safeToSpendDropPercent",
                "startDate",
                "lowestBalanceDate",
                "firstShortfallDate",
                "alreadyShortWithoutScenario",
                "baselineForecast",
                "scenarioForecast",
                "summary",
                "survivalMessage",
                "recommendations",
                "disclaimer"
        );

        assertThat(json.get("survivalStatus").asString()).isEqualTo("AFFORDABLE");
        assertThat(json.get("scenarioType").asString()).isEqualTo("HOME_RENOVATION");
        // The client parses these as ISO dates, not timestamps or arrays.
        assertThat(json.get("startDate").asString()).isEqualTo("2026-10-01");
    }

    @Test
    void keepsDropPercentPresentButNullWhenItCannotBeMeasured() throws Exception {
        LifePilotScenarioResponse response = new LifePilotScenarioResponse(
                "acc-123",
                LifePilotScenarioType.CUSTOM,
                "Custom event",
                new BigDecimal("100.00"),
                new BigDecimal("-500.00"),
                new BigDecimal("-900.00"),
                new BigDecimal("400.00"),
                BigDecimal.ZERO,
                null,
                "ZAR",
                MoneyCoachRiskLevel.CRITICAL,
                SurvivalStatus.UNAFFORDABLE,
                new BigDecimal("0.00"),
                new BigDecimal("900.00"),
                null,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 24),
                LocalDate.of(2026, 10, 21),
                true,
                null,
                null,
                "summary",
                "survival message",
                List.of(),
                "disclaimer"
        );

        ObjectNode json = (ObjectNode) objectMapper.valueToTree(response);

        // The key must survive so the client can tell "not measurable" from a
        // field it forgot to read.
        assertThat(json.has("safeToSpendDropPercent")).isTrue();
        assertThat(json.get("safeToSpendDropPercent").isNull()).isTrue();
    }
}
