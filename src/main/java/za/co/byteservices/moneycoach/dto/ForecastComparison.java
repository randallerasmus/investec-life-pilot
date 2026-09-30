package za.co.byteservices.moneycoach.dto;

/**
 * The same account projected twice from one read of its history: once as it
 * stands, and once with planned costs added. Both sides share the opening
 * balance and the detected payments, so any difference between them is the plan.
 */
public class ForecastComparison {

    private final BalanceForecastResponse baseline;
    private final BalanceForecastResponse withPlan;
    private final String currency;

    public ForecastComparison(BalanceForecastResponse baseline, BalanceForecastResponse withPlan, String currency) {
        this.baseline = baseline;
        this.withPlan = withPlan;
        this.currency = currency;
    }

    public BalanceForecastResponse getBaseline() {
        return baseline;
    }

    public BalanceForecastResponse getWithPlan() {
        return withPlan;
    }

    public String getCurrency() {
        return currency;
    }
}
