package za.co.byteservices.moneycoach.dto;

import za.co.byteservices.moneycoach.model.LifePilotScenarioType;

import java.math.BigDecimal;

public class LifePilotScenarioRequest {

    private String accountId;
    private BigDecimal bondOrRent;
    private BigDecimal schoolFees;
    private BigDecimal insurance;
    private BigDecimal groceries;
    private BigDecimal fuel;
    private BigDecimal subscriptions;
    private BigDecimal otherBills;
    private BigDecimal goalSavingAmount;
    private LifePilotScenarioType scenarioType;
    private String scenarioName;
    private BigDecimal monthlyCost;
    private BigDecimal onceOffCost;
    private Integer durationMonths;

    public LifePilotScenarioRequest() {
    }

    public LifePilotScenarioRequest(String accountId,
                                    BigDecimal bondOrRent,
                                    BigDecimal schoolFees,
                                    BigDecimal insurance,
                                    BigDecimal groceries,
                                    BigDecimal fuel,
                                    BigDecimal subscriptions,
                                    BigDecimal otherBills,
                                    BigDecimal goalSavingAmount,
                                    LifePilotScenarioType scenarioType,
                                    String scenarioName,
                                    BigDecimal monthlyCost,
                                    BigDecimal onceOffCost,
                                    Integer durationMonths) {
        this.accountId = accountId;
        this.bondOrRent = bondOrRent;
        this.schoolFees = schoolFees;
        this.insurance = insurance;
        this.groceries = groceries;
        this.fuel = fuel;
        this.subscriptions = subscriptions;
        this.otherBills = otherBills;
        this.goalSavingAmount = goalSavingAmount;
        this.scenarioType = scenarioType;
        this.scenarioName = scenarioName;
        this.monthlyCost = monthlyCost;
        this.onceOffCost = onceOffCost;
        this.durationMonths = durationMonths;
    }

    public String getAccountId() {
        return accountId;
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

    public BigDecimal getGoalSavingAmount() {
        return goalSavingAmount;
    }

    public LifePilotScenarioType getScenarioType() {
        return scenarioType;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public BigDecimal getMonthlyCost() {
        return monthlyCost;
    }

    public BigDecimal getOnceOffCost() {
        return onceOffCost;
    }

    public Integer getDurationMonths() {
        return durationMonths;
    }
}
