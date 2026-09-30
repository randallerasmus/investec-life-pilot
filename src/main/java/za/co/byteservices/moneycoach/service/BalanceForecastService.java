package za.co.byteservices.moneycoach.service;

import org.springframework.stereotype.Service;
import za.co.byteservices.moneycoach.dto.BalanceForecastRequest;
import za.co.byteservices.moneycoach.dto.BalanceForecastResponse;
import za.co.byteservices.moneycoach.dto.CashflowRisk;
import za.co.byteservices.moneycoach.dto.ForecastComparison;
import za.co.byteservices.moneycoach.dto.ForecastDay;
import za.co.byteservices.moneycoach.dto.InvestecBalanceResponse;
import za.co.byteservices.moneycoach.dto.InvestecTransactionResponse;
import za.co.byteservices.moneycoach.dto.PlannedCashflow;
import za.co.byteservices.moneycoach.dto.RecurringPayment;
import za.co.byteservices.moneycoach.model.MoneyCoachRiskLevel;
import za.co.byteservices.moneycoach.model.RecurringCadence;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Projects an account balance forward day by day.
 *
 * <p>The projection places each detected recurring debit and credit on its own
 * future due date, then applies an average daily figure for everything left over
 * as discretionary spending. Walking that curve produces the dated answer a
 * balance history cannot give: when the money runs short, and by how much.
 *
 * <p>The timeline covers {@code asOf} through {@code asOf + horizonDays}
 * inclusive. Day zero is the anchor and carries any recurring item already due
 * today, but no discretionary spend, since today's variable spending is already
 * reflected in the balance the bank reported.
 */
@Service
public class BalanceForecastService {

    private static final int DEFAULT_HORIZON_DAYS = 90;

    /** Six months of history so a monthly cadence has several repetitions to prove itself. */
    private static final int HISTORY_DAYS = 180;

    private static final int MAX_HORIZON_DAYS = 365;

    private static final BigDecimal DAYS_PER_MONTH = new BigDecimal("30");

    private static final String DEFAULT_CURRENCY = "ZAR";

    private final InvestecAccountService investecAccountService;
    private final RecurringPaymentDetector recurringPaymentDetector;

    public BalanceForecastService(InvestecAccountService investecAccountService,
                                  RecurringPaymentDetector recurringPaymentDetector) {
        this.investecAccountService = investecAccountService;
        this.recurringPaymentDetector = recurringPaymentDetector;
    }

    public BalanceForecastResponse forecast(String accountId, BalanceForecastRequest request) {
        return forecast(accountId, request, LocalDate.now());
    }

    public BalanceForecastResponse forecast(String accountId, BalanceForecastRequest request, LocalDate asOf) {
        LocalDate today = asOf != null ? asOf : LocalDate.now();
        return project(accountId, request, today, load(accountId, today), List.of());
    }

    /**
     * Projects the account as it stands and again with {@code plan} added, from a
     * single read of its balance and history. Reading once keeps the two curves
     * comparable and halves the calls to Investec.
     */
    public ForecastComparison compare(String accountId,
                                      BalanceForecastRequest request,
                                      LocalDate asOf,
                                      List<PlannedCashflow> plan) {
        LocalDate today = asOf != null ? asOf : LocalDate.now();
        AccountData data = load(accountId, today);
        return new ForecastComparison(
                project(accountId, request, today, data, List.of()),
                project(accountId, request, today, data, plan != null ? plan : List.of()),
                data.currency
        );
    }

    private AccountData load(String accountId, LocalDate today) {
        BigDecimal openingBalance = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        String currency = DEFAULT_CURRENCY;
        boolean fallbackUsed = false;

        try {
            InvestecBalanceResponse balanceResponse = investecAccountService.getBalance(accountId);
            if (balanceResponse != null && balanceResponse.getData() != null) {
                openingBalance = money(balanceResponse.getData().getAvailableBalance());
                if (balanceResponse.getData().getCurrency() != null) {
                    currency = balanceResponse.getData().getCurrency();
                }
            }
        } catch (RuntimeException ex) {
            fallbackUsed = true;
        }

        List<InvestecTransactionResponse.Transaction> history = List.of();
        try {
            InvestecTransactionResponse transactionResponse = investecAccountService.getTransactions(
                    accountId,
                    today.minusDays(HISTORY_DAYS),
                    today
            );
            if (transactionResponse != null
                    && transactionResponse.getData() != null
                    && transactionResponse.getData().getTransactions() != null) {
                history = transactionResponse.getData().getTransactions();
            }
        } catch (RuntimeException ex) {
            fallbackUsed = true;
        }

        return new AccountData(openingBalance, currency, history, fallbackUsed);
    }

