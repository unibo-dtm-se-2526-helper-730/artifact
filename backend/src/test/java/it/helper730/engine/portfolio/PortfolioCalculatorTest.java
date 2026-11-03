package it.helper730.engine.portfolio;

import static org.assertj.core.api.Assertions.assertThat;

import it.helper730.GoldenData;
import it.helper730.domain.AssetClass;
import it.helper730.domain.Instrument;
import it.helper730.domain.OperationType;
import it.helper730.domain.Transaction;
import it.helper730.domain.portfolio.AssetClassWeight;
import it.helper730.domain.portfolio.Portfolio;
import it.helper730.domain.portfolio.Position;
import it.helper730.engine.PortfolioCalculator;
import it.helper730.engine.rules.RuleSetFactory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PortfolioCalculatorTest {

    private final PortfolioCalculator calculator = new PortfolioCalculator(new RuleSetFactory());
    private int row = 1;

    private Transaction trade(OperationType type, String date, String quantity, String price, String total) {
        int fileRow = row++;
        return new Transaction(
                String.format("%064x", fileRow),
                Instant.parse(date + "T10:00:00Z"),
                type,
                "VWCE",
                new BigDecimal(quantity),
                new BigDecimal(price),
                new BigDecimal(total),
                "EUR",
                fileRow);
    }

    /** Two purchases with a 1 € fee each, then half of the shares sold. */
    private List<Transaction> twoPurchasesAndAPartialSale() {
        return List.of(
                trade(OperationType.BUY, "2026-01-10", "10", "10", "101"),
                trade(OperationType.BUY, "2026-02-10", "10", "20", "201"),
                trade(OperationType.SELL, "2026-03-10", "10", "30", "299"));
    }

    @Test
    void goldenPortfolio() {
        Portfolio portfolio = calculator.calculate(GoldenData.operations(), GoldenData.instruments());

        assertThat(portfolio.positions()).extracting(Position::ticker).containsExactly("VGWD", "EUNK", "CEBS", "XEON");
        assertThat(portfolio.totalCost()).isEqualByComparingTo("1150.23");

        Position vgwd = portfolio.positions().get(0);
        assertThat(vgwd.quantity()).isEqualByComparingTo("10.77834195");
        assertThat(vgwd.cost()).isEqualByComparingTo("781.38");
        assertThat(vgwd.weight()).isEqualByComparingTo("0.679323");

        // the fee of row 8 is part of what was paid, not of the tax cost of an ETF
        Position eunk = portfolio.positions().get(1);
        assertThat(eunk.cost()).isEqualByComparingTo("193.40");
        assertThat(eunk.taxCost()).isEqualByComparingTo("192.40");

        Position xeon = portfolio.positions().get(3);
        assertThat(xeon.quantity()).isEqualByComparingTo("0.00559597");
        assertThat(xeon.cost()).isEqualByComparingTo("0.83");

        // 5J50 was sold: only ETFs are left
        assertThat(portfolio.byAssetClass()).singleElement().satisfies(w -> {
            assertThat(w.assetClass()).isEqualTo(AssetClass.UCITS_ETF);
            assertThat(w.weight()).isEqualByComparingTo("1");
        });
    }

    @Test
    void classifiedEtfKeepsTheAverageCostAfterAPartialSale() {
        Map<String, Instrument> instruments =
                Map.of("VWCE", new Instrument("VWCE", null, null, AssetClass.UCITS_ETF, null, null));

        Position position = calculator
                .calculate(twoPurchasesAndAPartialSale(), instruments)
                .positions()
                .get(0);

        // half of each lot is left: 50.50 + 100.50 paid, 50 + 100 for tax
        assertThat(position.quantity()).isEqualByComparingTo("10");
        assertThat(position.cost()).isEqualByComparingTo("151.00");
        assertThat(position.taxCost()).isEqualByComparingTo("150.00");
        assertThat(position.averageCost()).isEqualByComparingTo("15.1");
    }

    @Test
    void unclassifiedInstrumentUsesLifoAndHasNoClass() {
        Portfolio portfolio = calculator.calculate(twoPurchasesAndAPartialSale(), Map.of());

        // LIFO sold the newer lot: the first one is left, fee included
        Position position = portfolio.positions().get(0);
        assertThat(position.assetClass()).isNull();
        assertThat(position.cost()).isEqualByComparingTo("101.00");
        assertThat(position.taxCost()).isEqualByComparingTo("101.00");
        assertThat(portfolio.byAssetClass())
                .extracting(AssetClassWeight::assetClass)
                .containsExactly((AssetClass) null);
    }

    @Test
    void nothingHeldGivesAnEmptyPortfolio() {
        Portfolio portfolio = calculator.calculate(List.of(), Map.of());

        assertThat(portfolio.isEmpty()).isTrue();
        assertThat(portfolio.totalCost()).isEqualByComparingTo("0");
    }
}
