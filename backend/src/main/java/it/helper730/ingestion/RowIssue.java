package it.helper730.ingestion;

import it.helper730.domain.Msg;
import java.util.Objects;

/**
 * RowIssue.
 *
 * A problem found in one row of an imported file.
 *
 * @param fileRow  line number in the file, starting from 1 (the header)
 * @param severity whether the row is rejected or only flagged
 * @param msg      translatable description
 */
public record RowIssue(int fileRow, Severity severity, Msg msg) {

    public RowIssue {
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(msg, "msg");
    }

    public static RowIssue error(int fileRow, Msg msg) {
        return new RowIssue(fileRow, Severity.ERROR, msg);
    }

    public static RowIssue warning(int fileRow, Msg msg) {
        return new RowIssue(fileRow, Severity.WARNING, msg);
    }
}