    private BalanceForecastResponse project(String accountId,
                                            BalanceForecastRequest request,
                                            LocalDate today,
                                            AccountData data,
                                            List<PlannedCashflow> plan) {
        int horizonDays = horizonDays(request);
        BigDecimal threshold = money(request != null ? request.getMinimumBalanceThreshold() : null);
        boolean includeDiscretionary = request == null
                || request.getIncludeDiscretionarySpend() == null
                || request.getIncludeDiscretionarySpend();

        BigDecimal openingBalance = data.openingBalance;
        boolean fallbackUsed = data.fallbackUsed;
        List<InvestecTransactionResponse.Transaction> history = data.history;

        List<RecurringPayment> recurringExpenses = recurringPaymentDetector.detect(history, today);
        List<RecurringPayment> recurringIncome = recurringPaymentDetector.detectIncome(history, today);

        BigDecimal discretionaryDaily = includeDiscretionary
                ? averageDailyDiscretionarySpend(history, recurringExpenses, today)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        LocalDate horizonEnd = today.plusDays(horizonDays);
        Map<LocalDate, List<ScheduledEvent>> calendar = new LinkedHashMap<>();
        for (RecurringPayment expense : recurringExpenses) {
            schedule(calendar, expense, today, horizonEnd, false);
        }

        List<String> assumptions = new ArrayList<>();
        BigDecimal overrideIncome = money(request != null ? request.getExpectedMonthlyIncome() : null);
        if (overrideIncome.signum() > 0) {
            // An explicit figure beats an inferred one, so the override replaces
            // detected income rather than adding to it.
            LocalDate firstIncomeDate = request.getNextIncomeDate() != null
                    ? request.getNextIncomeDate()
                    : today.plusMonths(1).withDayOfMonth(Math.min(25, today.plusMonths(1).lengthOfMonth()));
            scheduleFixedMonthly(calendar, "Expected monthly income", overrideIncome, firstIncomeDate, today, horizonEnd);
            assumptions.add(String.format(Locale.US,
                    "Monthly income of %.2f was supplied and used instead of detected income.", overrideIncome));
        } else {
            for (RecurringPayment income : recurringIncome) {
                schedule(calendar, income, today, horizonEnd, true);
            }
        }

        for (PlannedCashflow planned : plan) {
            schedulePlanned(calendar, planned, today, horizonEnd);
            assumptions.add(plannedAssumption(planned));
        }

        List<ForecastDay> timeline = buildTimeline(
                today, horizonEnd, openingBalance, calendar, discretionaryDaily, includeDiscretionary);

        ForecastDay lowestDay = timeline.stream()
                .min(Comparator.comparing(ForecastDay::getClosingBalance)
                        .thenComparing(ForecastDay::getDate))
                .orElseThrow();
        ForecastDay lastDay = timeline.get(timeline.size() - 1);

        List<CashflowRisk> risks = findRisks(timeline, threshold);
        MoneyCoachRiskLevel riskLevel = overallRiskLevel(risks);

        BigDecimal monthlyIncome = overrideIncome.signum() > 0
                ? overrideIncome
                : monthlyEquivalent(recurringIncome);
        BigDecimal monthlyRecurringExpenses = monthlyEquivalent(recurringExpenses);

        assumptions.addAll(baseAssumptions(
                recurringExpenses, recurringIncome, discretionaryDaily, includeDiscretionary, horizonDays, fallbackUsed));

        String summary = summary(today, lowestDay, lastDay, risks, threshold);

        return new BalanceForecastResponse(
                accountId,
                today,
                horizonDays,
                openingBalance,
                lastDay.getClosingBalance(),
                lowestDay.getClosingBalance(),
                lowestDay.getDate(),
                riskLevel,
                monthlyIncome,
                monthlyRecurringExpenses,
                discretionaryDaily,
                recurringExpenses,
                recurringIncome,
                risks,
                timeline,
                List.copyOf(assumptions),
                summary,
                fallbackUsed
        );
    }

