package it.helper730;

import static org.assertj.core.api.Assertions.assertThat;

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
import it.helper730.ingestion.revolut.RevolutParser;
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
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.Test;

/** revolut_golden.csv, from the file to the results expected in the README. */
class GoldenCsvTest {

    private static final Clock NOW = Clock.fixed(Instant.parse("2026-11-03T00:00:00Z"), ZoneOffset.UTC);

    private final RuleSetFactory rules = new RuleSetFactory();

    private static ParseResult parseGolden() throws IOException {
        BrokerParserRegistry registry = new BrokerParserRegistry(
                new CsvReader(),
                List.of(new RevolutParser(
                        new ValidationChain(List.of(new FutureDateValidator(NOW), new ImpliedFeeValidator())))));
        try (InputStream input = GoldenCsvTest.class.getResourceAsStream("/golden/revolut_golden.csv")) {
            assertThat(input).as("golden csv on the test classpath").isNotNull();
            return registry.parse(input);
        }
    }

    @Test
    void parserReadsExactlyTheGoldenOperations() throws IOException {
        ParseResult result = parseGolden();

        assertThat(result.errors()).isEmpty();
        assertThat(result.warnings()).singleElement().satisfies(w -> {
            assertThat(w.fileRow()).isEqualTo(8);
            assertThat(w.msg().code()).isEqualTo("warning.implied_fee");
            assertThat(w.msg().params()).containsExactly(new BigDecimal("1.00"), new BigDecimal("0.52"));
        });
        // the same values as the operations written by hand for the engine tests, ids
        // aside
        assertThat(result.transactions())
                .usingRecursiveFieldByFieldElementComparator(RecursiveComparisonConfiguration.builder()
                        .withIgnoredFields("id")
                        .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                        .build())
                .containsExactlyElementsOf(GoldenData.operations());
    }

    @Test
    void goldenResultsFromTheFile() throws IOException {
        List<Transaction> operations = parseGolden().transactions();

        TaxYearResult tax = new TaxCalculator(rules, CompensationStrategy.OLDEST_FIRST)
                .calculate(2026, operations, GoldenData.instruments(), Map.of());
        assertThat(tax.capitalTaxable()).isEqualByComparingTo("7.07");
        assertThat(tax.totalTax()).isEqualByComparingTo("1.84");
        assertThat(tax.payableTax()).isEqualByComparingTo("0");
        assertThat(tax.economicResult()).isEqualByComparingTo("-11.67");
        assertThat(tax.zainetto().closingTotal()).isEqualByComparingTo("16.90");

        IvafeResult ivafe =
                new IvafeCalculator(rules).calculate(2025, operations, Map.of("VGWD", new BigDecimal("70")), Map.of());
        assertThat(ivafe.total()).isEqualByComparingTo("0.09");
        assertThat(ivafe.rows())
                .singleElement()
                .satisfies(r -> assertThat(r.holdingDays()).isEqualTo(80));
    }
}
