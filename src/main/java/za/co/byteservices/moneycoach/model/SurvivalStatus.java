package za.co.byteservices.moneycoach.model;

/**
 * Whether a life event survives contact with the user's monthly position.
 *
 * <p>This is the same judgement as {@link MoneyCoachRiskLevel}, worded for
 * someone deciding whether to commit rather than for a budgeting readout.
 * "Unaffordable" answers the question a simulator is actually asked; "critical"
 * describes an account state.
 */
public enum SurvivalStatus {

    AFFORDABLE,
    TIGHT,
    UNAFFORDABLE;

    public static SurvivalStatus from(MoneyCoachRiskLevel riskLevel) {
        return switch (riskLevel) {
            case CRITICAL -> UNAFFORDABLE;
            case TIGHT -> TIGHT;
            case HEALTHY -> AFFORDABLE;
        };
    }
}
