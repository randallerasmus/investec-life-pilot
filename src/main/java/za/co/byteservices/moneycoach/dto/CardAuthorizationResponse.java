package za.co.byteservices.moneycoach.dto;

import za.co.byteservices.moneycoach.model.CardAssessment;
import za.co.byteservices.moneycoach.model.CardDecision;
import za.co.byteservices.moneycoach.model.MoneyCoachRiskLevel;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CardAuthorizationResponse {

    private final CardDecision decision;
    private final CardAssessment assessment;
    private final boolean monitorOnly;
    private final BigDecimal amount;
    private final BigDecimal discretionaryHeadroom;
    private final BigDecimal headroomAfterTransaction;
    private final LocalDate nextIncomeDate;
    private final LocalDate nextRiskDate;
    private final MoneyCoachRiskLevel riskLevel;
    private final long snapshotAgeSeconds;
    private final boolean fallbackUsed;
    private final String reason;

    public CardAuthorizationResponse(CardDecision decision,
                                     CardAssessment assessment,
                                     boolean monitorOnly,
                                     BigDecimal amount,
                                     BigDecimal discretionaryHeadroom,
                                     BigDecimal headroomAfterTransaction,
                                     LocalDate nextIncomeDate,
                                     LocalDate nextRiskDate,
                                     MoneyCoachRiskLevel riskLevel,
                                     long snapshotAgeSeconds,
                                     boolean fallbackUsed,
                                     String reason) {
        this.decision = decision;
        this.assessment = assessment;
        this.monitorOnly = monitorOnly;
        this.amount = amount;
        this.discretionaryHeadroom = discretionaryHeadroom;
        this.headroomAfterTransaction = headroomAfterTransaction;
        this.nextIncomeDate = nextIncomeDate;
        this.nextRiskDate = nextRiskDate;
        this.riskLevel = riskLevel;
        this.snapshotAgeSeconds = snapshotAgeSeconds;
        this.fallbackUsed = fallbackUsed;
        this.reason = reason;
    }

    /** What the card should do. Only an explicit DECLINE stops the transaction. */
    public CardDecision getDecision() {
        return decision;
    }

    /** What the forecast concluded, whether or not the decision acted on it. */
    public CardAssessment getAssessment() {
        return assessment;
    }

    /** True when the decision was forced to APPROVE because monitor mode is on. */
    public boolean isMonitorOnly() {
        return monitorOnly;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getDiscretionaryHeadroom() {
        return discretionaryHeadroom;
    }

    /** Negative when the amount reaches into committed money. */
    public BigDecimal getHeadroomAfterTransaction() {
        return headroomAfterTransaction;
    }

    public LocalDate getNextIncomeDate() {
        return nextIncomeDate;
    }

    public LocalDate getNextRiskDate() {
        return nextRiskDate;
    }

    public MoneyCoachRiskLevel getRiskLevel() {
        return riskLevel;
    }

    /** How stale the snapshot behind this decision was, in seconds. */
    public long getSnapshotAgeSeconds() {
        return snapshotAgeSeconds;
    }

    public boolean isFallbackUsed() {
        return fallbackUsed;
    }

    public String getReason() {
        return reason;
    }
}
