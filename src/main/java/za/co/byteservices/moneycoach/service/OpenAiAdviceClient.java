package za.co.byteservices.moneycoach.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import za.co.byteservices.moneycoach.config.OpenAiProperties;
import za.co.byteservices.moneycoach.dto.MoneyCoachAdviceResponse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class OpenAiAdviceClient implements AiAdviceClient {

    private final OpenAiProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    public OpenAiAdviceClient(OpenAiProperties properties) {
        this.properties = properties;
    }

    @Override
    public Optional<String> generateAdvice(MoneyCoachAdviceResponse deterministicAdvice) {
        if (isBlank(properties.getApiKey()) || isBlank(properties.getModel())) {
            return Optional.empty();
        }

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    properties.getBaseUrl() + "/responses",
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody(deterministicAdvice), headers()),
                    JsonNode.class
            );

            return extractOutputText(response.getBody()).filter(text -> !text.isBlank());
        } catch (RestClientException ex) {
            return Optional.empty();
        }
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(properties.getApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    private Map<String, Object> requestBody(MoneyCoachAdviceResponse advice) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getModel());
        body.put("instructions", """
                You are a budgeting coach. Rewrite the supplied deterministic budgeting facts into a concise, supportive summary.
                Do not add new numbers. Do not recommend financial products. Do not claim to provide financial advice.
                Keep the response to three short sentences.
                """);
        body.put("input", input(advice));
        body.put("max_output_tokens", 180);
        return body;
    }

    private String input(MoneyCoachAdviceResponse advice) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "accountId", advice.getAccountId(),
                    "currency", advice.getCurrency(),
                    "availableBalance", advice.getAvailableBalance(),
                    "estimatedBills", advice.getEstimatedBills(),
                    "goalSavingAmount", advice.getGoalSavingAmount(),
                    "safeToSpend", advice.getSafeToSpend(),
                    "riskLevel", advice.getRiskLevel(),
                    "deterministicSummary", advice.getSummary(),
                    "recommendations", advice.getRecommendations(),
                    "disclaimer", advice.getDisclaimer()
            ));
        } catch (Exception ex) {
            return advice.getSummary();
        }
    }

    private Optional<String> extractOutputText(JsonNode body) {
        if (body == null) {
            return Optional.empty();
        }

        JsonNode outputText = body.get("output_text");
        if (outputText != null && outputText.isTextual()) {
            return Optional.of(outputText.asText());
        }

        JsonNode output = body.get("output");
        if (output == null || !output.isArray()) {
            return Optional.empty();
        }

        for (JsonNode item : output) {
            JsonNode content = item.get("content");
            if (content == null || !content.isArray()) {
                continue;
            }
            for (JsonNode contentItem : content) {
                JsonNode text = contentItem.get("text");
                if (text != null && text.isTextual()) {
                    return Optional.of(text.asText());
                }
            }
        }

        return Optional.empty();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
