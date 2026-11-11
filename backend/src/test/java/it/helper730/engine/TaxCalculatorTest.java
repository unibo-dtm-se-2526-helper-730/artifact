package it.helper730.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import it.helper730.GoldenData;
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

    @Test
    void goldenOperationsGiveTheExpectedResultsFor2026() {
        TaxYearResult result = calculator.calculate(2026, GoldenData.operations(), GoldenData.instruments(), Map.of());

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
        TaxYearResult result = calculator.calculate(2025, GoldenData.operations(), GoldenData.instruments(), Map.of());

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
        Map<String, Instrument> instruments = GoldenData.instruments();
        instruments.put("CEBS", Instrument.unclassified("CEBS"));
        instruments.remove("EUNK");

        assertThatThrownBy(() -> calculator.calculate(2026, GoldenData.operations(), instruments, Map.of()))
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

    @Test
    void ucitsEtfLossIsReducedForItsGovernmentBondShare() {
        Map<String, Instrument> instruments =
                Map.of("AGGH", new Instrument("AGGH", null, null, AssetClass.UCITS_ETF, new BigDecimal("0.40"), null));
        List<Transaction> operations = List.of(
                trade(OperationType.BUY, "AGGH", "2026-02-02", "10", "100", "1001"),
                trade(OperationType.SELL, "AGGH", "2026-09-01", "10", "90", "899"));

        TaxYearResult result = calculator.calculate(2026, operations, instruments, Map.of());

        // loss 900 - 1000 on average cost without fees, 40% in government bonds:
        // 100 * (1 - 0.40 * (1 - 0.4808)) = 79.232
        assertThat(result.zainetto().closingTotal()).isEqualByComparingTo("79.23");
    }
}
