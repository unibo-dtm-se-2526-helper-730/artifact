package it.helper730.ingestion.revolut;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import it.helper730.domain.Msg;
import it.helper730.domain.OperationType;
import it.helper730.ingestion.BrokerParserRegistry;
import it.helper730.ingestion.CsvReader;
import it.helper730.ingestion.ParseResult;
import it.helper730.ingestion.RowIssue;
import it.helper730.ingestion.exceptions.MalformedCsvException;
import it.helper730.ingestion.exceptions.UnsupportedBrokerException;
import it.helper730.ingestion.validation.FutureDateValidator;
import it.helper730.ingestion.validation.ImpliedFeeValidator;
import it.helper730.ingestion.validation.ValidationChain;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RevolutParserTest {

    private static final String HEADER = "Date,Ticker,Type,Quantity,Price per share,Total Amount,Currency\n";
    private static final Clock NOW = Clock.fixed(Instant.parse("2026-11-03T00:00:00Z"), ZoneOffset.UTC);

    private final BrokerParserRegistry registry = new BrokerParserRegistry(
            new CsvReader(),
            List.of(new RevolutParser(
                    new ValidationChain(List.of(new FutureDateValidator(NOW), new ImpliedFeeValidator())))));

    /**
     * Parses the given data rows after the Revolut header: the first data row is
     * line 2.
     */
    private ParseResult parse(String... rows) {
        return registry.parse(HEADER + String.join("\n", rows) + "\n");
    }

    @Test
    void readsATradeWithDecimalCommaAndCurrencyPrefix() {
        ParseResult result =
                parse("2025-10-13T06:00:44.187Z,VGWD,BUY - MARKET,\"2,83427915\",EUR 66.80,EUR 189.33,EUR");

        assertThat(result.broker()).isEqualTo("REVOLUT");
        assertThat(result.issues()).isEmpty();
        assertThat(result.transactions()).singleElement().satisfies(t -> {
            assertThat(t.type()).isEqualTo(OperationType.BUY);
            assertThat(t.ticker()).isEqualTo("VGWD");
            assertThat(t.executedAt()).isEqualTo(Instant.parse("2025-10-13T06:00:44.187Z"));
            assertThat(t.quantity()).isEqualByComparingTo("2.83427915");
            assertThat(t.pricePerShare()).isEqualByComparingTo("66.80");
            assertThat(t.totalAmount()).isEqualByComparingTo("189.33");
            assertThat(t.fileRow()).isEqualTo(2);
            assertThat(t.id()).hasSize(64);
        });
    }

    @Test
    void readsMicrosecondTimestampsAndSkipsBlankRows() {
        ParseResult result = parse(",,,,,,", "2026-01-05T17:52:54.885708Z,VGWD,DIVIDEND,,,EUR 1.11,EUR");

        assertThat(result.issues()).isEmpty();
        assertThat(result.transactions()).singleElement().satisfies(t -> {
            assertThat(t.type()).isEqualTo(OperationType.DIVIDEND);
            assertThat(t.executedAt()).isEqualTo(Instant.parse("2026-01-05T17:52:54.885708Z"));
            assertThat(t.quantity()).isNull();
            assertThat(t.fileRow()).isEqualTo(3);
        });
    }

    @ParameterizedTest
    @CsvSource({
        "BUY - MARKET, BUY",
        "BUY - LIMIT, BUY",
        "SELL - STOP, SELL",
        "CASH TOP-UP, CASH_TOP_UP",
        "CASH WITHDRAWAL, CASH_WITHDRAWAL",
        "CUSTODY FEE, CUSTODY_FEE"
    })
    void mapsOperationTypes(String raw, OperationType expected) {
        assertThat(RevolutParser.parseType(raw, 2)).isEqualTo(expected);
    }

    @Test
    void sameRowAlwaysHasTheSameId() {
        String row = "2026-01-12T08:36:05.199Z,5J50,BUY - LIMIT,47,EUR 8.64,EUR 407,EUR";

        String id = parse(row).transactions().getFirst().id();
        String sameRowOnAnotherLine = parse("", row).transactions().getFirst().id();
        String anotherRow = parse("2026-01-12T08:36:05.199Z,5J50,BUY - LIMIT,47,EUR 8.64,EUR 407.01,EUR")
                .transactions()
                .getFirst()
                .id();

        assertThat(sameRowOnAnotherLine).isEqualTo(id);
        assertThat(anotherRow).isNotEqualTo(id);
    }

    @Test
    void cashMovementsNeedNoTickerAndMayUseThousandsSeparators() {
        ParseResult result = parse("2026-01-01T10:00:00Z,,CASH TOP-UP,,,\"EUR 1,000.50\",EUR");

        assertThat(result.transactions()).singleElement().satisfies(t -> {
            assertThat(t.ticker()).isNull();
            assertThat(t.totalAmount()).isEqualByComparingTo("1000.50");
        });
    }

    @Test
    void rowsWithProblemsAreRejectedWithTheirLineNumber() {
        ParseResult result = parse(
                "2026-01-01T10:00:00Z,X,STOCK SPLIT,,,EUR 0,EUR",
                "2026-01-01T10:00:00Z,X,FOO,,,EUR 1,EUR",
                "2026-01-01T10:00:00Z,X,BUY - MARKET,1,USD 10,USD 10,USD",
                "2026-01-01T10:00:00Z,X,BUY - MARKET,1,USD 10,EUR 10,EUR",
                "2027-01-01T10:00:00Z,X,DIVIDEND,,,EUR 1,EUR",
                "not-a-date,X,DIVIDEND,,,EUR 1,EUR");

        assertThat(result.transactions()).isEmpty();
        assertThat(result.errors()).extracting(RowIssue::fileRow).containsExactly(2, 3, 4, 5, 6, 7);
        assertThat(result.errors())
                .extracting(i -> i.msg().code())
                .containsExactly(
                        "error.corporate_action",
                        "error.unknown_operation_type",
                        "error.unsupported_currency",
                        "error.currency_mismatch",
                        "error.future_date",
                        "error.invalid_field");
    }

    @Test
    void unknownHeaderIsAnUnsupportedBroker() {
        assertThatThrownBy(() -> registry.parse("Date,Symbol,Side\n"))
                .isInstanceOfSatisfying(
                        UnsupportedBrokerException.class,
                        e -> assertThat(e.code()).isEqualTo("UNSUPPORTED_BROKER"));
    }

    @Test
    void emptyFileIsMalformed() {
        assertThatThrownBy(() -> registry.parse(""))
                .isInstanceOfSatisfying(
                        MalformedCsvException.class, e -> assertThat(e.msg()).isEqualTo(Msg.of("error.empty_file")));
    }
}
