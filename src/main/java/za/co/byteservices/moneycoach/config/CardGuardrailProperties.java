package za.co.byteservices.moneycoach.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Settings for the programmable card guardrail.
 *
 * <p>The defaults are deliberately harmless: monitor mode is on, so the service
 * reports the verdict it would have reached without ever declining a real
 * transaction. Declining someone at a till is not a behaviour to switch on by
 * accident, so it has to be asked for.
 */
@Configuration
@ConfigurationProperties(prefix = "lifepilot.card")
public class CardGuardrailProperties {

    /**
     * When true (the default) the guardrail always approves and reports what it
     * would otherwise have done. Turn it off only once the verdicts look right
     * for a real account.
     */
    private boolean monitorOnly = true;

    /**
     * Shared secret the card code must present on every call.
     *
     * <p>The card sandbox runs on Investec infrastructure, so these endpoints
     * have to be reachable from the public internet, and they answer with
     * balances and upcoming debits. Blank disables them outright rather than
     * leaving them open: an unconfigured deployment should refuse to answer,
     * not answer anyone.
     */
    private String sharedSecret = "";

    /** Held back from discretionary headroom so a decline is not the first warning. */
    private BigDecimal emergencyBuffer = new BigDecimal("500.00");

    /**
     * How long a spending snapshot stays usable. The card window is too short to
     * build one, so authorisation always answers from cache; this bounds how
     * stale that answer can be. Several refresh intervals long, so one failed
     * rebuild does not leave the next swipe without an answer.
     */
    private int snapshotTtlSeconds = 900;

    /**
     * How often every account the card has used is rebuilt in the background.
     * Read by the scheduler through its property placeholder; kept here so the
     * setting is documented with the others.
     */
    private int snapshotRefreshSeconds = 240;

    /**
     * Horizon used to total upcoming commitments when no recurring income was
     * detected and there is therefore no next payday to count towards.
     */
    private int fallbackHorizonDays = 30;

    /**
     * Merchant category codes that are never declined, whatever the forecast says.
     * Standard ISO 18245 codes: groceries, fuel, transport, pharmacy, medical and
     * utilities. Declining someone their petrol or their medication to protect a
     * projected balance does more harm than the shortfall it avoids.
     */
    private Set<String> essentialMerchantCategoryCodes = new LinkedHashSet<>(Set.of(
            "4111", // Local commuter transport
            "4121", // Taxicabs and rideshare
            "4900", // Utilities
            "5411", // Grocery stores and supermarkets
            "5412", // Convenience stores
            "5499", // Miscellaneous food stores
            "5541", // Service stations
            "5542", // Automated fuel dispensers
            "5912", // Drug stores and pharmacies
            "8011", // Doctors
            "8021", // Dentists
            "8062", // Hospitals
            "8099"  // Medical services
    ));

    public boolean isMonitorOnly() {
        return monitorOnly;
    }

    public String getSharedSecret() {
        return sharedSecret;
    }

    public void setSharedSecret(String sharedSecret) {
        this.sharedSecret = sharedSecret;
    }

    public void setMonitorOnly(boolean monitorOnly) {
        this.monitorOnly = monitorOnly;
    }

    public BigDecimal getEmergencyBuffer() {
        return emergencyBuffer;
    }

    public void setEmergencyBuffer(BigDecimal emergencyBuffer) {
        this.emergencyBuffer = emergencyBuffer;
    }

    public int getSnapshotTtlSeconds() {
        return snapshotTtlSeconds;
    }

    public void setSnapshotTtlSeconds(int snapshotTtlSeconds) {
        this.snapshotTtlSeconds = snapshotTtlSeconds;
    }

    public int getSnapshotRefreshSeconds() {
        return snapshotRefreshSeconds;
    }

    public void setSnapshotRefreshSeconds(int snapshotRefreshSeconds) {
        this.snapshotRefreshSeconds = snapshotRefreshSeconds;
    }

    public int getFallbackHorizonDays() {
        return fallbackHorizonDays;
    }

    public void setFallbackHorizonDays(int fallbackHorizonDays) {
        this.fallbackHorizonDays = fallbackHorizonDays;
    }

    public Set<String> getEssentialMerchantCategoryCodes() {
        return essentialMerchantCategoryCodes;
    }

    public void setEssentialMerchantCategoryCodes(Set<String> essentialMerchantCategoryCodes) {
        this.essentialMerchantCategoryCodes = essentialMerchantCategoryCodes;
    }
}
