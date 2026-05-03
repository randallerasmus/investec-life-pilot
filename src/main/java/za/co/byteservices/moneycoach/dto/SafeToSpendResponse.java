package za.co.byteservices.moneycoach.dto;

import java.math.BigDecimal;

public class SafeToSpendResponse {

    private String accountId;
    private BigDecimal availableBalance;

    private BigDecimal bondOrRent;
    private BigDecimal schoolFees;
    private BigDecimal insurance;
    private BigDecimal groceries;
    private BigDecimal fuel;
    private BigDecimal subscriptions;
    private BigDecimal otherBills;

    private BigDecimal estimatedBills;
    private BigDecimal goalSavingAmount;
    private BigDecimal safeToSpend;

    private String currency;
    private String message;

    public SafeToSpendResponse(String accountId,
                               BigDecimal availableBalance,
                               BigDecimal bondOrRent,
                               BigDecimal schoolFees,
                               BigDecimal insurance,
                               BigDecimal groceries,
                               BigDecimal fuel,
                               BigDecimal subscriptions,
                               BigDecimal otherBills,
                               BigDecimal estimatedBills,
                               BigDecimal goalSavingAmount,
                               BigDecimal safeToSpend,
                               String currency,
                               String message) {
        this.accountId = accountId;
        this.availableBalance = availableBalance;
        this.bondOrRent = bondOrRent;
        this.schoolFees = schoolFees;
        this.insurance = insurance;
        this.groceries = groceries;
        this.fuel = fuel;
        this.subscriptions = subscriptions;
        this.otherBills = otherBills;
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

    public BigDecimal getBondOrRent() {
        return bondOrRent;
    }

    public BigDecimal getSchoolFees() {
        return schoolFees;
    }

    public BigDecimal getInsurance() {
        return insurance;
    }

    public BigDecimal getGroceries() {
        return groceries;
    }

    public BigDecimal getFuel() {
        return fuel;
    }

    public BigDecimal getSubscriptions() {
        return subscriptions;
    }

    public BigDecimal getOtherBills() {
        return otherBills;
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