    /**
     * Places a planned cost on its own dates. Each occurrence is counted from the
     * first date rather than from the one before it, so a plan starting on the
     * 31st does not drift to the 28th after February and stay there.
     */
    private void schedulePlanned(Map<LocalDate, List<ScheduledEvent>> calendar,
                                 PlannedCashflow planned,
                                 LocalDate today,
                                 LocalDate horizonEnd) {
        BigDecimal amount = money(planned.getAmount());
        if (amount.signum() <= 0 || planned.getFirstDate() == null) {
            return;
        }
        for (int i = 0; i < planned.getOccurrences(); i++) {
            LocalDate date = planned.getFirstDate().plusMonths(i);
            if (date.isAfter(horizonEnd)) {
                break;
            }
            if (!date.isBefore(today)) {
                calendar.computeIfAbsent(date, unused -> new ArrayList<>())
                        .add(new ScheduledEvent(planned.getLabel(), amount, false));
            }
        }
    }

    private String plannedAssumption(PlannedCashflow planned) {
        if (planned.getOccurrences() == 1) {
            return String.format(Locale.US, "Planned: %s of %.2f once, on %s.",
                    planned.getLabel(), money(planned.getAmount()), planned.getFirstDate());
        }
        return String.format(Locale.US, "Planned: %s of %.2f monthly from %s for %d month(s).",
                planned.getLabel(), money(planned.getAmount()), planned.getFirstDate(), planned.getOccurrences());
    }

    private List<ForecastDay> buildTimeline(LocalDate today,
                                            LocalDate horizonEnd,
                                            BigDecimal openingBalance,
                                            Map<LocalDate, List<ScheduledEvent>> calendar,
                                            BigDecimal discretionaryDaily,
                                            boolean includeDiscretionary) {
        List<ForecastDay> timeline = new ArrayList<>();
        BigDecimal running = openingBalance;

        for (LocalDate date = today; !date.isAfter(horizonEnd); date = date.plusDays(1)) {
            BigDecimal inflows = BigDecimal.ZERO;
            BigDecimal outflows = BigDecimal.ZERO;
            List<String> events = new ArrayList<>();

            for (ScheduledEvent event : calendar.getOrDefault(date, List.of())) {
                if (event.inflow) {
                    inflows = inflows.add(event.amount);
                } else {
                    outflows = outflows.add(event.amount);
                }
                events.add(event.label);
            }

            // Day zero is only an anchor: today's discretionary spending is already
            // inside the balance the bank reported, so charging it again would
            // double count.
            if (includeDiscretionary && !date.isEqual(today) && discretionaryDaily.signum() > 0) {
                outflows = outflows.add(discretionaryDaily);
            }

            BigDecimal opening = running;
            BigDecimal closing = money(opening.add(inflows).subtract(outflows));
            running = closing;

            timeline.add(new ForecastDay(date, opening, money(inflows), money(outflows), closing, List.copyOf(events)));
        }

        return timeline;
    }

