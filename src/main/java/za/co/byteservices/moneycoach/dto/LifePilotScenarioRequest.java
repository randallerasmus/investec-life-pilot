package za.co.byteservices.moneycoach.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import za.co.byteservices.moneycoach.model.LifePilotScenarioType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A life decision to test against the account's forecast.
 *
 * <p>Existing bills are not asked for. The forecast finds them in the
 * transaction history, and asking again would count them twice.
 */
public class LifePilotScenarioRequest {

    @NotBlank
    private String accountId;

    private LifePilotScenarioType scenarioType;

    private String scenarioName;

    /** New monthly cost, or monthly income given up for a decision like unpaid leave. */
    @DecimalMin(value = "0.0")
    private BigDecimal monthlyCost;

    @DecimalMin(value = "0.0")
    private BigDecimal onceOffCost;

    /** How many months the monthly cost runs for. Absent means for the whole forecast. */
    @Min(1)
    @Max(600)
    private Integer durationMonths;

    /** When the decision takes effect. Defaults to the first of next month. */
    private LocalDate startDate;

    public LifePilotScenarioRequest() {
    }

    public LifePilotScenarioRequest(String accountId,
                                    LifePilotScenarioType scenarioType,
                                    String scenarioName,
                                    BigDecimal monthlyCost,
                                    BigDecimal onceOffCost,
                                    Integer durationMonths,
                                    LocalDate startDate) {
        this.accountId = accountId;
        this.scenarioType = scenarioType;
        this.scenarioName = scenarioName;
        this.monthlyCost = monthlyCost;
        this.onceOffCost = onceOffCost;
        this.durationMonths = durationMonths;
        this.startDate = startDate;
    }

    public String getAccountId() {
        return accountId;
    }

    public LifePilotScenarioType getScenarioType() {
        return scenarioType;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public BigDecimal getMonthlyCost() {
        return monthlyCost;
    }

    public BigDecimal getOnceOffCost() {
        return onceOffCost;
    }

    public Integer getDurationMonths() {
        return durationMonths;
    }

    public LocalDate getStartDate() {
        return startDate;
    }
}
