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

    /** True when this request should be served from generated data. */
    public boolean handles(String requestedAccountId) {
        return enabled && accountId != null && accountId.equals(requestedAccountId);
    }
}
