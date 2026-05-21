package za.co.byteservices.moneycoach.dto;

import jakarta.validation.constraints.NotBlank;

public class GuardrailCheckRequest {

    @NotBlank
    private String answer;

    public GuardrailCheckRequest() {
    }

    public GuardrailCheckRequest(String answer) {
        this.answer = answer;
    }

    public String getAnswer() {
        return answer;
    }
}
