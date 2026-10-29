package it.helper730.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import it.helper730.domain.Disposal;
import it.helper730.domain.LotConsumption;
import it.helper730.domain.OperationType;
import it.helper730.domain.Transaction;
import it.helper730.domain.UncoveredSaleException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class LotBookTest {

    private int row = 1;

    /**
     * Builds a trade; the price is derived from the total so that the implied fee
     * is zero.
     */
    private Transaction trade(OperationType type, String ticker, String date, String quantity, String total) {
        BigDecimal qty = new BigDecimal(quantity);
        BigDecimal amount = new BigDecimal(total);
        int fileRow = row++;
        return new Transaction(
                String.format("%064x", fileRow),
                Instant.parse(date + "T10:00:00Z"),
                type,
                ticker,
                qty,
                amount.divide(qty, 6, RoundingMode.HALF_UP),
                amount,
                "EUR",
                fileRow);
    }

    private Transaction buy(String ticker, String date, String quantity, String total) {
        return trade(OperationType.BUY, ticker, date, quantity, total);
    }

    private Transaction sell(String ticker, String date, String quantity, String total) {
        return trade(OperationType.SELL, ticker, date, quantity, total);
    }

    @Test
    void saleConsumesTheMostRecentLotFirst() {
        LotBook book = new LotBook();
        Transaction older = buy("VGWD", "2026-01-10", "10", "100");
        Transaction newer = buy("VGWD", "2026-02-10", "10", "200");
        book.apply(older);
        book.apply(newer);

        Disposal disposal = book.apply(sell("VGWD", "2026-03-10", "5", "125")).orElseThrow();

        assertThat(disposal.consumptions()).singleElement().satisfies(c -> {
            assertThat(c.buyTransactionId()).isEqualTo(newer.id());
            assertThat(c.cost()).isEqualByComparingTo("100");
        });
        assertThat(disposal.gain()).isEqualByComparingTo("25.00");
        assertThat(book.openQuantities()).containsOnlyKeys("VGWD");
        assertThat(book.openQuantities().get("VGWD")).isEqualByComparingTo("15");
    }

    @Test
    void saleSpanningTwoLotsSplitsCostAndProceeds() {
        LotBook book = new LotBook();
        Transaction older = buy("XEON", "2026-01-10", "10", "100");
        Transaction newer = buy("XEON", "2026-02-10", "5", "75");
        book.apply(older);
        book.apply(newer);

        Disposal disposal = book.apply(sell("XEON", "2026-03-10", "8", "120")).orElseThrow();

        List<LotConsumption> c = disposal.consumptions();
        assertThat(c).hasSize(2);
        // newest lot first: all 5 shares, cost 75, proceeds 120 × 5/8 = 75
        assertThat(c.get(0).buyTransactionId()).isEqualTo(newer.id());
        assertThat(c.get(0).quantity()).isEqualByComparingTo("5");
        assertThat(c.get(0).proceeds()).isEqualByComparingTo("75");
        // then 3 shares of the older lot: cost 30, proceeds what is left of the 120
        assertThat(c.get(1).buyTransactionId()).isEqualTo(older.id());
        assertThat(c.get(1).cost()).isEqualByComparingTo("30");
        assertThat(c.get(1).proceeds()).isEqualByComparingTo("45");

        assertThat(disposal.cost()).isEqualByComparingTo("105");
        assertThat(disposal.gain()).isEqualByComparingTo("15.00");
        assertThat(book.openLots("XEON")).singleElement().satisfies(lot -> {
            assertThat(lot.quantity()).isEqualByComparingTo("7");
            assertThat(lot.cost()).isEqualByComparingTo("70");
        });
    }

    @Test
    void lossIsANegativeGain() {
        LotBook book = new LotBook();
        book.apply(buy("5J50", "2026-01-10", "2", "100"));

        Disposal disposal = book.apply(sell("5J50", "2026-05-10", "2", "83.10")).orElseThrow();

        assertThat(disposal.gain()).isEqualByComparingTo("-16.90");
    }

    @Test
    void partialSalesAddUpToTheOriginalCost() {
        LotBook book = new LotBook();
        book.apply(buy("VGWD", "2026-01-10", "3", "100"));

        BigDecimal totalCost = BigDecimal.ZERO;
        for (String date : List.of("2026-02-10", "2026-03-10", "2026-04-10")) {
            totalCost = totalCost.add(
                    book.apply(sell("VGWD", date, "1", "40")).orElseThrow().cost());
        }

        assertThat(totalCost).isEqualByComparingTo("100");
        assertThat(book.openQuantities()).isEmpty();
    }

    @Test
    void fractionalResidualStaysOpen() {
        LotBook book = new LotBook();
        book.apply(buy("XEON", "2026-01-10", "1", "100"));

        book.apply(sell("XEON", "2026-02-10", "0.99440403", "101"));

        assertThat(book.openQuantities().get("XEON")).isEqualByComparingTo("0.00559597");
    }

    @Test
    void uncoveredSaleIsRejectedAndLeavesTheBookUnchanged() {
        LotBook book = new LotBook();
        book.apply(buy("VGWD", "2026-01-10", "2", "100"));

        assertThatThrownBy(() -> book.apply(sell("VGWD", "2026-02-10", "3", "150")))
                .isInstanceOfSatisfying(UncoveredSaleException.class, e -> {
                    assertThat(e.code()).isEqualTo("UNCOVERED_SALE");
                    assertThat(e.msg().params()).containsExactly("VGWD", 2, "3", "2");
                });
        assertThat(book.openQuantities().get("VGWD")).isEqualByComparingTo("2");
        assertThat(book.disposals()).isEmpty();
    }

    @Test
    void lotsOfDifferentInstrumentsAreIndependent() {
        LotBook book = new LotBook();
        book.apply(buy("VGWD", "2026-01-10", "1", "50"));
        book.apply(buy("XEON", "2026-01-11", "1", "70"));

        Disposal disposal = book.apply(sell("VGWD", "2026-02-10", "1", "60")).orElseThrow();

        assertThat(disposal.cost()).isEqualByComparingTo("50");
        assertThat(book.openQuantities()).containsOnlyKeys("XEON");
    }

    @Test
    void replaySortsOperationsChronologically() {
        Transaction sale = sell("VGWD", "2026-03-10", "1", "60");
        Transaction purchase = buy("VGWD", "2026-01-10", "1", "50");

        LotBook book = LotBook.replay(List.of(sale, purchase));

        assertThat(book.disposals())
                .singleElement()
                .satisfies(d -> assertThat(d.gain()).isEqualByComparingTo("10.00"));
    }

    @Test
    void applyRejectsOperationsOutOfOrder() {
        LotBook book = new LotBook();
        Transaction later = buy("VGWD", "2026-03-10", "1", "50");
        Transaction earlier = buy("VGWD", "2026-01-10", "1", "50");
        book.apply(later);

        assertThatThrownBy(() -> book.apply(earlier)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nonTradesDoNotChangeLots() {
        LotBook book = new LotBook();
        Transaction dividend = new Transaction(
                String.format("%064x", 99),
                Instant.parse("2026-01-10T10:00:00Z"),
                OperationType.DIVIDEND,
                "VGWD",
                null,
                null,
                new BigDecimal("1.20"),
                "EUR",
                99);

        assertThat(book.apply(dividend)).isEmpty();
        assertThat(book.openQuantities()).isEmpty();
    }
}
