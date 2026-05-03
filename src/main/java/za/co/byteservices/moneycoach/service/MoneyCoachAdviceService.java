package za.co.byteservices.moneycoach.service;

import org.springframework.stereotype.Service;
import za.co.byteservices.moneycoach.dto.MoneyCoachAdviceResponse;
import za.co.byteservices.moneycoach.dto.SafeToSpendResponse;
import za.co.byteservices.moneycoach.model.MoneyCoachRiskLevel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class MoneyCoachAdviceService {

    static final String DISCLAIMER = "Educational budgeting guidance only. This is not financial advice.";

    private final MoneyCoachService moneyCoachService;
    private final Optional<AiAdviceClient> aiAdviceClient;

    public MoneyCoachAdviceService(MoneyCoachService moneyCoachService,
                                   Optional<AiAdviceClient> aiAdviceClient) {
        this.moneyCoachService = moneyCoachService;
        this.aiAdviceClient = aiAdviceClient;
    }

    public MoneyCoachAdviceResponse getAdvice(String accountId,
                                              BigDecimal bondOrRent,
                                              BigDecimal schoolFees,
                                              BigDecimal insurance,
                                              BigDecimal groceries,
                                              BigDecimal fuel,
                                              BigDecimal subscriptions,
                                              BigDecimal otherBills,
                                              BigDecimal goalSavingAmount) {
        SafeToSpendResponse safeToSpend = moneyCoachService.calculateSafeToSpend(
                accountId,
                bondOrRent,
                schoolFees,
                insurance,
                groceries,
                fuel,
                subscriptions,
                otherBills,
                goalSavingAmount
        );

        MoneyCoachAdviceResponse deterministicAdvice = buildDeterministicAdvice(safeToSpend);

        return aiAdviceClient
                .flatMap(client -> client.generateAdvice(deterministicAdvice))
                .filter(advice -> !advice.isBlank())
                .map(deterministicAdvice::withAiSummary)
                .orElse(deterministicAdvice);
    }

    private MoneyCoachAdviceResponse buildDeterministicAdvice(SafeToSpendResponse safeToSpend) {
        MoneyCoachRiskLevel riskLevel = riskLevel(
                safeToSpend.getAvailableBalance(),
                safeToSpend.getSafeToSpend()
        );

        return new MoneyCoachAdviceResponse(
                safeToSpend.getAccountId(),
                money(safeToSpend.getAvailableBalance()),
                money(safeToSpend.getEstimatedBills()),
                money(safeToSpend.getGoalSavingAmount()),
                money(safeToSpend.getSafeToSpend()),
                safeToSpend.getCurrency(),
                riskLevel,
                summary(safeToSpend, riskLevel),
                recommendations(riskLevel),
                false,
                DISCLAIMER
        );
    }

    private MoneyCoachRiskLevel riskLevel(BigDecimal availableBalance, BigDecimal safeToSpend) {
        BigDecimal resolvedSafeToSpend = valueOrZero(safeToSpend);
        BigDecimal resolvedAvailableBalance = valueOrZero(availableBalance);

        if (resolvedSafeToSpend.compareTo(BigDecimal.ZERO) < 0) {
            return MoneyCoachRiskLevel.CRITICAL;
        }

        BigDecimal tenPercentBuffer = resolvedAvailableBalance.multiply(new BigDecimal("0.10"));
        if (resolvedSafeToSpend.compareTo(BigDecimal.ZERO) == 0
                || resolvedSafeToSpend.compareTo(tenPercentBuffer) < 0) {
            return MoneyCoachRiskLevel.TIGHT;
        }

        return MoneyCoachRiskLevel.HEALTHY;
    }

    private String summary(SafeToSpendResponse safeToSpend, MoneyCoachRiskLevel riskLevel) {
        String currency = safeToSpend.getCurrency();
        BigDecimal availableBalance = money(safeToSpend.getAvailableBalance());
        BigDecimal estimatedBills = money(safeToSpend.getEstimatedBills());
        BigDecimal goalSavingAmount = money(safeToSpend.getGoalSavingAmount());
        BigDecimal safeToSpendAmount = money(safeToSpend.getSafeToSpend());

        if (riskLevel == MoneyCoachRiskLevel.CRITICAL) {
            return String.format(
                    Locale.US,
                    "You are short by %s %.2f after protecting estimated bills of %s %.2f and goal savings of %s %.2f.",
                    currency,
                    safeToSpendAmount.abs(),
                    currency,
                    estimatedBills,
                    currency,
                    goalSavingAmount
            );
        }

        if (riskLevel == MoneyCoachRiskLevel.TIGHT) {
            return String.format(
                    Locale.US,
                    "You have a small buffer of %s %.2f after estimated bills of %s %.2f and goal savings of %s %.2f.",
                    currency,
                    safeToSpendAmount,
                    currency,
                    estimatedBills,
                    currency,
                    goalSavingAmount
            );
        }

        return String.format(
                Locale.US,
                "%s %.2f is safe to spend from your %s %.2f available balance after protecting bills and savings.",
                currency,
                safeToSpendAmount,
                currency,
                availableBalance
        );
    }

    private List<String> recommendations(MoneyCoachRiskLevel riskLevel) {
        if (riskLevel == MoneyCoachRiskLevel.CRITICAL) {
            return List.of(
                    "Reduce or delay non-essential spending until your bills and savings target are covered.",
                    "Review bill estimates and adjust the savings target if the shortfall is temporary.",
                    "Avoid new discretionary commitments until your safe-to-spend amount is positive."
            );
        }

        if (riskLevel == MoneyCoachRiskLevel.TIGHT) {
            return List.of(
                    "Keep discretionary spending low until more income arrives or bills reduce.",
                    "Track small daily purchases because they can quickly consume this buffer.",
                    "Protect your savings target before increasing flexible spending."
            );
        }

        return List.of(
                "Keep your planned bills and savings protected before increasing flexible spending.",
                "Consider moving the savings amount out of your spending account to reduce temptation.",
                "Review transactions weekly to catch spending patterns early."
        );
    }

    private BigDecimal money(BigDecimal value) {
        return valueOrZero(value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
