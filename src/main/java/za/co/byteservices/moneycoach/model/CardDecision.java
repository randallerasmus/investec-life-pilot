package za.co.byteservices.moneycoach.model;

/**
 * What the card should do with an authorisation.
 *
 * <p>The card code turns this into the boolean the Investec runtime expects, so
 * anything other than an explicit DECLINE lets the transaction through.
 */
public enum CardDecision {
    APPROVE,
    DECLINE
}
