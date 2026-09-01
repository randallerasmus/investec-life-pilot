package za.co.byteservices.moneycoach.service;

import org.junit.jupiter.api.Test;
import za.co.byteservices.moneycoach.dto.BalanceForecastRequest;
import za.co.byteservices.moneycoach.dto.BalanceForecastResponse;
import za.co.byteservices.moneycoach.dto.CashflowRisk;
import za.co.byteservices.moneycoach.dto.ForecastDay;
import za.co.byteservices.moneycoach.dto.InvestecBalanceResponse;
import za.co.byteservices.moneycoach.dto.InvestecTransactionResponse;
import za.co.byteservices.moneycoach.model.MoneyCoachRiskLevel;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BalanceForecastServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 1);
    private static final String ACCOUNT = "acc-123";

    private final InvestecAccountService investecAccountService = mock(InvestecAccountService.class);
    private final BalanceForecastService service =
            new BalanceForecastService(investecAccountService, new RecurringPaymentDetector());

    @Test
    void projectsBalanceForwardPlacingRecurringItemsOnTheirDueDates() throws Exception {
        givenBalance("20000.00");
        givenTransactions(concat(
                monthly("Bond Repayment", "-12400.00", 2, 5),
                monthly("Salary ACME", "34000.00", 25, 5)
        ));

        BalanceForecastResponse forecast = service.forecast(ACCOUNT, request(30, null, false), TODAY);

        assertThat(forecast.getAccountId()).isEqualTo(ACCOUNT);
        assertThat(forecast.getGeneratedOn()).isEqualTo(TODAY);
        assertThat(forecast.getOpeningBalance()).isEqualByComparingTo("20000.00");
        // Day zero plus 30 projected days.
        assertThat(forecast.getTimeline()).hasSize(31);
        assertThat(forecast.getTimeline().get(0).getDate()).isEqualTo(TODAY);
        assertThat(forecast.getTimeline().get(30).getDate()).isEqualTo(LocalDate.of(2026, 10, 1));

        // Bond debits on the 2nd, salary lands on the 25th.
        ForecastDay bondDay = dayOf(forecast, LocalDate.of(2026, 9, 2));
        assertThat(bondDay.getOutflows()).isEqualByComparingTo("12400.00");
        assertThat(bondDay.getEvents()).contains("Bond Repayment");
        assertThat(bondDay.getClosingBalance()).isEqualByComparingTo("7600.00");

        ForecastDay salaryDay = dayOf(forecast, LocalDate.of(2026, 9, 25));
        assertThat(salaryDay.getInflows()).isEqualByComparingTo("34000.00");
        assertThat(salaryDay.getClosingBalance()).isEqualByComparingTo("41600.00");

        assertThat(forecast.getProjectedClosingBalance()).isEqualByComparingTo("41600.00");
        assertThat(forecast.isFallbackUsed()).isFalse();
    }

    @Test
    void namesTheDateTheBalanceGoesNegative() throws Exception {
        givenBalance("5000.00");
        givenTransactions(concat(
                monthly("Bond Repayment", "-12400.00", 5, 5),
                monthly("Salary ACME", "34000.00", 25, 5)
        ));

        BalanceForecastResponse forecast = service.forecast(ACCOUNT, request(40, null, false), TODAY);

        assertThat(forecast.getRiskLevel()).isEqualTo(MoneyCoachRiskLevel.CRITICAL);
        assertThat(forecast.getRisks()).hasSize(1);

        CashflowRisk risk = forecast.getRisks().get(0);
        assertThat(risk.getSeverity()).isEqualTo(MoneyCoachRiskLevel.CRITICAL);
        // Bond of 12400 against a 5000 balance on 5 September, recovered by salary on the 25th.
        assertThat(risk.getStartDate()).isEqualTo(LocalDate.of(2026, 9, 5));
        assertThat(risk.getEndDate()).isEqualTo(LocalDate.of(2026, 9, 24));
        assertThat(risk.getDaysAffected()).isEqualTo(20);
        assertThat(risk.getLowestBalance()).isEqualByComparingTo("-7400.00");
        assertThat(risk.getMessage()).contains("2026-09-05");

        assertThat(forecast.getLowestProjectedBalance()).isEqualByComparingTo("-7400.00");
        assertThat(forecast.getLowestProjectedBalanceDate()).isEqualTo(LocalDate.of(2026, 9, 5));
        assertThat(forecast.getSummary()).contains("2026-09-05");
    }

    @Test
    void groupsConsecutiveAtRiskDaysIntoOneWindow() throws Exception {
        givenBalance("5000.00");
        givenTransactions(concat(
                monthly("Bond Repayment", "-12400.00", 5, 5),
                monthly("Salary ACME", "34000.00", 25, 5)
        ));

        BalanceForecastResponse forecast = service.forecast(ACCOUNT, request(40, null, false), TODAY);

        // Twenty consecutive negative days must read as one warning, not twenty.
        assertThat(forecast.getRisks()).hasSize(1);
        assertThat(forecast.getRisks().get(0).getDaysAffected()).isEqualTo(20);
    }

    @Test
    void reportsTightRiskWhenBalanceDipsBelowThresholdButStaysPositive() throws Exception {
        givenBalance("20000.00");
        givenTransactions(concat(
                monthly("Bond Repayment", "-12400.00", 5, 5),
                monthly("Salary ACME", "34000.00", 25, 5)
        ));

        BalanceForecastResponse forecast = service.forecast(
                ACCOUNT, request(40, new BigDecimal("10000.00"), false), TODAY);

        assertThat(forecast.getRiskLevel()).isEqualTo(MoneyCoachRiskLevel.TIGHT);
        assertThat(forecast.getRisks()).hasSize(1);
        assertThat(forecast.getRisks().get(0).getSeverity()).isEqualTo(MoneyCoachRiskLevel.TIGHT);
        assertThat(forecast.getRisks().get(0).getLowestBalance()).isEqualByComparingTo("7600.00");
        assertThat(forecast.getRisks().get(0).getMessage()).contains("threshold");
    }

    @Test
    void reportsHealthyWhenBalanceStaysAboveThreshold() throws Exception {
        givenBalance("50000.00");
        givenTransactions(concat(
                monthly("Bond Repayment", "-12400.00", 5, 5),
                monthly("Salary ACME", "34000.00", 25, 5)
        ));

        BalanceForecastResponse forecast = service.forecast(ACCOUNT, request(40, null, false), TODAY);

        assertThat(forecast.getRiskLevel()).isEqualTo(MoneyCoachRiskLevel.HEALTHY);
        assertThat(forecast.getRisks()).isEmpty();
        assertThat(forecast.getSummary()).contains("stay above");
    }

    @Test
    void detectsRecurringIncomeSoTheCurveIsNotOnlyOutflows() throws Exception {
        givenBalance("20000.00");
        givenTransactions(concat(
                monthly("Bond Repayment", "-12400.00", 2, 5),
                monthly("Salary ACME", "34000.00", 25, 5)
        ));

        BalanceForecastResponse forecast = service.forecast(ACCOUNT, request(60, null, false), TODAY);

        assertThat(forecast.getRecurringIncome()).hasSize(1);
        assertThat(forecast.getRecurringIncome().get(0).getMerchant()).isEqualTo("Salary ACME");
        assertThat(forecast.getDetectedMonthlyIncome()).isEqualByComparingTo("34000.00");
        assertThat(forecast.getRecurringExpenses()).hasSize(1);
        assertThat(forecast.getDetectedMonthlyRecurringExpenses()).isEqualByComparingTo("12400.00");
    }

    @Test
    void suppliedIncomeReplacesDetectedIncome() throws Exception {
        givenBalance("1000.00");
        givenTransactions(concat(
                monthly("Bond Repayment", "-12400.00", 2, 5),
                monthly("Salary ACME", "34000.00", 25, 5)
        ));

        BalanceForecastRequest request = new BalanceForecastRequest(
                40, null, new BigDecimal("50000.00"), LocalDate.of(2026, 9, 15), false);

        BalanceForecastResponse forecast = service.forecast(ACCOUNT, request, TODAY);

        assertThat(forecast.getDetectedMonthlyIncome()).isEqualByComparingTo("50000.00");
        // The supplied 50000 lands on the 15th; the detected 34000 salary on the 25th must not also apply.
        assertThat(dayOf(forecast, LocalDate.of(2026, 9, 15)).getInflows()).isEqualByComparingTo("50000.00");
        assertThat(dayOf(forecast, LocalDate.of(2026, 9, 25)).getInflows()).isEqualByComparingTo("0.00");
        assertThat(forecast.getAssumptions())
                .anyMatch(assumption -> assumption.contains("supplied and used instead of detected income"));
    }

    @Test
    void appliesDiscretionarySpendFromDayOneButNotDayZero() throws Exception {
        givenBalance("20000.00");
        List<InvestecTransactionResponse.Transaction> history = new ArrayList<>(
                monthly("Bond Repayment", "-12400.00", 2, 5));
        // Thirty days of once-off card spend at 100 a day, none of it recurring.
        for (int day = 0; day < 30; day++) {
            history.add(transaction("Card Purchase " + day, "-100.00", TODAY.minusDays(day + 1), null));
        }
        givenTransactions(history);

        BalanceForecastResponse forecast = service.forecast(ACCOUNT, request(10, null, true), TODAY);

        assertThat(forecast.getAverageDailyDiscretionarySpend()).isGreaterThan(BigDecimal.ZERO);

        // Day zero is an anchor: today's variable spending is already inside the
        // balance the bank reported, so charging it again would double count.
        ForecastDay dayZero = forecast.getTimeline().get(0);
        assertThat(dayZero.getOutflows()).isEqualByComparingTo("0.00");
        assertThat(dayZero.getClosingBalance()).isEqualByComparingTo("20000.00");

        // A quiet day carries the daily average alone.
        ForecastDay quietDay = dayOf(forecast, LocalDate.of(2026, 9, 4));
        assertThat(quietDay.getEvents()).isEmpty();
        assertThat(quietDay.getOutflows()).isEqualByComparingTo(forecast.getAverageDailyDiscretionarySpend());

        // A day with a debit order carries both.
        ForecastDay bondDay = dayOf(forecast, LocalDate.of(2026, 9, 2));
        assertThat(bondDay.getEvents()).contains("Bond Repayment");
        assertThat(bondDay.getOutflows())
                .isEqualByComparingTo(new BigDecimal("12400.00").add(forecast.getAverageDailyDiscretionarySpend()));
    }

    @Test
    void excludesDiscretionarySpendWhenRequested() throws Exception {
        givenBalance("20000.00");
        List<InvestecTransactionResponse.Transaction> history = new ArrayList<>();
        for (int day = 0; day < 30; day++) {
            history.add(transaction("Card Purchase " + day, "-100.00", TODAY.minusDays(day + 1), null));
        }
        givenTransactions(history);

        BalanceForecastResponse forecast = service.forecast(ACCOUNT, request(10, null, false), TODAY);

        assertThat(forecast.getAverageDailyDiscretionarySpend()).isEqualByComparingTo("0.00");
        assertThat(forecast.getProjectedClosingBalance()).isEqualByComparingTo("20000.00");
        assertThat(forecast.getAssumptions())
                .anyMatch(assumption -> assumption.contains("Day-to-day spending was excluded"));
    }

    @Test
    void doesNotDoubleCountRecurringPaymentsInsideDiscretionaryAverage() throws Exception {
        givenBalance("100000.00");
        // History is nothing but the bond debit order, so there is no discretionary spend left over.
        givenTransactions(monthly("Bond Repayment", "-12400.00", 2, 5));

        BalanceForecastResponse forecast = service.forecast(ACCOUNT, request(10, null, true), TODAY);

        assertThat(forecast.getAverageDailyDiscretionarySpend()).isEqualByComparingTo("0.00");
    }

    @Test
    void defaultsToNinetyDayHorizonAndClampsOutOfRangeValues() throws Exception {
        givenBalance("20000.00");
        givenTransactions(List.of());

        assertThat(service.forecast(ACCOUNT, null, TODAY).getHorizonDays()).isEqualTo(90);
        assertThat(service.forecast(ACCOUNT, request(0, null, false), TODAY).getHorizonDays()).isEqualTo(1);
        assertThat(service.forecast(ACCOUNT, request(9999, null, false), TODAY).getHorizonDays()).isEqualTo(365);
    }

    @Test
    void fallsBackGracefullyWhenInvestecDataIsUnavailable() {
        when(investecAccountService.getBalance(ACCOUNT)).thenThrow(new IllegalStateException("No token"));
        when(investecAccountService.getTransactions(eq(ACCOUNT), any(), any()))
                .thenThrow(new IllegalStateException("No token"));

        BalanceForecastResponse forecast = service.forecast(ACCOUNT, request(30, null, true), TODAY);

        assertThat(forecast.isFallbackUsed()).isTrue();
        assertThat(forecast.getOpeningBalance()).isEqualByComparingTo("0.00");
        assertThat(forecast.getRecurringExpenses()).isEmpty();
        assertThat(forecast.getRecurringIncome()).isEmpty();
        assertThat(forecast.getTimeline()).hasSize(31);
        assertThat(forecast.getAssumptions())
                .anyMatch(assumption -> assumption.contains("local fallbacks were used"));
    }

    @Test
    void alwaysDisclosesThatProjectionsAreEstimates() throws Exception {
        givenBalance("20000.00");
        givenTransactions(List.of());

        BalanceForecastResponse forecast = service.forecast(ACCOUNT, request(30, null, true), TODAY);

        assertThat(forecast.getAssumptions())
                .anyMatch(assumption -> assumption.contains("not a guarantee or financial advice"));
    }

    private ForecastDay dayOf(BalanceForecastResponse forecast, LocalDate date) {
        return forecast.getTimeline().stream()
                .filter(day -> day.getDate().isEqual(date))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No forecast day for " + date));
    }

    private BalanceForecastRequest request(Integer horizonDays,
                                           BigDecimal threshold,
                                           boolean includeDiscretionary) {
        return new BalanceForecastRequest(horizonDays, threshold, null, null, includeDiscretionary);
    }

    /** Builds {@code count} monthly transactions landing on {@code dayOfMonth}, most recent first. */
    private List<InvestecTransactionResponse.Transaction> monthly(String description,
                                                                  String amount,
                                                                  int dayOfMonth,
                                                                  int count) throws Exception {
        List<InvestecTransactionResponse.Transaction> transactions = new ArrayList<>();
        LocalDate date = TODAY.withDayOfMonth(dayOfMonth);
        if (date.isAfter(TODAY)) {
            date = date.minusMonths(1);
        }
        for (int i = 0; i < count; i++) {
            transactions.add(transaction(description, amount, date.minusMonths(i), null));
        }
        return transactions;
    }

    @SafeVarargs
    private List<InvestecTransactionResponse.Transaction> concat(
            List<InvestecTransactionResponse.Transaction>... lists) {
        List<InvestecTransactionResponse.Transaction> combined = new ArrayList<>();
        for (List<InvestecTransactionResponse.Transaction> list : lists) {
            combined.addAll(list);
        }
        return combined;
    }

    private void givenBalance(String availableBalance) throws Exception {
        InvestecBalanceResponse response = new InvestecBalanceResponse();
        InvestecBalanceResponse.Data data = new InvestecBalanceResponse.Data();
        setField(data, "accountId", ACCOUNT);
        setField(data, "availableBalance", new BigDecimal(availableBalance));
        setField(data, "currentBalance", new BigDecimal(availableBalance));
        setField(data, "currency", "ZAR");
        setField(response, "data", data);
        when(investecAccountService.getBalance(ACCOUNT)).thenReturn(response);
    }

    private void givenTransactions(List<InvestecTransactionResponse.Transaction> transactions) throws Exception {
        InvestecTransactionResponse response = new InvestecTransactionResponse();
        InvestecTransactionResponse.Data data = new InvestecTransactionResponse.Data();
        setField(data, "transactions", List.copyOf(transactions));
        setField(response, "data", data);
        when(investecAccountService.getTransactions(eq(ACCOUNT), any(), any())).thenReturn(response);
    }

    private InvestecTransactionResponse.Transaction transaction(String description,
                                                                String amount,
                                                                LocalDate transactionDate,
                                                                String type) throws Exception {
        InvestecTransactionResponse.Transaction transaction = new InvestecTransactionResponse.Transaction();
        setField(transaction, "accountId", ACCOUNT);
        setField(transaction, "description", description);
        setField(transaction, "amount", new BigDecimal(amount));
        setField(transaction, "transactionDate", transactionDate.toString());
        if (type != null) {
            setField(transaction, "type", type);
        }
        return transaction;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