    private List<CashflowRisk> findRisks(List<ForecastDay> timeline, BigDecimal threshold) {
        List<CashflowRisk> risks = new ArrayList<>();
        int index = 0;

        while (index < timeline.size()) {
            if (timeline.get(index).getClosingBalance().compareTo(threshold) >= 0) {
                index++;
                continue;
            }

            // Group the consecutive at-risk days into a single window so a month-end
            // squeeze reads as one warning with a date range.
            int start = index;
            ForecastDay lowest = timeline.get(index);
            while (index < timeline.size() && timeline.get(index).getClosingBalance().compareTo(threshold) < 0) {
                if (timeline.get(index).getClosingBalance().compareTo(lowest.getClosingBalance()) < 0) {
                    lowest = timeline.get(index);
                }
                index++;
            }

            ForecastDay first = timeline.get(start);
            ForecastDay last = timeline.get(index - 1);
            boolean overdrawn = lowest.getClosingBalance().signum() < 0;
            int daysAffected = index - start;

            String message = overdrawn
                    ? String.format(Locale.US,
                    "Projected balance goes below zero on %s and stays negative for %d day(s), reaching %.2f on %s.",
                    first.getDate(), daysAffected, lowest.getClosingBalance(), lowest.getDate())
                    : String.format(Locale.US,
                    "Projected balance drops below your %.2f threshold on %s for %d day(s), reaching %.2f on %s.",
                    threshold, first.getDate(), daysAffected, lowest.getClosingBalance(), lowest.getDate());

            risks.add(new CashflowRisk(
                    first.getDate(),
                    last.getDate(),
                    daysAffected,
                    lowest.getClosingBalance(),
                    lowest.getDate(),
                    overdrawn ? MoneyCoachRiskLevel.CRITICAL : MoneyCoachRiskLevel.TIGHT,
                    message
            ));
        }

        return risks;
    }

    private MoneyCoachRiskLevel overallRiskLevel(List<CashflowRisk> risks) {
        if (risks.isEmpty()) {
            return MoneyCoachRiskLevel.HEALTHY;
        }
        boolean critical = risks.stream()
                .anyMatch(risk -> risk.getSeverity() == MoneyCoachRiskLevel.CRITICAL);
        return critical ? MoneyCoachRiskLevel.CRITICAL : MoneyCoachRiskLevel.TIGHT;
    }

    private void schedule(Map<LocalDate, List<ScheduledEvent>> calendar,
                          RecurringPayment payment,
                          LocalDate today,
                          LocalDate horizonEnd,
                          boolean inflow) {
        RecurringCadence cadence = payment.getCadence();
        LocalDate date = payment.getNextDueDate();
        while (date != null && !date.isAfter(horizonEnd)) {
            if (!date.isBefore(today)) {
                calendar.computeIfAbsent(date, unused -> new ArrayList<>())
                        .add(new ScheduledEvent(payment.getMerchant(), payment.getExpectedAmount(), inflow));
            }
            date = cadence.next(date);
        }
    }

    private void scheduleFixedMonthly(Map<LocalDate, List<ScheduledEvent>> calendar,
                                      String label,
                                      BigDecimal amount,
                                      LocalDate firstDate,
                                      LocalDate today,
                                      LocalDate horizonEnd) {
        LocalDate date = firstDate;
        while (date.isBefore(today)) {
            date = RecurringCadence.MONTHLY.next(date);
        }
        while (!date.isAfter(horizonEnd)) {
            calendar.computeIfAbsent(date, unused -> new ArrayList<>())
                    .add(new ScheduledEvent(label, amount, true));
            date = RecurringCadence.MONTHLY.next(date);
        }
    }

