package it.helper730;

import static org.assertj.core.api.Assertions.assertThat;

import it.helper730.domain.AssetClass;
import it.helper730.domain.Instrument;
import it.helper730.domain.TaxYearResult;
import it.helper730.domain.Transaction;
import it.helper730.domain.ivafe.IvafeResult;
import it.helper730.engine.TaxCalculator;
import it.helper730.engine.ivafe.IvafeCalculator;
import it.helper730.engine.rules.RuleSetFactory;
import it.helper730.engine.zainetto.CompensationStrategy;
import it.helper730.ingestion.BrokerParserRegistry;
import it.helper730.ingestion.CsvReader;
import it.helper730.ingestion.ParseResult;
import it.helper730.ingestion.RowIssue;
import it.helper730.ingestion.traderepublic.TradeRepublicParser;
import it.helper730.ingestion.validation.FutureDateValidator;
import it.helper730.ingestion.validation.ImpliedFeeValidator;
import it.helper730.ingestion.validation.ValidationChain;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * TradeRepublicGoldenTest.
 *
 * Acceptance test of the Trade Republic import: trade_republic_golden.csv is
 * parsed and run through the tax and IVAFE calculators for 2025, and the
 * results must match the expected ones.
 */
class TradeRepublicGoldenTest {

    private static final Clock NOW = Clock.fixed(Instant.parse("2026-11-03T00:00:00Z"), ZoneOffset.UTC);

    private static final String VWCE = "IE00BK5BQT80";
    private static final String SXR8 = "IE00B5BMR087";
    private static final String AAPL = "US0378331005";

    private static final Map<String, Instrument> INSTRUMENTS = Map.of(
            VWCE, new Instrument(VWCE, VWCE, null, AssetClass.UCITS_ETF, null, null),
            SXR8, new Instrument(SXR8, SXR8, null, AssetClass.UCITS_ETF, null, null),
            AAPL, new Instrument(AAPL, AAPL, null, AssetClass.STOCK, null, null));

    private final RuleSetFactory rules = new RuleSetFactory();

    private static ParseResult parseGolden() throws IOException {
        BrokerParserRegistry registry = new BrokerParserRegistry(
                new CsvReader(),
                List.of(new TradeRepublicParser(
                        new ValidationChain(List.of(new FutureDateValidator(NOW), new ImpliedFeeValidator())))));
        try (InputStream input =
                TradeRepublicGoldenTest.class.getResourceAsStream("/golden/trade_republic_golden.csv")) {
            assertThat(input).as("golden csv on the test classpath").isNotNull();
            return registry.parse(input);
        }
    }

    @Test
    void parserReadsEveryOperation() throws IOException {
        ParseResult result = parseGolden();

        assertThat(result.broker()).isEqualTo("TRADE_REPUBLIC");
        assertThat(result.errors()).isEmpty();
        assertThat(result.transactions()).hasSize(16);
        // the 1 EUR fee is 0.67% of the 150 EUR trades, above the threshold of 0.5%
        assertThat(result.warnings()).extracting(RowIssue::fileRow).containsExactly(4, 12, 13, 15);
        assertThat(result.transactions()).filteredOn(t -> t.taxYear() == 2026).hasSize(1);
    }

    @Test
    void goldenResultsOf2025() throws IOException {
        List<Transaction> operations = parseGolden().transactions();

        TaxYearResult tax = new TaxCalculator(rules, CompensationStrategy.OLDEST_FIRST)
                .calculate(2025, operations, INSTRUMENTS, Map.of());
        // dividend 150 + gain on the ETF 50, taxed at 26%
        assertThat(tax.capitalTaxable()).isEqualByComparingTo("200.00");
        assertThat(tax.totalTax()).isEqualByComparingTo("52.00");
        assertThat(tax.payableTax()).isEqualByComparingTo("52.00");
        // the losses on S&P 500 and Apple go into the zainetto
        assertThat(tax.zainetto().closingTotal()).isEqualByComparingTo("52.17");
        assertThat(tax.economicResult()).isEqualByComparingTo("143.69");

        IvafeResult ivafe = new IvafeCalculator(rules)
                .calculate(
                        2025,
                        operations,
                        Map.of(VWCE, new BigDecimal("130"), SXR8, new BigDecimal("520"), AAPL, new BigDecimal("210")),
                        Map.of());
        assertThat(ivafe.isComplete()).isTrue();
        assertThat(ivafe.total()).isEqualByComparingTo("1.82");
    }
}
