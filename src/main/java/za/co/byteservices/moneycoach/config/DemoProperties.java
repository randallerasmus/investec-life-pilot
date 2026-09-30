package za.co.byteservices.moneycoach.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * The demo account.
 *
 * <p>Every feature here reads live Investec data, which means nobody without
 * credentials can see any of it work. The demo account fills that gap with
 * generated history so the app is runnable by someone evaluating it.
 *
 * <p>It is on by default and scoped to one account id, so a deployment with
 * real credentials is unaffected: any other account id goes to Investec as
 * before.
 */
@Configuration
@ConfigurationProperties(prefix = "lifepilot.demo")
public class DemoProperties {

    private boolean enabled = true;

    /** Requests for this account id are served from generated data. */
    private String accountId = "demo-account";

    /**
     * A second demo account with room to spare. The first is tight by design, so
     * every decision tested on it fails; this one lets a decision pass or come
     * close, which is the rest of what the simulator can say.
     */
    private String comfortableAccountId = "demo-comfortable";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getComfortableAccountId() {
        return comfortableAccountId;
    }

    public void setComfortableAccountId(String comfortableAccountId) {
        this.comfortableAccountId = comfortableAccountId;
    }

    /** True when this request should be served from generated data. */
    public boolean handles(String requestedAccountId) {
        return enabled && requestedAccountId != null
                && (requestedAccountId.equals(accountId) || requestedAccountId.equals(comfortableAccountId));
    }

    /** True when the request is for the account with room to spare. */
    public boolean isComfortable(String requestedAccountId) {
        return requestedAccountId != null && requestedAccountId.equals(comfortableAccountId);
    }
}
