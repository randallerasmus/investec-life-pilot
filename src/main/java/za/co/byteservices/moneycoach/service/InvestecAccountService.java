package za.co.byteservices.moneycoach.service;

import org.springframework.stereotype.Service;
import za.co.byteservices.moneycoach.client.InvestecApiClient;
import za.co.byteservices.moneycoach.dto.InvestecAccountResponse;
import za.co.byteservices.moneycoach.dto.InvestecTokenResponse;

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
}
