package za.co.byteservices.moneycoach.service;

import za.co.byteservices.moneycoach.dto.InvestecTransactionResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Interprets the direction of an Investec transaction.
 *
 * <p>The Investec Account Information API reports every amount as a positive
 * number and carries the direction in {@code type}, which is {@code DEBIT} or
 * {@code CREDIT}. Some fixtures and earlier callers instead use a signed amount
 * where an outflow is negative. Both conventions are honoured here so that no
 * call site has to re-derive the rule and get it wrong.
 */
final class TransactionAmounts {

    private TransactionAmounts() {
    }

    static boolean isDebit(InvestecTransactionResponse.Transaction transaction) {
        if (transaction == null || transaction.getAmount() == null) {
            return false;
        }
        if (transaction.getAmount().signum() < 0) {
            return true;
        }
        return "DEBIT".equalsIgnoreCase(transaction.getType());
    }

    static boolean isCredit(InvestecTransactionResponse.Transaction transaction) {
        if (transaction == null || transaction.getAmount() == null) {
            return false;
        }
        return transaction.getAmount().signum() > 0 && !isDebit(transaction);
    }

    /** Positive magnitude of a debit, or zero when the transaction is not a debit. */
    static BigDecimal debitAmount(InvestecTransactionResponse.Transaction transaction) {
        if (!isDebit(transaction)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return transaction.getAmount().abs().setScale(2, RoundingMode.HALF_UP);
    }

    /** Positive magnitude of a credit, or zero when the transaction is not a credit. */
    static BigDecimal creditAmount(InvestecTransactionResponse.Transaction transaction) {
        if (!isCredit(transaction)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return transaction.getAmount().abs().setScale(2, RoundingMode.HALF_UP);
    }
}
