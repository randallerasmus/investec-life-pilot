package za.co.byteservices.moneycoach.dto;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AdvancedSafeToSpendRequest that = (AdvancedSafeToSpendRequest) o;
        return Objects.equals(payday, that.payday) && Objects.equals(emergencyBuffer, that.emergencyBuffer) && Objects.equals(plannedPurchaseAmount, that.plannedPurchaseAmount) && Objects.equals(plannedPurchaseDescription, that.plannedPurchaseDescription);
    }

    @Override
    public int hashCode() {
        return Objects.hash(payday, emergencyBuffer, plannedPurchaseAmount, plannedPurchaseDescription);
    }
}
