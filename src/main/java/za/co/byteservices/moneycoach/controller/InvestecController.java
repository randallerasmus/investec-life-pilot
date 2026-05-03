package za.co.byteservices.moneycoach.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.byteservices.moneycoach.config.InvestecApiProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class InvestecController {

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
}
