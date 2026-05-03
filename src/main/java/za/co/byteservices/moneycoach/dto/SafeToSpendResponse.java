package za.co.byteservices.moneycoach.dto;

import java.math.BigDecimal;

public class SafeToSpendResponse {

    private String accountId;
    private BigDecimal availableBalance;
    private BigDecimal estimatedBills;
    private BigDecimal goalSavingAmount;
    private BigDecimal safeToSpend;
    private String currency;
    private String message;

    public SafeToSpendResponse(String accountId,
                               BigDecimal availableBalance,
                               BigDecimal estimatedBills,
                               BigDecimal goalSavingAmount,
                               BigDecimal safeToSpend,
                               String currency,
                               String message) {
        this.accountId = accountId;
        this.availableBalance = availableBalance;
        this.estimatedBills = estimatedBills;
        this.goalSavingAmount = goalSavingAmount;
        this.safeToSpend = safeToSpend;
        this.currency = currency;
        this.message = message;
    }

    public String getAccountId() {
        return accountId;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public BigDecimal getEstimatedBills() {
        return estimatedBills;
    }

    public BigDecimal getGoalSavingAmount() {
        return goalSavingAmount;
    }

    public BigDecimal getSafeToSpend() {
        return safeToSpend;
    }

    public String getCurrency() {
        return currency;
    }

    public String getMessage() {
        return message;
    }
}
