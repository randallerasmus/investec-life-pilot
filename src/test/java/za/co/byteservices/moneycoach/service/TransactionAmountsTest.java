package za.co.byteservices.moneycoach.service;

import org.junit.jupiter.api.Test;
import za.co.byteservices.moneycoach.dto.InvestecTransactionResponse;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionAmountsTest {

    @Test
    void treatsPositiveAmountWithDebitTypeAsDebit() throws Exception {
        InvestecTransactionResponse.Transaction transaction = transaction("4500.00", "DEBIT");

        assertThat(TransactionAmounts.isDebit(transaction)).isTrue();
        assertThat(TransactionAmounts.debitAmount(transaction)).isEqualByComparingTo("4500.00");
    }

    @Test
    void treatsNegativeAmountAsDebitRegardlessOfType() throws Exception {
        InvestecTransactionResponse.Transaction transaction = transaction("-4500.00", null);

        assertThat(TransactionAmounts.isDebit(transaction)).isTrue();
        assertThat(TransactionAmounts.debitAmount(transaction)).isEqualByComparingTo("4500.00");
    }

    @Test
    void treatsPositiveAmountWithCreditTypeAsInflow() throws Exception {
        InvestecTransactionResponse.Transaction transaction = transaction("22000.00", "CREDIT");

        assertThat(TransactionAmounts.isDebit(transaction)).isFalse();
        assertThat(TransactionAmounts.debitAmount(transaction)).isEqualByComparingTo("0.00");
    }

    @Test
    void treatsPositiveAmountWithNoTypeAsInflow() throws Exception {
        InvestecTransactionResponse.Transaction transaction = transaction("22000.00", null);

        assertThat(TransactionAmounts.isDebit(transaction)).isFalse();
        assertThat(TransactionAmounts.debitAmount(transaction)).isEqualByComparingTo("0.00");
    }

    @Test
    void handlesNullTransactionAndNullAmount() throws Exception {
        assertThat(TransactionAmounts.isDebit(null)).isFalse();
        assertThat(TransactionAmounts.debitAmount(null)).isEqualByComparingTo("0.00");

        InvestecTransactionResponse.Transaction transaction = new InvestecTransactionResponse.Transaction();
        setField(transaction, "type", "DEBIT");
        assertThat(TransactionAmounts.isDebit(transaction)).isFalse();
        assertThat(TransactionAmounts.debitAmount(transaction)).isEqualByComparingTo("0.00");
    }

    private InvestecTransactionResponse.Transaction transaction(String amount, String type) throws Exception {
        InvestecTransactionResponse.Transaction transaction = new InvestecTransactionResponse.Transaction();
        setField(transaction, "amount", new BigDecimal(amount));
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
