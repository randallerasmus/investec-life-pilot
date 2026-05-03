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
                                                    BigDecimal bondOrRent,
                                                    BigDecimal schoolFees,
                                                    BigDecimal insurance,
                                                    BigDecimal groceries,
                                                    BigDecimal fuel,
                                                    BigDecimal subscriptions,
                                                    BigDecimal otherBills,
                                                    BigDecimal goalSavingAmount) {

        BigDecimal resolvedBondOrRent = valueOrZero(bondOrRent);
        BigDecimal resolvedSchoolFees = valueOrZero(schoolFees);
        BigDecimal resolvedInsurance = valueOrZero(insurance);
        BigDecimal resolvedGroceries = valueOrZero(groceries);
        BigDecimal resolvedFuel = valueOrZero(fuel);
        BigDecimal resolvedSubscriptions = valueOrZero(subscriptions);
        BigDecimal resolvedOtherBills = valueOrZero(otherBills);
        BigDecimal resolvedGoalSavingAmount = valueOrZero(goalSavingAmount);

        BigDecimal estimatedBills = resolvedBondOrRent
                .add(resolvedSchoolFees)
                .add(resolvedInsurance)
                .add(resolvedGroceries)
                .add(resolvedFuel)
                .add(resolvedSubscriptions)
                .add(resolvedOtherBills);

        InvestecBalanceResponse balanceResponse = investecAccountService.getBalance(accountId);

        if (balanceResponse == null || balanceResponse.getData() == null) {
            throw new IllegalStateException("Could not retrieve balance for account: " + accountId);
        }

        BigDecimal availableBalance = valueOrZero(balanceResponse.getData().getAvailableBalance());

        BigDecimal safeToSpend = availableBalance
                .subtract(estimatedBills)
                .subtract(resolvedGoalSavingAmount)
                .setScale(2, RoundingMode.HALF_UP);

        String currency = balanceResponse.getData().getCurrency() != null
                ? balanceResponse.getData().getCurrency()
                : "ZAR";

        String message = buildMessage(
                currency,
                availableBalance,
                estimatedBills,
                resolvedGoalSavingAmount,
                safeToSpend
        );

        return new SafeToSpendResponse(
                accountId,
                money(availableBalance),
                money(resolvedBondOrRent),
                money(resolvedSchoolFees),
                money(resolvedInsurance),
                money(resolvedGroceries),
                money(resolvedFuel),
                money(resolvedSubscriptions),
                money(resolvedOtherBills),
                money(estimatedBills),
                money(resolvedGoalSavingAmount),
                money(safeToSpend),
                currency,
                message
        );
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal money(BigDecimal value) {
        return valueOrZero(value).setScale(2, RoundingMode.HALF_UP);
    }

    private String buildMessage(String currency,
                                BigDecimal availableBalance,
                                BigDecimal estimatedBills,
                                BigDecimal goalSavingAmount,
                                BigDecimal safeToSpend) {

        if (safeToSpend.compareTo(BigDecimal.ZERO) < 0) {
            return String.format(
                    "Warning: Your %s %.2f available balance is not enough to cover estimated bills of %s %.2f and goal savings of %s %.2f. You are short by %s %.2f.",
                    currency,
                    availableBalance,
                    currency,
                    estimatedBills,
                    currency,
                    goalSavingAmount,
                    currency,
                    safeToSpend.abs()
            );
        }

        return String.format(
                "You have %s %.2f available, but only %s %.2f is safe to spend after estimated bills of %s %.2f and goal savings of %s %.2f.",
                currency,
                availableBalance,
                currency,
                safeToSpend,
                currency,
                estimatedBills,
                currency,
                goalSavingAmount
        );
    }
}
