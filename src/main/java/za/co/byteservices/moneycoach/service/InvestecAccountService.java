package za.co.byteservices.moneycoach.service;

import org.springframework.stereotype.Service;
import za.co.byteservices.moneycoach.client.InvestecApiClient;
import za.co.byteservices.moneycoach.config.DemoProperties;
import za.co.byteservices.moneycoach.dto.InvestecAccountResponse;
import za.co.byteservices.moneycoach.dto.InvestecBalanceResponse;
import za.co.byteservices.moneycoach.dto.InvestecTokenResponse;
import za.co.byteservices.moneycoach.dto.InvestecTransactionResponse;

import java.time.LocalDate;

/**
 * Fetches account data, from Investec or from the demo generator.
 *
 * <p>The demo account is intercepted here rather than further down, so every
 * feature built on this service — safe-to-spend, advice, scenarios, the
 * forecast, the AI coach and the card guardrail — works against it without
 * knowing it exists.
 */
@Service
public class InvestecAccountService {

    private final InvestecAuthService authService;
    private final InvestecApiClient apiClient;
    private final DemoProperties demoProperties;
    private final DemoAccountData demoAccountData;

    public InvestecAccountService(InvestecAuthService authService,
                                  InvestecApiClient apiClient,
                                  DemoProperties demoProperties,
                                  DemoAccountData demoAccountData) {
        this.authService = authService;
        this.apiClient = apiClient;
        this.demoProperties = demoProperties;
        this.demoAccountData = demoAccountData;
    }

    public InvestecAccountResponse getAccounts() {
        if (!demoProperties.isEnabled()) {
            return apiClient.getAccounts(accessToken());
        }

        // Without credentials the demo account is the only thing to list, and
        // failing here would hide it behind the very problem it exists to solve.
        try {
            return demoAccountData.accountsAlongside(apiClient.getAccounts(accessToken()));
        } catch (RuntimeException ex) {
            return demoAccountData.accounts();
        }
    }

    public InvestecBalanceResponse getBalance(String accountId) {
        if (demoProperties.handles(accountId)) {
            return demoAccountData.balance(accountId, LocalDate.now());
        }

        return apiClient.getBalance(accessToken(), accountId);
    }

    public InvestecTransactionResponse getTransactions(String accountId,
                                                       LocalDate fromDate,
                                                       LocalDate toDate) {

        LocalDate resolvedToDate = toDate != null ? toDate : LocalDate.now();
        LocalDate resolvedFromDate = fromDate != null ? fromDate : resolvedToDate.minusDays(30);

        if (demoProperties.handles(accountId)) {
            return demoAccountData.transactions(accountId, resolvedFromDate, resolvedToDate);
        }

        return apiClient.getTransactions(
                accessToken(),
                accountId,
                resolvedFromDate,
                resolvedToDate
        );
    }

    private String accessToken() {
        InvestecTokenResponse token = authService.getAccessToken();

        if (token == null || token.getAccessToken() == null || token.getAccessToken().isBlank()) {
            throw new IllegalStateException("Could not retrieve Investec access token");
        }

        return token.getAccessToken();
    }
}
