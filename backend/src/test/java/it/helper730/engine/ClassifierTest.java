package it.helper730.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import it.helper730.domain.AssetClass;
import it.helper730.domain.Disposal;
import it.helper730.domain.EventType;
import it.helper730.domain.IncomeCategory;
import it.helper730.domain.Instrument;
import it.helper730.domain.OperationType;
import it.helper730.domain.TaxableEvent;
import it.helper730.domain.Transaction;
import it.helper730.domain.exceptions.UnclassifiedInstrumentsException;
import it.helper730.domain.exceptions.UnsupportedTaxYearException;
import it.helper730.engine.rules.CurrentRegime;
import it.helper730.engine.rules.RuleSetFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ClassifierTest {

    private final Classifier classifier = new Classifier(new RuleSetFactory());
    private int row = 1;

    private Transaction trade(OperationType type, String ticker, String date, String total) {
        BigDecimal amount = new BigDecimal(total);
        int fileRow = row++;
        return new Transaction(
                String.format("%064x", fileRow),
                Instant.parse(date + "T10:00:00Z"),
                type,
                ticker,
                BigDecimal.ONE,
                amount.setScale(6, RoundingMode.HALF_UP),
                amount,
                "EUR",
                fileRow);
    }

    private Transaction payment(OperationType type, String ticker, String date, String net) {
        int fileRow = row++;
        return new Transaction(
                String.format("%064x", fileRow),
                Instant.parse(date + "T10:00:00Z"),
                type,
                ticker,
                null,
                null,
                new BigDecimal(net),
                "EUR",
                fileRow);
    }

    /** Buys one share at {@code cost} and sells it at {@code proceeds}. */
    private Disposal sale(String ticker, String year, String cost, String proceeds) {
        LotBook book = new LotBook();
        book.apply(trade(OperationType.BUY, ticker, year + "-01-10", cost));
        return book.apply(trade(OperationType.SELL, ticker, year + "-03-10", proceeds))
                .orElseThrow();
    }

    private static Instrument instrument(String ticker, AssetClass assetClass) {
        return new Instrument(ticker, null, null, assetClass, null, null);
    }

    @Test
    void stockGainIsFullyTaxableMiscellaneousIncomeWithItsLots() {
        TaxableEvent event = classifier.classify(sale("5J50", "2026", "100", "125"),
                instrument("5J50", AssetClass.STOCK));

        assertThat(event.type()).isEqualTo(EventType.CAPITAL_GAIN);
        assertThat(event.category()).isEqualTo(IncomeCategory.MISCELLANEOUS);
        assertThat(event.amount()).isEqualByComparingTo("25.00");
        assertThat(event.taxableAmount()).isEqualByComparingTo("25.00");
        assertThat(event.consumptions()).hasSize(1);
    }

    @Test
    void stockLossIsANegativeMiscellaneousEvent() {
        TaxableEvent event = classifier.classify(sale("5J50", "2026", "100", "83.10"),
                instrument("5J50", AssetClass.STOCK));

        assertThat(event.type()).isEqualTo(EventType.CAPITAL_LOSS);
        assertThat(event.category()).isEqualTo(IncomeCategory.MISCELLANEOUS);
        assertThat(event.amount()).isEqualByComparingTo("-16.90");
    }

    @Test
    void governmentBondGainIsTaxedAt12Point5Percent() {
        TaxableEvent event = classifier.classify(
                sale("BTP", "2026", "100", "200"), instrument("BTP", AssetClass.GOVERNMENT_BOND_WL));

        BigDecimal tax = event.taxableAmount().multiply(CurrentRegime.RATE).setScale(2, RoundingMode.HALF_UP);

        assertThat(event.taxableAmount()).isEqualByComparingTo("48.08");
        assertThat(tax).isEqualByComparingTo("12.50");
    }

    @Test
    void ucitsEtfGainIsCapitalIncome() {
        TaxableEvent event = classifier.classify(sale("XEON", "2026", "100", "101.50"),
                instrument("XEON", AssetClass.UCITS_ETF));

        assertThat(event.category()).isEqualTo(IncomeCategory.CAPITAL);
        assertThat(event.taxableAmount()).isEqualByComparingTo("1.50");
    }

    @Test
    void stockDividendIsGrossedUpForForeignWithholding() {
        Instrument stock = new Instrument("5J50", null, null, AssetClass.STOCK, null, new BigDecimal("0.15"));

        TaxableEvent event = classifier.classify(payment(OperationType.DIVIDEND, "5J50", "2026-04-01", "8.50"), stock);

        assertThat(event.type()).isEqualTo(EventType.DIVIDEND);
        assertThat(event.category()).isEqualTo(IncomeCategory.CAPITAL);
        assertThat(event.amount()).isEqualByComparingTo("10.00");
        assertThat(event.foreignTax()).isEqualByComparingTo("1.50");
        assertThat(event.consumptions()).isEmpty();
    }

    @Test
    void etfDividendWithoutWithholdingKeepsTheAmountReceived() {
        TaxableEvent event = classifier.classify(
                payment(OperationType.DIVIDEND, "VGWD", "2026-04-01", "1.23"),
                instrument("VGWD", AssetClass.UCITS_ETF));

        assertThat(event.amount()).isEqualByComparingTo("1.23");
        assertThat(event.foreignTax()).isEqualByComparingTo("0");
    }

    @Test
    void classifyAllListsEveryUnclassifiedTicker() {
        Disposal disposal = sale("5J50", "2026", "100", "90");
        Transaction dividend = payment(OperationType.DIVIDEND, "VGWD", "2026-04-01", "1.23");
        Map<String, Instrument> instruments = Map.of("VGWD", Instrument.unclassified("VGWD"));

        assertThatThrownBy(() -> classifier.classifyAll(List.of(disposal), List.of(dividend), instruments))
                .isInstanceOfSatisfying(UnclassifiedInstrumentsException.class, e -> {
                    assertThat(e.code()).isEqualTo("UNCLASSIFIED_INSTRUMENTS");
                    assertThat(e.tickers()).containsExactly("5J50", "VGWD");
                });
    }

    @Test
    void classifyAllReturnsEventsInChronologicalOrderAndIgnoresCashMovements() {
        Transaction dividend = payment(OperationType.DIVIDEND, "VGWD", "2026-02-01", "1.23");
        Transaction topUp = new Transaction(
                String.format("%064x", 999),
                Instant.parse("2026-01-01T10:00:00Z"),
                OperationType.CASH_TOP_UP,
                null,
                null,
                null,
                new BigDecimal("500"),
                "EUR",
                999);
        Disposal disposal = sale("5J50", "2026", "100", "90"); // sold on 2026-03-10
        Map<String, Instrument> instruments = Map.of(
                "VGWD", instrument("VGWD", AssetClass.UCITS_ETF),
                "5J50", instrument("5J50", AssetClass.STOCK));

        List<TaxableEvent> events = classifier.classifyAll(List.of(disposal), List.of(topUp, dividend), instruments);

        assertThat(events).extracting(TaxableEvent::type).containsExactly(EventType.DIVIDEND, EventType.CAPITAL_LOSS);
    }

    @Test
    void saleBeforeTheCurrentRegimeIsRejected() {
        Disposal disposal = sale("5J50", "2014", "100", "110");

        assertThatThrownBy(() -> classifier.classify(disposal, instrument("5J50", AssetClass.STOCK)))
                .isInstanceOf(UnsupportedTaxYearException.class);
    }
}
