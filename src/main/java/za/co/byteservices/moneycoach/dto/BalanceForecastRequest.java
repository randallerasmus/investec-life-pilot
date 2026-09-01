package za.co.byteservices.moneycoach.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Inputs for a forward balance projection. Every field is optional; the service
 * falls back to detected history and documented defaults when one is absent.
 */
public class BalanceForecastRequest {

    @Min(1)
    @Max(365)
    private Integer horizonDays;

    /** Balance to warn below. Defaults to zero, so only going into overdraft warns. */
    @DecimalMin(value = "0.0")
    private BigDecimal minimumBalanceThreshold;

    /** Overrides detected salary when history is too short to find it. */
    @DecimalMin(value = "0.0")
    private BigDecimal expectedMonthlyIncome;

    /** Anchors the first income date when income is supplied rather than detected. */
    private LocalDate nextIncomeDate;

    /** Set false to project only known recurring items, ignoring day-to-day spending. */
    private Boolean includeDiscretionarySpend;

    public BalanceForecastRequest() {
    }

    public BalanceForecastRequest(Integer horizonDays,
                                  BigDecimal minimumBalanceThreshold,
                                  BigDecimal expectedMonthlyIncome,
                                  LocalDate nextIncomeDate,
                                  Boolean includeDiscretionarySpend) {
        this.horizonDays = horizonDays;
        this.minimumBalanceThreshold = minimumBalanceThreshold;
        this.expectedMonthlyIncome = expectedMonthlyIncome;
        this.nextIncomeDate = nextIncomeDate;
        this.includeDiscretionarySpend = includeDiscretionarySpend;
    }

    public Integer getHorizonDays() {
        return horizonDays;
    }

    public BigDecimal getMinimumBalanceThreshold() {
        return minimumBalanceThreshold;
    }

    public BigDecimal getExpectedMonthlyIncome() {
        return expectedMonthlyIncome;
    }

    public LocalDate getNextIncomeDate() {
        return nextIncomeDate;
    }

    public Boolean getIncludeDiscretionarySpend() {
        return includeDiscretionarySpend;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        BalanceForecastRequest that = (BalanceForecastRequest) o;
        return Objects.equals(horizonDays, that.horizonDays)
                && Objects.equals(minimumBalanceThreshold, that.minimumBalanceThreshold)
                && Objects.equals(expectedMonthlyIncome, that.expectedMonthlyIncome)
                && Objects.equals(nextIncomeDate, that.nextIncomeDate)
                && Objects.equals(includeDiscretionarySpend, that.includeDiscretionarySpend);
    }

    @Override
    public int hashCode() {
        return Objects.hash(horizonDays, minimumBalanceThreshold, expectedMonthlyIncome, nextIncomeDate,
                includeDiscretionarySpend);
    }
}
