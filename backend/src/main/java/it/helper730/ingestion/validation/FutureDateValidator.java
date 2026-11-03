package it.helper730.ingestion.validation;

import it.helper730.domain.Msg;
import it.helper730.domain.Transaction;
import it.helper730.ingestion.RowIssue;
import java.time.Clock;
import java.util.List;
import java.util.Objects;

/**
 * FutureDateValidator.
 *
 * Rejects operations dated in the future.
 */
public final class FutureDateValidator implements TransactionValidator {

    private final Clock clock;

    public FutureDateValidator(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public List<RowIssue> validate(Transaction transaction) {
        if (transaction.executedAt().isAfter(clock.instant())) {
            return List.of(RowIssue.error(
                    transaction.fileRow(),
                    Msg.of("error.future_date", transaction.executedAt().toString())));
        }
        return List.of();
    }
}
