package za.co.byteservices.moneycoach.dto;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AdvancedSafeToSpendRequest {

    private LocalDate payday;

    @DecimalMin(value = "0.0")
    private BigDecimal emergencyBuffer;

    @DecimalMin(value = "0.0")
    private BigDecimal plannedPurchaseAmount;

    private String plannedPurchaseDescription;

    public AdvancedSafeToSpendRequest() {
    }

    public AdvancedSafeToSpendRequest(LocalDate payday,
                                      BigDecimal emergencyBuffer,
                                      BigDecimal plannedPurchaseAmount,
                                      String plannedPurchaseDescription) {
        this.payday = payday;
        this.emergencyBuffer = emergencyBuffer;
        this.plannedPurchaseAmount = plannedPurchaseAmount;
        this.plannedPurchaseDescription = plannedPurchaseDescription;
    }

    public LocalDate getPayday() {
        return payday;
    }

    public BigDecimal getEmergencyBuffer() {
        return emergencyBuffer;
    }

    public BigDecimal getPlannedPurchaseAmount() {
        return plannedPurchaseAmount;
    }

    public String getPlannedPurchaseDescription() {
        return plannedPurchaseDescription;
    }
}
