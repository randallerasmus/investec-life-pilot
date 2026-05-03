package za.co.byteservices.moneycoach.service;

import org.springframework.stereotype.Service;
import za.co.byteservices.moneycoach.client.InvestecApiClient;
import za.co.byteservices.moneycoach.dto.InvestecAccountResponse;
import za.co.byteservices.moneycoach.dto.InvestecBalanceResponse;
import za.co.byteservices.moneycoach.dto.InvestecTokenResponse;
import za.co.byteservices.moneycoach.dto.InvestecTransactionResponse;

import java.time.LocalDate;

@Service
public class InvestecAccountService {

    private final InvestecAuthService authService;
    private final InvestecApiClient apiClient;

    public InvestecAccountService(InvestecAuthService authService,
                                  InvestecApiClient apiClient) {
        this.authService = authService;
        this.apiClient = apiClient;
    }

    public InvestecAccountResponse getAccounts() {
        InvestecTokenResponse token = authService.getAccessToken();

        if (token == null || token.getAccessToken() == null || token.getAccessToken().isBlank()) {
            throw new IllegalStateException("Could not retrieve Investec access token");
        }

        return apiClient.getAccounts(token.getAccessToken());
    }

    public InvestecBalanceResponse getBalance(String accountId) {
        InvestecTokenResponse token = authService.getAccessToken();

        if (token == null || token.getAccessToken() == null || token.getAccessToken().isBlank()) {
            throw new IllegalStateException("Could not retrieve Investec access token");
        }

        return apiClient.getBalance(token.getAccessToken(), accountId);
    }

    public InvestecTransactionResponse getTransactions(String accountId,
                                                       LocalDate fromDate,
                                                       LocalDate toDate) {

        LocalDate resolvedToDate = toDate != null ? toDate : LocalDate.now();
        LocalDate resolvedFromDate = fromDate != null ? fromDate : resolvedToDate.minusDays(30);

        InvestecTokenResponse token = authService.getAccessToken();

        if (token == null || token.getAccessToken() == null || token.getAccessToken().isBlank()) {
            throw new IllegalStateException("Could not retrieve Investec access token");
        }

        return apiClient.getTransactions(
                token.getAccessToken(),
                accountId,
                resolvedFromDate,
                resolvedToDate
        );
    }
}
