package it.helper730.ingestion.validation;

import static org.assertj.core.api.Assertions.assertThat;

import it.helper730.domain.OperationType;
import it.helper730.domain.Transaction;
import it.helper730.ingestion.RowIssue;
import it.helper730.ingestion.Severity;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class ValidatorsTest {

    private final ImpliedFeeValidator feeValidator = new ImpliedFeeValidator();

    private static Transaction buy(String quantity, String price, String total) {
        return new Transaction(
                "a".repeat(64),
                Instant.parse("2026-01-19T07:45:18.487Z"),
                OperationType.BUY,
                "EUNK",
                new BigDecimal(quantity),
                new BigDecimal(price),
                new BigDecimal(total),
                "EUR",
                8);
    }

    @Test
    void feeAboveHalfAPercentIsFlagged() {
        // row 8 of the golden file: 2 × 96.20 = 192.40, paid 193.40
        List<RowIssue> issues = feeValidator.validate(buy("2", "96.20", "193.40"));

        assertThat(issues).singleElement().satisfies(i -> {
            assertThat(i.severity()).isEqualTo(Severity.WARNING);
            assertThat(i.fileRow()).isEqualTo(8);
            assertThat(i.msg().code()).isEqualTo("warning.implied_fee");
            assertThat(i.msg().params()).containsExactly(new BigDecimal("1.00"), new BigDecimal("0.52"));
        });
    }

    @Test
    void feeOfOnePercentIsFlagged() {
        assertThat(feeValidator.validate(buy("1", "100", "101"))).hasSize(1);
    }

    @Test
    void feeBelowTheThresholdIsNotFlagged() {
        // row 6 of the golden file: 0.92 on 406.08, 0.23%
        assertThat(feeValidator.validate(buy("47", "8.64", "407"))).isEmpty();
    }

    @Test
    void incomeIsNotChecked() {
        Transaction dividend = new Transaction(
                "b".repeat(64),
                Instant.parse("2026-01-05T10:00:00Z"),
                OperationType.DIVIDEND,
                "VGWD",
                null,
                null,
                new BigDecimal("1.11"),
                "EUR",
                4);

        assertThat(feeValidator.validate(dividend)).isEmpty();
    }

    @Test
    void futureOperationsAreRejected() {
        Clock newYear = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

        assertThat(new FutureDateValidator(newYear).validate(buy("2", "96.20", "193.40")))
                .singleElement()
                .satisfies(i -> {
                    assertThat(i.severity()).isEqualTo(Severity.ERROR);
                    assertThat(i.msg().code()).isEqualTo("error.future_date");
                });
    }

    @Test
    void chainStopsAtTheFirstError() {
        Clock newYear = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        ValidationChain chain = new ValidationChain(List.of(new FutureDateValidator(newYear), feeValidator));

        // the fee would be flagged too, but the row is already rejected
        assertThat(chain.validate(buy("2", "96.20", "193.40")))
                .extracting(RowIssue::severity)
                .containsExactly(Severity.ERROR);
    }
}
