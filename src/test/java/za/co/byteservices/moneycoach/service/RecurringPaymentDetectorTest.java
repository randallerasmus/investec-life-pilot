package za.co.byteservices.moneycoach.service;

import org.junit.jupiter.api.Test;
import za.co.byteservices.moneycoach.dto.InvestecTransactionResponse;
import za.co.byteservices.moneycoach.dto.RecurringPayment;
import za.co.byteservices.moneycoach.model.RecurringCadence;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecurringPaymentDetectorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 1);

    private final RecurringPaymentDetector detector = new RecurringPaymentDetector();

    @Test
    void detectsMonthlyDebitOrderAndProjectsNextDueDate() throws Exception {
        List<RecurringPayment> payments = detector.detect(List.of(
                debit("Netflix.com", "-199.00", LocalDate.of(2026, 5, 3)),
                debit("Netflix.com", "-199.00", LocalDate.of(2026, 6, 3)),
                debit("Netflix.com", "-199.00", LocalDate.of(2026, 7, 3)),
                debit("Netflix.com", "-199.00", LocalDate.of(2026, 8, 3))
        ), TODAY);

        assertThat(payments).hasSize(1);
        RecurringPayment netflix = payments.get(0);
        assertThat(netflix.getMerchant()).isEqualTo("Netflix.com");
        assertThat(netflix.getExpectedAmount()).isEqualByComparingTo("199.00");
        assertThat(netflix.getCadence()).isEqualTo(RecurringCadence.MONTHLY);
        assertThat(netflix.getOccurrences()).isEqualTo(4);
        assertThat(netflix.getFirstSeen()).isEqualTo(LocalDate.of(2026, 5, 3));
        assertThat(netflix.getLastSeen()).isEqualTo(LocalDate.of(2026, 8, 3));
        assertThat(netflix.getNextDueDate()).isEqualTo(LocalDate.of(2026, 9, 3));
        assertThat(netflix.getAmountDrift()).isEqualByComparingTo("0.00");
        assertThat(netflix.getConfidence()).isGreaterThanOrEqualTo(new BigDecimal("0.80"));
    }

    @Test
    void detectsWeeklyPayment() throws Exception {
        List<RecurringPayment> payments = detector.detect(List.of(
                debit("Weekly Cleaner", "-450.00", LocalDate.of(2026, 8, 4)),
                debit("Weekly Cleaner", "-450.00", LocalDate.of(2026, 8, 11)),
                debit("Weekly Cleaner", "-450.00", LocalDate.of(2026, 8, 18)),
                debit("Weekly Cleaner", "-450.00", LocalDate.of(2026, 8, 25))
        ), TODAY);

        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getCadence()).isEqualTo(RecurringCadence.WEEKLY);
        assertThat(payments.get(0).getAverageDaysBetween()).isEqualTo(7);
        assertThat(payments.get(0).getNextDueDate()).isEqualTo(LocalDate.of(2026, 9, 1));
    }

    @Test
    void detectsAnnualPayment() throws Exception {
        List<RecurringPayment> payments = detector.detect(List.of(
                debit("Car Licence Renewal", "-780.00", LocalDate.of(2024, 8, 20)),
                debit("Car Licence Renewal", "-780.00", LocalDate.of(2025, 8, 22)),
                debit("Car Licence Renewal", "-800.00", LocalDate.of(2026, 8, 21))
        ), TODAY);

        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getCadence()).isEqualTo(RecurringCadence.ANNUAL);
        assertThat(payments.get(0).getNextDueDate()).isEqualTo(LocalDate.of(2027, 8, 21));
    }

    @Test
    void groupsDescriptionsThatDifferOnlyByReferenceNumbers() throws Exception {
        List<RecurringPayment> payments = detector.detect(List.of(
                debit("VITALITY PREMIUM 449213", "-349.00", LocalDate.of(2026, 6, 15)),
                debit("VITALITY PREMIUM 883410", "-349.00", LocalDate.of(2026, 7, 15)),
                debit("Vitality Premium 991002", "-349.00", LocalDate.of(2026, 8, 15))
        ), TODAY);

        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getOccurrences()).isEqualTo(3);
        assertThat(payments.get(0).getCadence()).isEqualTo(RecurringCadence.MONTHLY);
    }

    @Test
    void reportsAmountDriftWhenSubscriptionPriceRises() throws Exception {
        List<RecurringPayment> payments = detector.detect(List.of(
                debit("Spotify", "-199.00", LocalDate.of(2026, 5, 10)),
                debit("Spotify", "-199.00", LocalDate.of(2026, 6, 10)),
                debit("Spotify", "-229.00", LocalDate.of(2026, 7, 10)),
                debit("Spotify", "-229.00", LocalDate.of(2026, 8, 10))
        ), TODAY);

        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getAmountDrift()).isEqualByComparingTo("30.00");
        assertThat(payments.get(0).getCadence()).isEqualTo(RecurringCadence.MONTHLY);
    }

    @Test
    void ignoresVariableSpendAtTheSameMerchant() throws Exception {
        List<RecurringPayment> payments = detector.detect(List.of(
                debit("Woolworths Food", "-243.10", LocalDate.of(2026, 8, 2)),
                debit("Woolworths Food", "-1187.45", LocalDate.of(2026, 8, 9)),
                debit("Woolworths Food", "-88.00", LocalDate.of(2026, 8, 17)),
                debit("Woolworths Food", "-902.30", LocalDate.of(2026, 8, 24))
        ), TODAY);

        assertThat(payments).isEmpty();
    }

    @Test
    void ignoresIrregularlySpacedPayments() throws Exception {
        List<RecurringPayment> payments = detector.detect(List.of(
                debit("Hardware Store", "-500.00", LocalDate.of(2026, 6, 1)),
                debit("Hardware Store", "-500.00", LocalDate.of(2026, 6, 3)),
                debit("Hardware Store", "-500.00", LocalDate.of(2026, 8, 20))
        ), TODAY);

        assertThat(payments).isEmpty();
    }

    @Test
    void ignoresCreditsSoSalaryIsNotTreatedAsRecurringPayment() throws Exception {
        List<RecurringPayment> payments = detector.detect(List.of(
                credit("Salary ACME Ltd", "42000.00", LocalDate.of(2026, 6, 25)),
                credit("Salary ACME Ltd", "42000.00", LocalDate.of(2026, 7, 25)),
                credit("Salary ACME Ltd", "42000.00", LocalDate.of(2026, 8, 25))
        ), TODAY);

        assertThat(payments).isEmpty();
    }

    @Test
    void treatsPositiveAmountsWithDebitTypeAsOutflows() throws Exception {
        List<RecurringPayment> payments = detector.detect(List.of(
                investecDebit("Discovery Insure", "1250.00", LocalDate.of(2026, 6, 5)),
                investecDebit("Discovery Insure", "1250.00", LocalDate.of(2026, 7, 5)),
                investecDebit("Discovery Insure", "1250.00", LocalDate.of(2026, 8, 5))
        ), TODAY);

        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getExpectedAmount()).isEqualByComparingTo("1250.00");
        assertThat(payments.get(0).getCadence()).isEqualTo(RecurringCadence.MONTHLY);
    }

    @Test
    void ignoresSingleOccurrence() throws Exception {
        List<RecurringPayment> payments = detector.detect(List.of(
                debit("Once Off Transfer", "-2500.00", LocalDate.of(2026, 8, 12))
        ), TODAY);

        assertThat(payments).isEmpty();
    }

    @Test
    void ignoresCancelledSubscriptionThatStoppedMonthsAgo() throws Exception {
        List<RecurringPayment> payments = detector.detect(List.of(
                debit("Gym Membership", "-599.00", LocalDate.of(2026, 1, 7)),
                debit("Gym Membership", "-599.00", LocalDate.of(2026, 2, 7)),
                debit("Gym Membership", "-599.00", LocalDate.of(2026, 3, 7))
        ), TODAY);

        assertThat(payments).isEmpty();
    }

    @Test
    void ordersPaymentsByNextDueDate() throws Exception {
        List<RecurringPayment> payments = detector.detect(List.of(
                debit("Netflix.com", "-199.00", LocalDate.of(2026, 6, 20)),
                debit("Netflix.com", "-199.00", LocalDate.of(2026, 7, 20)),
                debit("Netflix.com", "-199.00", LocalDate.of(2026, 8, 20)),
                debit("Bond Repayment", "-12400.00", LocalDate.of(2026, 6, 2)),
                debit("Bond Repayment", "-12400.00", LocalDate.of(2026, 7, 2)),
                debit("Bond Repayment", "-12400.00", LocalDate.of(2026, 8, 2))
        ), TODAY);

        assertThat(payments).hasSize(2);
        assertThat(payments).extracting(RecurringPayment::getMerchant)
                .containsExactly("Bond Repayment", "Netflix.com");
        assertThat(payments.get(0).getNextDueDate()).isEqualTo(LocalDate.of(2026, 9, 2));
        assertThat(payments.get(1).getNextDueDate()).isEqualTo(LocalDate.of(2026, 9, 20));
    }

    @Test
    void fallsBackToValueDateWhenTransactionDateIsMissing() throws Exception {
        InvestecTransactionResponse.Transaction first = debit("Insurance Premium", "-810.00", null);
        setField(first, "valueDate", "2026-07-11");
        InvestecTransactionResponse.Transaction second = debit("Insurance Premium", "-810.00", null);
        setField(second, "valueDate", "2026-08-11");

        List<RecurringPayment> payments = detector.detect(List.of(first, second), TODAY);

        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getLastSeen()).isEqualTo(LocalDate.of(2026, 8, 11));
    }

    @Test
    void returnsEmptyListForMissingOrEmptyData() throws Exception {
        assertThat(detector.detect((InvestecTransactionResponse) null, TODAY)).isEmpty();
        assertThat(detector.detect(new InvestecTransactionResponse(), TODAY)).isEmpty();
        assertThat(detector.detect(transactions(), TODAY)).isEmpty();
        assertThat(detector.detect((List<InvestecTransactionResponse.Transaction>) null, TODAY)).isEmpty();
    }

    @Test
    void readsTransactionsFromInvestecResponseWrapper() throws Exception {
        InvestecTransactionResponse response = transactions(
                debit("Old Mutual Premium", "-1100.00", LocalDate.of(2026, 6, 28)),
                debit("Old Mutual Premium", "-1100.00", LocalDate.of(2026, 7, 28)),
                debit("Old Mutual Premium", "-1100.00", LocalDate.of(2026, 8, 28))
        );

        List<RecurringPayment> payments = detector.detect(response, TODAY);

        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getNextDueDate()).isEqualTo(LocalDate.of(2026, 9, 28));
    }

    private InvestecTransactionResponse transactions(InvestecTransactionResponse.Transaction... transactions) throws Exception {
        InvestecTransactionResponse response = new InvestecTransactionResponse();
        InvestecTransactionResponse.Data data = new InvestecTransactionResponse.Data();
        setField(data, "transactions", List.of(transactions));
        setField(response, "data", data);
        return response;
    }

    private InvestecTransactionResponse.Transaction debit(String description,
                                                          String amount,
                                                          LocalDate transactionDate) throws Exception {
        return transaction(description, amount, transactionDate, null);
    }

    private InvestecTransactionResponse.Transaction credit(String description,
                                                           String amount,
                                                           LocalDate transactionDate) throws Exception {
        return transaction(description, amount, transactionDate, "CREDIT");
    }

    private InvestecTransactionResponse.Transaction investecDebit(String description,
                                                                  String amount,
                                                                  LocalDate transactionDate) throws Exception {
        return transaction(description, amount, transactionDate, "DEBIT");
    }

    private InvestecTransactionResponse.Transaction transaction(String description,
                                                                String amount,
                                                                LocalDate transactionDate,
                                                                String type) throws Exception {
        InvestecTransactionResponse.Transaction transaction = new InvestecTransactionResponse.Transaction();
        setField(transaction, "accountId", "acc-123");
        setField(transaction, "description", description);
        setField(transaction, "amount", new BigDecimal(amount));
        if (transactionDate != null) {
            setField(transaction, "transactionDate", transactionDate.toString());
        }
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
