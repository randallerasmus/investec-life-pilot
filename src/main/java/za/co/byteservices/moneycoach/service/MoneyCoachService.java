package za.co.byteservices.moneycoach.service;

import org.springframework.stereotype.Service;
import za.co.byteservices.moneycoach.dto.InvestecBalanceResponse;
import za.co.byteservices.moneycoach.dto.SafeToSpendResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class MoneyCoachService {

    private final InvestecAccountService investecAccountService;

    public MoneyCoachService(InvestecAccountService investecAccountService) {
        this.investecAccountService = investecAccountService;
    }

    public SafeToSpendResponse calculateSafeToSpend(String accountId,
                                                    BigDecimal estimatedBills,
                                                    BigDecimal goalSavingAmount) {

        BigDecimal resolvedEstimatedBills = estimatedBills != null
                ? estimatedBills
                : BigDecimal.ZERO;

        BigDecimal resolvedGoalSavingAmount = goalSavingAmount != null
                ? goalSavingAmount
                : BigDecimal.ZERO;

        InvestecBalanceResponse balanceResponse = investecAccountService.getBalance(accountId);

        if (balanceResponse == null || balanceResponse.getData() == null) {
            throw new IllegalStateException("Could not retrieve balance for account: " + accountId);
        }

        BigDecimal availableBalance = balanceResponse.getData().getAvailableBalance() != null
                ? balanceResponse.getData().getAvailableBalance()
                : BigDecimal.ZERO;

        BigDecimal safeToSpend = availableBalance
                .subtract(resolvedEstimatedBills)
                .subtract(resolvedGoalSavingAmount)
                .setScale(2, RoundingMode.HALF_UP);

        String currency = balanceResponse.getData().getCurrency();

        String message = buildMessage(
                currency,
                availableBalance,
                resolvedEstimatedBills,
                resolvedGoalSavingAmount,
                safeToSpend
        );

        return new SafeToSpendResponse(
                accountId,
                availableBalance.setScale(2, RoundingMode.HALF_UP),
                resolvedEstimatedBills.setScale(2, RoundingMode.HALF_UP),
                resolvedGoalSavingAmount.setScale(2, RoundingMode.HALF_UP),
                safeToSpend,
                currency,
                message
        );
    }

    private String buildMessage(BigDecimal availableBalance,
                                BigDecimal estimatedBills,
                                BigDecimal goalSavingAmount,
                                BigDecimal safeToSpend) {
        return buildMessage("ZAR", availableBalance, estimatedBills, goalSavingAmount, safeToSpend);
    }

    private String buildMessage(String currency,
                                BigDecimal availableBalance,
                                BigDecimal estimatedBills,
                                BigDecimal goalSavingAmount,
                                BigDecimal safeToSpend) {

        String resolvedCurrency = currency != null ? currency : "ZAR";

        if (safeToSpend.compareTo(BigDecimal.ZERO) < 0) {
            return String.format(
                    "Warning: Your %s %.2f available balance is not enough to cover estimated bills of %s %.2f and goal savings of %s %.2f. You are short by %s %.2f.",
                    resolvedCurrency,
                    availableBalance,
                    resolvedCurrency,
                    estimatedBills,
                    resolvedCurrency,
                    goalSavingAmount,
                    resolvedCurrency,
                    safeToSpend.abs()
            );
        }

        return String.format(
                "You have %s %.2f available, but only %s %.2f is safe to spend after estimated bills of %s %.2f and goal savings of %s %.2f.",
                resolvedCurrency,
                availableBalance,
                resolvedCurrency,
                safeToSpend,
                resolvedCurrency,
                estimatedBills,
                resolvedCurrency,
                goalSavingAmount
        );
    }
}