    /**
     * Average daily spend that is not already accounted for by a detected recurring
     * payment. Recurring amounts are removed first so they are not charged twice:
     * once on their due date and again inside the daily average.
     */
    private BigDecimal averageDailyDiscretionarySpend(List<InvestecTransactionResponse.Transaction> history,
                                                      List<RecurringPayment> recurringExpenses,
                                                      LocalDate today) {
        BigDecimal totalDebits = BigDecimal.ZERO;
        LocalDate earliest = null;

        for (InvestecTransactionResponse.Transaction transaction : history) {
            BigDecimal amount = TransactionAmounts.debitAmount(transaction);
            if (amount.signum() == 0) {
                continue;
            }
            totalDebits = totalDebits.add(amount);

            LocalDate date = TransactionDates.parse(transaction);
            if (date != null && (earliest == null || date.isBefore(earliest))) {
                earliest = date;
            }
        }

        for (RecurringPayment expense : recurringExpenses) {
            totalDebits = totalDebits.subtract(
                    expense.getExpectedAmount().multiply(BigDecimal.valueOf(expense.getOccurrences())));
        }

        if (totalDebits.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        long observedDays = earliest != null
                ? Math.max(1, ChronoUnit.DAYS.between(earliest, today))
                : HISTORY_DAYS;

        return totalDebits.divide(BigDecimal.valueOf(observedDays), 2, RoundingMode.HALF_UP);
    }

    /** Normalises a mix of cadences to a single comparable monthly figure. */
    private BigDecimal monthlyEquivalent(List<RecurringPayment> payments) {
        BigDecimal total = BigDecimal.ZERO;
        for (RecurringPayment payment : payments) {
            total = total.add(payment.getExpectedAmount()
                    .multiply(DAYS_PER_MONTH)
                    .divide(BigDecimal.valueOf(payment.getCadence().getNominalDays()), 2, RoundingMode.HALF_UP));
        }
        return money(total);
    }

    private List<String> baseAssumptions(List<RecurringPayment> recurringExpenses,
                                         List<RecurringPayment> recurringIncome,
                                         BigDecimal discretionaryDaily,
                                         boolean includeDiscretionary,
                                         int horizonDays,
                                         boolean fallbackUsed) {
        List<String> assumptions = new ArrayList<>();
        assumptions.add(String.format(Locale.US,
                "Projected %d days ahead from %d days of transaction history.", horizonDays, HISTORY_DAYS));
        assumptions.add(String.format(Locale.US,
                "%d recurring expense(s) and %d recurring income item(s) were detected and repeat on their own cadence.",
                recurringExpenses.size(), recurringIncome.size()));

        if (includeDiscretionary) {
            assumptions.add(String.format(Locale.US,
                    "Day-to-day spending is averaged at %.2f per day, excluding detected recurring payments.",
                    discretionaryDaily));
        } else {
            assumptions.add("Day-to-day spending was excluded; only known recurring items are projected.");
        }

        if (recurringIncome.isEmpty()) {
            assumptions.add("No recurring income was detected, so the projection shows outflows only. "
                    + "Supply expectedMonthlyIncome for a fuller picture.");
        }
        if (fallbackUsed) {
            assumptions.add("Some Investec data was unavailable and local fallbacks were used.");
        }

        assumptions.add("Projections are estimates based on past behaviour, not a guarantee or financial advice.");
        return assumptions;
    }

    private String summary(LocalDate today,
                           ForecastDay lowestDay,
                           ForecastDay lastDay,
                           List<CashflowRisk> risks,
                           BigDecimal threshold) {
        if (risks.isEmpty()) {
            return String.format(Locale.US,
                    "Balance is projected to stay above %.2f for the whole period, with a low of %.2f on %s and "
                            + "a closing balance of %.2f on %s.",
                    threshold, lowestDay.getClosingBalance(), lowestDay.getDate(),
                    lastDay.getClosingBalance(), lastDay.getDate());
        }

        CashflowRisk first = risks.get(0);
        long daysAway = ChronoUnit.DAYS.between(today, first.getStartDate());
        return String.format(Locale.US,
                "%s That is %d day(s) from now. Lowest point across the period is %.2f on %s.",
                first.getMessage(), daysAway, lowestDay.getClosingBalance(), lowestDay.getDate());
    }

    private int horizonDays(BalanceForecastRequest request) {
        if (request == null || request.getHorizonDays() == null) {
            return DEFAULT_HORIZON_DAYS;
        }
        return Math.min(MAX_HORIZON_DAYS, Math.max(1, request.getHorizonDays()));
    }

    private BigDecimal money(BigDecimal value) {
        return (value != null ? value : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    /** What was read from Investec, so one read can feed more than one projection. */
    private static final class AccountData {
        private final BigDecimal openingBalance;
        private final String currency;
        private final List<InvestecTransactionResponse.Transaction> history;
        private final boolean fallbackUsed;

        private AccountData(BigDecimal openingBalance,
                            String currency,
                            List<InvestecTransactionResponse.Transaction> history,
                            boolean fallbackUsed) {
            this.openingBalance = openingBalance;
            this.currency = currency;
            this.history = history;
            this.fallbackUsed = fallbackUsed;
        }
    }

    private static final class ScheduledEvent {
        private final String label;
        private final BigDecimal amount;
        private final boolean inflow;

        private ScheduledEvent(String label, BigDecimal amount, boolean inflow) {
            this.label = label;
            this.amount = amount;
            this.inflow = inflow;
        }
    }
}
