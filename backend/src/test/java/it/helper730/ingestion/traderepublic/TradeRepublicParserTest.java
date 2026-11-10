package it.helper730.ingestion.traderepublic;

import static org.assertj.core.api.Assertions.assertThat;

import it.helper730.domain.OperationType;
import it.helper730.ingestion.BrokerParserRegistry;
import it.helper730.ingestion.CsvReader;
import it.helper730.ingestion.ParseResult;
import it.helper730.ingestion.revolut.RevolutParser;
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

class TradeRepublicParserTest {

    private static final String HEADER = String.join(",", TradeRepublicParser.HEADER) + "\n";
    private static final Clock NOW = Clock.fixed(Instant.parse("2026-11-03T00:00:00Z"), ZoneOffset.UTC);

    private static final ValidationChain VALIDATION =
            new ValidationChain(List.of(new FutureDateValidator(NOW), new ImpliedFeeValidator()));

    private final BrokerParserRegistry registry = new BrokerParserRegistry(
            new CsvReader(), List.of(new RevolutParser(VALIDATION), new TradeRepublicParser(VALIDATION)));

    /**
     * Parses the given data rows after the header: the first data row is line 2.
     */
    private ParseResult parse(String... rows) {
        return registry.parse(HEADER + String.join("\n", rows) + "\n");
    }

    /** A row of the export: only the columns read by the parser have a value. */
    private static String row(
            String type, String symbol, String shares, String price, String amount, String fee, String tax, String id) {
        String datetime = "2025-06-02T09:31:56.778Z";
        return String.join(
                ",",
                datetime,
                "2025-06-02",
                "DEFAULT",
                "",
                type,
                "",
                "",
                symbol,
                shares,
                price,
                amount,
                fee,
                tax,
                "EUR",
                "",
                "",
                "",
                "",
                id,
                "",
                "",
                "",
                "");
    }

    @Test
    void aPurchaseIncludesItsFee() {
        ParseResult result =
                parse(row("BUY", "IE00BK5BQT80", "1.6000000000", "125.000000", "-200.00", "-1.00", "", "a"));

        assertThat(result.broker()).isEqualTo("TRADE_REPUBLIC");
        assertThat(result.issues()).isEmpty();
        assertThat(result.transactions()).singleElement().satisfies(t -> {
            assertThat(t.type()).isEqualTo(OperationType.BUY);
            assertThat(t.ticker()).isEqualTo("IE00BK5BQT80");
            assertThat(t.executedAt()).isEqualTo(Instant.parse("2025-06-02T09:31:56.778Z"));
            assertThat(t.quantity()).isEqualByComparingTo("1.6");
            assertThat(t.pricePerShare()).isEqualByComparingTo("125");
            assertThat(t.totalAmount()).isEqualByComparingTo("201.00");
            assertThat(t.fileRow()).isEqualTo(2);
        });
    }

    @Test
    void aSaleIsNetOfItsFeeAndHasAPositiveQuantity() {
        ParseResult result =
                parse(row("SELL", "IE00BK5BQT80", "-2.0000000000", "150.000000", "300.00", "-1.00", "", "b"));

        assertThat(result.transactions()).singleElement().satisfies(t -> {
            assertThat(t.type()).isEqualTo(OperationType.SELL);
            assertThat(t.quantity()).isEqualByComparingTo("2");
            assertThat(t.totalAmount()).isEqualByComparingTo("299.00");
        });
    }

    @Test
    void aDividendIsNetOfTax() {
        ParseResult result = parse(row("DIVIDEND", "US0378331005", "", "", "10.00", "", "-1.50", "c"));

        assertThat(result.issues()).isEmpty();
        assertThat(result.transactions()).singleElement().satisfies(t -> {
            assertThat(t.type()).isEqualTo(OperationType.DIVIDEND);
            assertThat(t.quantity()).isNull();
            assertThat(t.totalAmount()).isEqualByComparingTo("8.50");
        });
    }

    @ParameterizedTest
    @CsvSource({"CASH_TOP_UP, 300.00, CASH_TOP_UP", "CASH_WITHDRAWAL, -200.00, CASH_WITHDRAWAL", "coupon, 5.00, COUPON"
    })
    void theTypeColumnHoldsTheOperationType(String type, String amount, OperationType expected) {
        String symbol = expected == OperationType.COUPON ? "XS0000000001" : "";
        ParseResult result = parse(row(type, symbol, "", "", amount, "", "", "d"));

        assertThat(result.transactions()).singleElement().satisfies(t -> {
            assertThat(t.type()).isEqualTo(expected);
            assertThat(t.totalAmount()).isPositive();
        });
    }

    @Test
    void aRowWithAnUnknownTypeIsAnErrorAndTheOthersAreImported() {
        ParseResult result = parse(
                row("INTEREST_PAYMENT", "", "", "", "0.50", "", "", "e"),
                row("CASH_TOP_UP", "", "", "", "100.00", "", "", "f"));

        assertThat(result.transactions()).hasSize(1);
        assertThat(result.errors()).singleElement().satisfies(e -> {
            assertThat(e.fileRow()).isEqualTo(2);
            assertThat(e.msg().code()).isEqualTo("error.unknown_operation_type");
        });
    }

    @Test
    void aRowWithoutTransactionIdIsAnError() {
        ParseResult result = parse(row("CASH_TOP_UP", "", "", "", "100.00", "", "", ""));

        assertThat(result.transactions()).isEmpty();
        assertThat(result.errors())
                .singleElement()
                .satisfies(e -> assertThat(e.msg().code()).isEqualTo("error.missing_field"));
    }

    @Test
    void theIdDependsOnlyOnTheTransactionId() {
        ParseResult first = parse(row("CASH_TOP_UP", "", "", "", "100.00", "", "", "same"));
        ParseResult again = parse(row("CASH_TOP_UP", "", "", "", "100.00", "", "", "same"));
        ParseResult other = parse(row("CASH_TOP_UP", "", "", "", "100.00", "", "", "other"));

        String id = first.transactions().getFirst().id();
        assertThat(again.transactions().getFirst().id()).isEqualTo(id);
        assertThat(other.transactions().getFirst().id()).isNotEqualTo(id);
    }

    @Test
    void theBrokerIsInGermany() {
        assertThat(registry.countryOf("TRADE_REPUBLIC")).contains("DE");
        assertThat(registry.countryOf("REVOLUT")).contains("LT");
        assertThat(registry.countryOf("UNKNOWN")).isEmpty();
    }
}
