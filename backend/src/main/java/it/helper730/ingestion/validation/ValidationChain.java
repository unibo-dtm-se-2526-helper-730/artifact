package it.helper730.ingestion.validation;

import it.helper730.domain.Transaction;
import it.helper730.ingestion.RowIssue;
import it.helper730.ingestion.Severity;
import java.util.ArrayList;
import java.util.List;

/**
 * ValidationChain.
 *
 * Runs validators in order and collects their issues. The chain stops at the
 * first error: once a row is rejected, further checks would only add noise.
 */
public final class ValidationChain {

    private final List<TransactionValidator> validators;

    public ValidationChain(List<TransactionValidator> validators) {
        this.validators = List.copyOf(validators);
    }

    /** Validates a transaction against all validators in the chain. */
    public List<RowIssue> validate(Transaction transaction) {
        List<RowIssue> issues = new ArrayList<>();
        for (TransactionValidator validator : validators) {
            List<RowIssue> found = validator.validate(transaction);
            issues.addAll(found);
            if (found.stream().anyMatch(i -> i.severity() == Severity.ERROR)) {
                break;
            }
        }
        return List.copyOf(issues);
    }
}
