package za.co.byteservices.moneycoach.dto;

import jakarta.validation.constraints.NotBlank;

public class KnowledgeSearchRequest {

    @NotBlank
    private String question;

    public KnowledgeSearchRequest() {
    }

    public KnowledgeSearchRequest(String question) {
        this.question = question;
    }

    public String getQuestion() {
        return question;
    }
}
