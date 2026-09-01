package za.co.byteservices.moneycoach.service;

import za.co.byteservices.moneycoach.dto.InvestecTransactionResponse;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Reads the date off an Investec transaction.
 *
 * <p>The API carries several date fields and not all of them are populated on
 * every transaction, so the most specific one wins and the rest act as fallbacks
 * rather than the transaction being discarded.
 */
final class TransactionDates {

    private TransactionDates() {
    }

    /** The transaction's effective date, or {@code null} when none can be read. */
    static LocalDate parse(InvestecTransactionResponse.Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        String[] candidates = {
                transaction.getTransactionDate(),
                transaction.getValueDate(),
                transaction.getActionDate(),
                transaction.getPostingDate()
        };

        for (String candidate : candidates) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            try {
                return LocalDate.parse(candidate.trim());
            } catch (DateTimeParseException ex) {
                // Try the next date field rather than discarding the transaction.
            }
        }

        return null;
    }
}
