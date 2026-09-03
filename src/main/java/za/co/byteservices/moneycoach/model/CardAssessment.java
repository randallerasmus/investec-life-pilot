package za.co.byteservices.moneycoach.model;

/**
 * Why the guardrail reached its decision.
 *
 * <p>Kept separate from {@link CardDecision} so monitor mode stays meaningful:
 * the assessment records what the forecast concluded even on the many
 * authorisations that were approved regardless.
 */
public enum CardAssessment {

    /** The amount fits inside the headroom left before the next payday. */
    WITHIN_HEADROOM,

    /** The amount would eat into money already committed to upcoming debits. */
    EXCEEDS_HEADROOM,

    /** An essential merchant category, which is never declined. */
    ESSENTIAL_CATEGORY,

    /** No usable forecast, so the guardrail stood aside. */
    NO_FORECAST_DATA
}
