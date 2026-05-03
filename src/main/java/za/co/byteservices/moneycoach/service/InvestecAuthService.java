package za.co.byteservices.moneycoach.service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import za.co.byteservices.moneycoach.config.InvestecApiProperties;
import za.co.byteservices.moneycoach.dto.InvestecTokenResponse;

@Service
public class InvestecAuthService {

    private final InvestecApiProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    public InvestecAuthService(InvestecApiProperties properties) {
        this.properties = properties;
    }

    public InvestecTokenResponse getAccessToken() {
        String url = properties.getBaseUrl() + "/identity/v2/oauth2/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(properties.getClientId(), properties.getClientSecret());
        headers.set("x-api-key", properties.getApiKey());
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        LinkedMultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        HttpEntity<LinkedMultiValueMap<String, String>> request =
                new HttpEntity<>(body, headers);

        ResponseEntity<InvestecTokenResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                InvestecTokenResponse.class
        );

        return response.getBody();
    }
}
