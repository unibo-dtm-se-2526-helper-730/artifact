package it.helper730.ingestion;

import it.helper730.domain.Transaction;
import java.util.List;
import java.util.Objects;

/**
 * ParseResult.
 *
 * Outcome of parsing a broker export.
 *
 * @param broker       name of the broker that produced the file
 * @param transactions rows read successfully, in file order
 * @param issues       errors (rows not imported) and warnings, in file order
 */
public record ParseResult(String broker, List<Transaction> transactions, List<RowIssue> issues) {

    public ParseResult {
        Objects.requireNonNull(broker, "broker");
        transactions = List.copyOf(transactions);
        issues = List.copyOf(issues);
    }

    /** Rows that could not be imported. */
    public List<RowIssue> errors() {
        return issues.stream().filter(i -> i.severity() == Severity.ERROR).toList();
    }

    /** Rows imported with something to check. */
    public List<RowIssue> warnings() {
        return issues.stream().filter(i -> i.severity() == Severity.WARNING).toList();
    }
}
