package za.co.byteservices.moneycoach.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import za.co.byteservices.moneycoach.model.LifePilotScenarioType;
import za.co.byteservices.moneycoach.model.MoneyCoachRiskLevel;
import za.co.byteservices.moneycoach.model.SurvivalStatus;

import java.math.BigDecimal;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

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
                "summary",
                "survival message",
                List.of("recommendation"),
                "disclaimer"
        );

        ObjectNode json = (ObjectNode) objectMapper.valueToTree(response);

        assertThat(json.fieldNames()).toIterable().containsExactlyInAnyOrder(
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
                "monthlyBufferAfterScenario",
                "monthlyShortfall",
                "safeToSpendDropPercent",
                "summary",
                "survivalMessage",
                "recommendations",
                "disclaimer"
        );

        assertThat(json.get("survivalStatus").asText()).isEqualTo("AFFORDABLE");
        assertThat(json.get("scenarioType").asText()).isEqualTo("HOME_RENOVATION");
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
