package za.co.byteservices.moneycoach.client;

import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import za.co.byteservices.moneycoach.config.InvestecApiProperties;
import za.co.byteservices.moneycoach.dto.InvestecAccountResponse;
import za.co.byteservices.moneycoach.dto.InvestecBalanceResponse;

@Component
public class InvestecApiClient {

    private final InvestecApiProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    public InvestecApiClient(InvestecApiProperties properties) {
        this.properties = properties;
    }

    public InvestecAccountResponse getAccounts(String accessToken) {
        String url = properties.getBaseUrl() + "/za/pb/v1/accounts";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<InvestecAccountResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                InvestecAccountResponse.class
        );

        return response.getBody();
    }

    public InvestecBalanceResponse getBalance(String accessToken, String accountId) {
        String url = properties.getBaseUrl() + "/za/pb/v1/accounts/" + accountId + "/balance";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<InvestecBalanceResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                InvestecBalanceResponse.class
        );

        return response.getBody();
    }
}
