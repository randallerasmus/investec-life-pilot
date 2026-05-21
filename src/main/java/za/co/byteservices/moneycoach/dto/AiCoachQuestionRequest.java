package za.co.byteservices.moneycoach.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AiCoachQuestionRequest {

    @NotBlank
    private String question;

    private LocalDate payday;

    @DecimalMin(value = "0.0")
    private BigDecimal emergencyBuffer;

    public AiCoachQuestionRequest() {
    }

    public AiCoachQuestionRequest(String question, LocalDate payday, BigDecimal emergencyBuffer) {
        this.question = question;
        this.payday = payday;
        this.emergencyBuffer = emergencyBuffer;
    }

    public String getQuestion() {
        return question;
    }

    public LocalDate getPayday() {
        return payday;
    }

    public BigDecimal getEmergencyBuffer() {
        return emergencyBuffer;
    }
}
