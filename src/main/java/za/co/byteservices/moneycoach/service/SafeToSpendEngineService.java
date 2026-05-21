package za.co.byteservices.moneycoach.service;

import org.springframework.stereotype.Service;
import za.co.byteservices.moneycoach.dto.AdvancedSafeToSpendRequest;
import za.co.byteservices.moneycoach.dto.AdvancedSafeToSpendResponse;
import za.co.byteservices.moneycoach.dto.InvestecBalanceResponse;
import za.co.byteservices.moneycoach.dto.InvestecTransactionResponse;
import za.co.byteservices.moneycoach.model.MoneyCoachRiskLevel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class SafeToSpendEngineService {

    private final InvestecAccountService investecAccountService;

    public SafeToSpendEngineService(InvestecAccountService investecAccountService) {
        this.investecAccountService = investecAccountService;
    }

    public AdvancedSafeToSpendResponse calculateAdvancedSafeToSpend(String accountId, AdvancedSafeToSpendRequest request) {
        BigDecimal currentBalance = BigDecimal.ZERO;
        BigDecimal estimatedRecurringExpenses = BigDecimal.ZERO;
        BigDecimal emergencyBuffer = money(request != null ? request.getEmergencyBuffer() : BigDecimal.ZERO);
        BigDecimal plannedPurchaseAmount = money(request != null ? request.getPlannedPurchaseAmount() : BigDecimal.ZERO);
        LocalDate today = LocalDate.now();
        LocalDate payday = request != null && request.getPayday() != null ? request.getPayday() : today.plusDays(14);
        int daysUntilPayday = Math.max(0, (int) ChronoUnit.DAYS.between(today, payday));
        boolean fallbackUsed = false;

        try {
            InvestecBalanceResponse balanceResponse = investecAccountService.getBalance(accountId);
            if (balanceResponse != null && balanceResponse.getData() != null) {
                currentBalance = money(balanceResponse.getData().getAvailableBalance());
            }
        } catch (RuntimeException ex) {
            fallbackUsed = true;
        }

        try {
            InvestecTransactionResponse transactionResponse = investecAccountService.getTransactions(
                    accountId,
                    today.minusDays(30),
                    today
            );
            estimatedRecurringExpenses = estimateRecurringExpenses(transactionResponse, daysUntilPayday);
        } catch (RuntimeException ex) {
            fallbackUsed = true;
        }

        BigDecimal safeToSpend = money(currentBalance
                .subtract(estimatedRecurringExpenses)
                .subtract(emergencyBuffer));

        BigDecimal remainingAfterPurchase = safeToSpend.subtract(plannedPurchaseAmount);
        String affordabilityStatus = affordabilityStatus(currentBalance, remainingAfterPurchase);
        MoneyCoachRiskLevel riskLevel = riskLevel(currentBalance, remainingAfterPurchase);

        String explanation = String.format(
                Locale.US,
                "%sCurrent balance %.2f, estimated recurring expenses %.2f, emergency buffer %.2f, and %d days until payday produce a safe-to-spend amount of %.2f before the planned purchase.",
                fallbackUsed ? "Local fallback was used for missing Investec data. " : "",
                currentBalance,
                estimatedRecurringExpenses,
                emergencyBuffer,
                daysUntilPayday,
                safeToSpend
        );

        return new AdvancedSafeToSpendResponse(
                accountId,
                currentBalance,
                estimatedRecurringExpenses,
                emergencyBuffer,
                daysUntilPayday,
                plannedPurchaseAmount,
                safeToSpend,
                affordabilityStatus,
                riskLevel,
                explanation
        );
    }

    private BigDecimal estimateRecurringExpenses(InvestecTransactionResponse response, int daysUntilPayday) {
        if (response == null || response.getData() == null || response.getData().getTransactions() == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        List<InvestecTransactionResponse.Transaction> transactions = response.getData().getTransactions();
        Map<String, Integer> descriptionCounts = new HashMap<>();
        BigDecimal totalOutflow = BigDecimal.ZERO;

        for (InvestecTransactionResponse.Transaction transaction : transactions) {
            BigDecimal amount = valueOrZero(transaction.getAmount());
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                totalOutflow = totalOutflow.add(amount.abs());
                String key = normalize(transaction.getDescription());
                if (!key.isBlank()) {
                    descriptionCounts.merge(key, 1, Integer::sum);
                }
            }
        }

        BigDecimal recurringCandidates = BigDecimal.ZERO;
        for (InvestecTransactionResponse.Transaction transaction : transactions) {
            BigDecimal amount = valueOrZero(transaction.getAmount());
            if (amount.compareTo(BigDecimal.ZERO) >= 0) {
                continue;
            }
            String description = normalize(transaction.getDescription());
            boolean keywordMatch = description.contains("rent")
                    || description.contains("insurance")
                    || description.contains("subscription")
                    || description.contains("school")
                    || description.contains("loan")
                    || description.contains("debit order");
            boolean repeated = descriptionCounts.getOrDefault(description, 0) > 1;
            if (keywordMatch || repeated) {
                recurringCandidates = recurringCandidates.add(amount.abs());
            }
        }

        BigDecimal projectedVariableExpenses = BigDecimal.ZERO;
        if (daysUntilPayday > 0) {
            BigDecimal averageDailyOutflow = totalOutflow.divide(new BigDecimal("30"), 2, RoundingMode.HALF_UP);
            projectedVariableExpenses = averageDailyOutflow.multiply(BigDecimal.valueOf(daysUntilPayday));
        }

        return money(recurringCandidates.max(projectedVariableExpenses));
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.US).trim();
    }

    private String affordabilityStatus(BigDecimal currentBalance, BigDecimal remainingAfterPurchase) {
        if (remainingAfterPurchase.compareTo(BigDecimal.ZERO) < 0) {
            return "NOT_AFFORDABLE";
        }

        BigDecimal tightThreshold = currentBalance.multiply(new BigDecimal("0.05")).max(new BigDecimal("500.00"));
        if (remainingAfterPurchase.compareTo(tightThreshold) < 0) {
            return "TIGHT";
        }

        return "AFFORDABLE";
    }

    private MoneyCoachRiskLevel riskLevel(BigDecimal currentBalance, BigDecimal remainingAfterPurchase) {
        if (remainingAfterPurchase.compareTo(BigDecimal.ZERO) < 0) {
            return MoneyCoachRiskLevel.CRITICAL;
        }

        BigDecimal tenPercentBuffer = currentBalance.multiply(new BigDecimal("0.10"));
        if (remainingAfterPurchase.compareTo(BigDecimal.ZERO) == 0
                || remainingAfterPurchase.compareTo(tenPercentBuffer) < 0) {
            return MoneyCoachRiskLevel.TIGHT;
        }

        return MoneyCoachRiskLevel.HEALTHY;
    }

    private BigDecimal money(BigDecimal value) {
        return valueOrZero(value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
