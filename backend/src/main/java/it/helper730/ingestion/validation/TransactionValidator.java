package it.helper730.ingestion.validation;

import it.helper730.domain.Transaction;
import it.helper730.ingestion.RowIssue;
import java.util.List;

/**
 * TransactionValidator.
 *
 * Check the imported transactions, beyond the rules the domain enforces.
 */
@FunctionalInterface
public interface TransactionValidator {

    /**
     * Validates the given transaction.
     *
     * @param transaction the transaction to validate
     * @return a list of issues found during validation
     */
    List<RowIssue> validate(Transaction transaction);
}
