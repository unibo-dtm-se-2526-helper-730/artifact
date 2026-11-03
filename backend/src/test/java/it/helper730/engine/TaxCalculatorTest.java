package it.helper730.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import it.helper730.domain.AssetClass;
import it.helper730.domain.Instrument;
import it.helper730.domain.OperationType;
import it.helper730.domain.TaxYearResult;
import it.helper730.domain.Transaction;
import it.helper730.engine.exceptions.UnclassifiedInstrumentsException;
import it.helper730.engine.rules.RuleSetFactory;
import it.helper730.engine.rules.exceptions.UnsupportedTaxYearException;
import it.helper730.engine.zainetto.CompensationStrategy;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TaxCalculatorTest {

    private final TaxCalculator calculator = new TaxCalculator(new RuleSetFactory(), CompensationStrategy.OLDEST_FIRST);
    private int row = 1;

    private Transaction trade(
            OperationType type, String ticker, String date, String quantity, String price, String total) {
        int fileRow = row++;
        return new Transaction(
                String.format("%064x", fileRow),
                Instant.parse(date + "T10:00:00Z"),
                type,
                ticker,
                new BigDecimal(quantity),
                new BigDecimal(price),
                new BigDecimal(total),
                "EUR",
                fileRow);
    }

    private Transaction dividend(String ticker, String date, String net) {
        int fileRow = row++;
        return new Transaction(
                String.format("%064x", fileRow),
                Instant.parse(date + "T10:00:00Z"),
                OperationType.DIVIDEND,
                ticker,
                null,
                null,
                new BigDecimal(net),
                "EUR",
                fileRow);
    }

    private static Instrument instrument(String ticker, AssetClass assetClass) {
        return new Instrument(ticker, null, null, assetClass, null, null);
    }

    private static Transaction golden(
            int fileRow, String at, String ticker, OperationType type, String quantity, String price, String total) {
        return new Transaction(
                String.format("%064x", fileRow),
                Instant.parse(at),
                type,
                ticker,
                quantity == null ? null : new BigDecimal(quantity),
                price == null ? null : new BigDecimal(price),
                new BigDecimal(total),
                "EUR",
                fileRow);
    }

    /**
     * The operations of revolut_golden.csv, written by hand until the parser
     * exists.
     */
    private static List<Transaction> goldenOperations() {
        return List.of(
                golden(3, "2025-10-13T06:00:44.187Z", "VGWD", OperationType.BUY, "2.83427915", "66.80", "189.33"),
                golden(4, "2026-01-05T17:52:54.885708Z", "VGWD", OperationType.DIVIDEND, null, null, "1.11"),
                golden(5, "2026-01-07T11:11:42.252Z", "VGWD", OperationType.BUY, "2.9440628", "71.33", "210"),
                golden(6, "2026-01-12T08:36:05.199Z", "5J50", OperationType.BUY, "47", "8.64", "407"),
                golden(7, "2026-01-16T13:29:07.734Z", "CEBS", OperationType.BUY, "20", "8.73", "174.62"),
                golden(8, "2026-01-19T07:45:18.487Z", "EUNK", OperationType.BUY, "2", "96.20", "193.40"),
                golden(9, "2026-02-27T17:20:41.244Z", "VGWD", OperationType.BUY, "5", "76.41", "382.05"),
                golden(10, "2026-04-07T15:42:19.811099Z", "VGWD", OperationType.DIVIDEND, null, null, "4"),
                golden(11, "2026-04-08T16:46:06.125Z", "XEON", OperationType.BUY, "4.90559597", "148.82", "730.04"),
                golden(12, "2026-05-27T07:44:19.205Z", "5J50", OperationType.SELL, "47", "8.30", "390.10"),
                golden(13, "2026-05-29T06:00:32.920Z", "XEON", OperationType.SELL, "4.9", "149.22", "729.33"));
    }

    private static Map<String, Instrument> goldenInstruments() {
        Map<String, Instrument> instruments = new HashMap<>();
        for (String ticker : List.of("VGWD", "CEBS", "EUNK", "XEON")) {
            instruments.put(ticker, instrument(ticker, AssetClass.UCITS_ETF));
        }
        instruments.put("5J50", instrument("5J50", AssetClass.STOCK));
        return instruments;
    }

    @Test
    void goldenOperationsGiveTheExpectedResultsFor2026() {
        TaxYearResult result = calculator.calculate(2026, goldenOperations(), goldenInstruments(), Map.of());

        // dividends 1.11 + 4.00, XEON gain on average cost without fees 1.96
        assertThat(result.capitalTaxable()).isEqualByComparingTo("7.07");
        assertThat(result.miscellaneousTaxable()).isEqualByComparingTo("0");
        assertThat(result.totalTax()).isEqualByComparingTo("1.84");
        assertThat(result.belowThreshold()).isTrue();
        assertThat(result.payableTax()).isEqualByComparingTo("0");
        // 5J50 loss
        assertThat(result.zainetto().closingTotal()).isEqualByComparingTo("16.90");
        // cash: 5.11 + 0.12 - 16.90, fees included
        assertThat(result.economicResult()).isEqualByComparingTo("-11.67");
        assertThat(result.events()).hasSize(4);
    }

    @Test
    void goldenOperationsHaveNoTaxIn2025() {
        TaxYearResult result = calculator.calculate(2025, goldenOperations(), goldenInstruments(), Map.of());

        assertThat(result.events()).isEmpty();
        assertThat(result.totalTax()).isEqualByComparingTo("0");
    }

    @Test
    void lossesOfPreviousYearsReduceMiscellaneousTax() {
        List<Transaction> operations = List.of(
                trade(OperationType.BUY, "ENI", "2025-01-10", "1", "100", "100"),
                trade(OperationType.SELL, "ENI", "2025-06-10", "1", "50", "50"),
                trade(OperationType.BUY, "ENI", "2026-01-10", "1", "100", "100"),
                trade(OperationType.SELL, "ENI", "2026-06-10", "1", "300", "300"));

        TaxYearResult result =
                calculator.calculate(2026, operations, Map.of("ENI", instrument("ENI", AssetClass.STOCK)), Map.of());

        // gain 200 minus the 50 loss of 2025
        assertThat(result.miscellaneousTaxable()).isEqualByComparingTo("150");
        assertThat(result.miscellaneousTax()).isEqualByComparingTo("39.00");
        assertThat(result.belowThreshold()).isFalse();
        assertThat(result.payableTax()).isEqualByComparingTo("39.00");
    }

    @Test
    void capitalIncomeIsTaxedEvenWithLossesInTheZainetto() {
        List<Transaction> operations = List.of(
                trade(OperationType.BUY, "ENI", "2025-01-10", "1", "100", "100"),
                trade(OperationType.SELL, "ENI", "2025-06-10", "1", "50", "50"),
                dividend("VWCE", "2026-03-01", "100"));
        Map<String, Instrument> instruments = Map.of(
                "ENI", instrument("ENI", AssetClass.STOCK),
                "VWCE", instrument("VWCE", AssetClass.UCITS_ETF));

        TaxYearResult result = calculator.calculate(2026, operations, instruments, Map.of());

        assertThat(result.capitalTax()).isEqualByComparingTo("26.00");
        assertThat(result.zainetto().closingTotal()).isEqualByComparingTo("50");
    }

    @Test
    void economicResultIncludesFeesThatTheTaxIgnores() {
        List<Transaction> operations = List.of(
                trade(OperationType.BUY, "VWCE", "2026-01-10", "10", "10", "101"),
                trade(OperationType.SELL, "VWCE", "2026-06-10", "10", "11", "109"));

        TaxYearResult result = calculator.calculate(
                2026, operations, Map.of("VWCE", instrument("VWCE", AssetClass.UCITS_ETF)), Map.of());

        // for tax: 110 - 100; in cash: 109 - 101
        assertThat(result.capitalTaxable()).isEqualByComparingTo("10");
        assertThat(result.economicResult()).isEqualByComparingTo("8");
    }

    @Test
    void operationsAfterTheYearAreIgnored() {
        List<Transaction> operations = List.of(
                dividend("VWCE", "2026-03-01", "10"),
                // a sale of shares never bought, in 2027, of an instrument nobody classified
                trade(OperationType.SELL, "ZZZ", "2027-01-10", "5", "1", "5"));

        TaxYearResult result = calculator.calculate(
                2026, operations, Map.of("VWCE", instrument("VWCE", AssetClass.UCITS_ETF)), Map.of());

        assertThat(result.capitalTaxable()).isEqualByComparingTo("10");
    }

    @Test
    void everyUnclassifiedInstrumentIsReportedAtOnce() {
        Map<String, Instrument> instruments = goldenInstruments();
        instruments.put("CEBS", Instrument.unclassified("CEBS"));
        instruments.remove("EUNK");

        assertThatThrownBy(() -> calculator.calculate(2026, goldenOperations(), instruments, Map.of()))
                .isInstanceOfSatisfying(
                        UnclassifiedInstrumentsException.class,
                        e -> assertThat(e.tickers()).containsExactly("CEBS", "EUNK"));
    }

    @Test
    void nonUcitsEtfIsExcludedAndReported() {
        List<Transaction> operations = List.of(
                trade(OperationType.BUY, "SPY", "2026-01-10", "1", "100", "100"),
                trade(OperationType.SELL, "SPY", "2026-06-10", "1", "150", "150"));

        TaxYearResult result = calculator.calculate(
                2026, operations, Map.of("SPY", instrument("SPY", AssetClass.NON_UCITS_ETF)), Map.of());

        assertThat(result.excludedEvents()).hasSize(1);
        assertThat(result.totalTax()).isEqualByComparingTo("0");
    }

    @Test
    void unsupportedYearIsRejected() {
        assertThatThrownBy(() -> calculator.calculate(2014, List.of(), Map.of(), Map.of()))
                .isInstanceOf(UnsupportedTaxYearException.class);
    }
}
