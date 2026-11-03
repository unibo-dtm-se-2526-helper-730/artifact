package it.helper730.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import it.helper730.domain.AssetClass;
import it.helper730.domain.CostMethod;
import it.helper730.domain.Disposal;
import it.helper730.domain.Instrument;
import it.helper730.domain.Lot;
import it.helper730.domain.LotConsumption;
import it.helper730.domain.OperationType;
import it.helper730.domain.Transaction;
import it.helper730.engine.exceptions.UnclassifiedInstrumentsException;
import it.helper730.engine.rules.RuleSetFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WeightedAverageCostTest {

    private static final CostMethodPolicy AVERAGE = CostMethodPolicy.always(CostMethod.WEIGHTED_AVERAGE);

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

    /** 10 shares at 10 € and 10 shares at 20 €, each purchase with a 1 € fee. */
    private LotBook bookWithTwoPurchases(CostMethodPolicy policy) {
        LotBook book = new LotBook(policy);
        book.apply(trade(OperationType.BUY, "VWCE", "2026-01-10", "10", "10", "101"));
        book.apply(trade(OperationType.BUY, "VWCE", "2026-02-10", "10", "20", "201"));
        return book;
    }

    /** 5 shares at 30 €, with a 1 € fee: 149 € received. */
    private Transaction saleOfFive() {
        return trade(OperationType.SELL, "VWCE", "2026-03-10", "5", "30", "149");
    }

    @Test
    void costIsTheWeightedAverageWithoutFees() {
        Disposal disposal = bookWithTwoPurchases(AVERAGE).apply(saleOfFive()).orElseThrow();

        assertThat(disposal.costMethod()).isEqualTo(CostMethod.WEIGHTED_AVERAGE);
        // average cost (100 + 200) / 20 = 15 per share; purchase fees ignored
        assertThat(disposal.cost()).isEqualByComparingTo("75");
        // proceeds 5 × 30; the sale fee is not deducted
        assertThat(disposal.proceeds()).isEqualByComparingTo("150");
        assertThat(disposal.gain()).isEqualByComparingTo("75.00");
        // every purchase contributes to the average, newest first
        assertThat(disposal.consumptions()).hasSize(2);
    }

    @Test
    void lifoOnTheSameOperationsGivesADifferentGain() {
        Disposal disposal = bookWithTwoPurchases(CostMethodPolicy.always(CostMethod.LIFO))
                .apply(saleOfFive())
                .orElseThrow();

        // half of the newest lot, 201 € with its fee
        assertThat(disposal.cost()).isEqualByComparingTo("100.50");
        // the amount actually received
        assertThat(disposal.proceeds()).isEqualByComparingTo("149");
        assertThat(disposal.gain()).isEqualByComparingTo("48.50");
    }

    @Test
    void salesLeaveTheAverageCostUnchanged() {
        LotBook book = bookWithTwoPurchases(AVERAGE);

        book.apply(saleOfFive());

        List<Lot> lots = book.openLots("VWCE");
        BigDecimal quantity = lots.stream().map(Lot::quantity).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cost = lots.stream().map(Lot::costExcludingFees).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(quantity).isEqualByComparingTo("15");
        assertThat(cost.divide(quantity, 10, RoundingMode.HALF_UP)).isEqualByComparingTo("15");
    }

    @Test
    void fractionalSalesAddUpToTheTotalCost() {
        LotBook book = new LotBook(AVERAGE);
        book.apply(trade(OperationType.BUY, "XEON", "2026-01-10", "3", "10", "30"));
        book.apply(trade(OperationType.BUY, "XEON", "2026-01-11", "0.33333333", "11", "3.67"));
        book.apply(trade(OperationType.BUY, "XEON", "2026-01-12", "7", "13", "91"));

        String[] sold = {"1", "2.5", "0.12345678", "6.7", "0.00987655"};
        BigDecimal totalCost = BigDecimal.ZERO;
        for (int i = 0; i < sold.length; i++) {
            Disposal disposal = book.apply(trade(OperationType.SELL, "XEON", "2026-02-1" + i, sold[i], "12", "1"))
                    .orElseThrow();
            BigDecimal parts = disposal.consumptions().stream()
                    .map(LotConsumption::quantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(parts).isEqualByComparingTo(sold[i]);
            totalCost = totalCost.add(disposal.cost());
        }

        // 3 × 10 + 0.33333333 × 11 + 7 × 13: what was paid, fees excluded, to the last
        // decimal
        assertThat(totalCost).isEqualByComparingTo("124.66666663");
        assertThat(book.openQuantities()).isEmpty();
    }

    @Test
    void rulesChooseWeightedAverageForEtfsAndLifoForStocks() {
        CostMethodPolicy policy = CostMethodPolicy.fromRules(
                new RuleSetFactory(),
                Map.of(
                        "VWCE", new Instrument("VWCE", null, null, AssetClass.UCITS_ETF, null, null),
                        "5J50", new Instrument("5J50", null, null, AssetClass.STOCK, null, null)));

        assertThat(policy.forSale(trade(OperationType.SELL, "VWCE", "2026-03-10", "1", "10", "10")))
                .isEqualTo(CostMethod.WEIGHTED_AVERAGE);
        assertThat(policy.forSale(trade(OperationType.SELL, "5J50", "2026-03-10", "1", "10", "10")))
                .isEqualTo(CostMethod.LIFO);
    }

    @Test
    void rulesRejectSalesOfUnclassifiedInstruments() {
        CostMethodPolicy policy = CostMethodPolicy.fromRules(new RuleSetFactory(), Map.of());

        assertThatThrownBy(() -> policy.forSale(trade(OperationType.SELL, "XEON", "2026-03-10", "1", "10", "10")))
                .isInstanceOf(UnclassifiedInstrumentsException.class);
    }
}
