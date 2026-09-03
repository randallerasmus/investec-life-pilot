package za.co.byteservices.moneycoach.dto;

import za.co.byteservices.moneycoach.model.MoneyCoachRiskLevel;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A forecast reduced to what a card authorisation actually needs.
 *
 * <p>Building a projection takes an OAuth token, a balance call, six months of
 * transactions and a day-by-day walk. The card gets two seconds. So the
 * expensive work happens away from the swipe and collapses to this: how much
 * can be spent before money already promised to upcoming debits is touched.
 */
public class SpendingSnapshot {

    private final String accountId;
    private final Instant generatedAt;
    private final BigDecimal availableBalance;
    private final BigDecimal committedBeforeNextIncome;
    private final BigDecimal emergencyBuffer;
    private final BigDecimal discretionaryHeadroom;
    private final LocalDate nextIncomeDate;
    private final MoneyCoachRiskLevel riskLevel;
    private final LocalDate nextRiskDate;
    private final boolean fallbackUsed;

    public SpendingSnapshot(String accountId,
                            Instant generatedAt,
                            BigDecimal availableBalance,
                            BigDecimal committedBeforeNextIncome,
                            BigDecimal emergencyBuffer,
                            BigDecimal discretionaryHeadroom,
                            LocalDate nextIncomeDate,
                            MoneyCoachRiskLevel riskLevel,
                            LocalDate nextRiskDate,
                            boolean fallbackUsed) {
        this.accountId = accountId;
        this.generatedAt = generatedAt;
        this.availableBalance = availableBalance;
        this.committedBeforeNextIncome = committedBeforeNextIncome;
        this.emergencyBuffer = emergencyBuffer;
        this.discretionaryHeadroom = discretionaryHeadroom;
        this.nextIncomeDate = nextIncomeDate;
        this.riskLevel = riskLevel;
        this.nextRiskDate = nextRiskDate;
        this.fallbackUsed = fallbackUsed;
    }

    public String getAccountId() {
        return accountId;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    /** Recurring debits already due between now and the next expected income. */
    public BigDecimal getCommittedBeforeNextIncome() {
        return committedBeforeNextIncome;
    }

    public BigDecimal getEmergencyBuffer() {
        return emergencyBuffer;
    }

    /** Balance less upcoming commitments less the buffer; never below zero. */
    public BigDecimal getDiscretionaryHeadroom() {
        return discretionaryHeadroom;
    }

    /** Null when no recurring income was detected. */
    public LocalDate getNextIncomeDate() {
        return nextIncomeDate;
    }

    public MoneyCoachRiskLevel getRiskLevel() {
        return riskLevel;
    }

    /** Start of the next projected cashflow risk window, or null when there is none. */
    public LocalDate getNextRiskDate() {
        return nextRiskDate;
    }

    /** True when the projection behind this snapshot fell back on incomplete data. */
    public boolean isFallbackUsed() {
        return fallbackUsed;
    }
}
