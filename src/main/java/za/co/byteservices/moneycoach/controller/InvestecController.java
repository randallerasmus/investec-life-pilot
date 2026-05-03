package za.co.byteservices.moneycoach.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.byteservices.moneycoach.config.InvestecApiProperties;
import za.co.byteservices.moneycoach.dto.InvestecAccountResponse;
import za.co.byteservices.moneycoach.dto.InvestecTokenResponse;
import za.co.byteservices.moneycoach.service.InvestecAccountService;
import za.co.byteservices.moneycoach.service.InvestecAuthService;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class InvestecController {

    @Autowired
    InvestecAuthService  investecAuthService;

    @Autowired
    InvestecAccountService accountService;

    private final InvestecApiProperties properties;

    public InvestecController(InvestecApiProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/api/investec/config-check")
    public Map<String, Object> configCheck() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("baseUrl", properties.getBaseUrl());
        response.put("clientIdLoaded", properties.getClientId() != null && !properties.getClientId().isBlank());
        response.put("clientSecretLoaded", properties.getClientSecret() != null && !properties.getClientSecret().isBlank());
        response.put("apiKeyLoaded", properties.getApiKey() != null && !properties.getApiKey().isBlank());
        return response;
    }

    @GetMapping("/api/investec/token-check")
    public Map<String, Object> tokenCheck() {
        InvestecTokenResponse token = investecAuthService.getAccessToken();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("tokenReceived", token != null && token.getAccessToken() != null);
        response.put("tokenType", token != null ? token.getTokenType() : null);
        response.put("expiresIn", token != null ? token.getExpiresIn() : null);
        response.put("scope", token != null ? token.getScope() : null);

        return response;
    }

    @GetMapping("/api/investec/accounts")
    public InvestecAccountResponse getAccounts() {
        return accountService.getAccounts();
    }
}
