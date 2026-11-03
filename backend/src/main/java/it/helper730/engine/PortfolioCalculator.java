package it.helper730.engine;

import it.helper730.domain.AssetClass;
import it.helper730.domain.CostMethod;
import it.helper730.domain.Instrument;
import it.helper730.domain.Lot;
import it.helper730.domain.Money;
import it.helper730.domain.Transaction;
import it.helper730.domain.portfolio.AssetClassWeight;
import it.helper730.domain.portfolio.Portfolio;
import it.helper730.domain.portfolio.Position;
import it.helper730.engine.rules.RuleSetFactory;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Computes the current portfolio at cost. Sales consume lots with the cost
 * method of the instrument when it is classified, with LIFO otherwise, so the
 * portfolio is available before classification.
 */
public final class PortfolioCalculator {

    /**
     * Scale of the weights: a millionth is far below what a chart or a table shows.
     */
    public static final int WEIGHT_SCALE = 6;

    private final RuleSetFactory rules;

    public PortfolioCalculator(RuleSetFactory rules) {
        this.rules = Objects.requireNonNull(rules, "rules");
    }

    /**
     * Computes the portfolio after all the given operations.
     *
     * @param transactions all stored operations
     * @param instruments  instruments by ticker; unknown or unclassified
     *                     instruments are allowed
     */
    public Portfolio calculate(Collection<Transaction> transactions, Map<String, Instrument> instruments) {
        CostMethodPolicy policy = sale -> costMethod(sale, instruments.get(sale.ticker()));
        LotBook book = LotBook.replay(transactions, policy);
        // rules of the last year with operations decide the tax cost of what is still
        // held
        int referenceYear = Math.max(
                transactions.stream().mapToInt(Transaction::taxYear).max().orElse(0),
                RuleSetFactory.FIRST_SUPPORTED_YEAR);

        List<Holding> holdings = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> open : book.openQuantities().entrySet()) {
            String ticker = open.getKey();
            Instrument instrument = instruments.get(ticker);
            AssetClass assetClass = instrument == null ? null : instrument.assetClass();
            boolean withFees = assetClass == null
                    || rules.forYear(referenceYear).costMethod(instrument).includesFees();
            BigDecimal cost = BigDecimal.ZERO;
            BigDecimal taxCost = BigDecimal.ZERO;
            for (Lot lot : book.openLots(ticker)) {
                cost = cost.add(lot.cost());
                taxCost = taxCost.add(withFees ? lot.cost() : lot.costExcludingFees());
            }
            holdings.add(new Holding(
                    ticker, instrument == null ? null : instrument.name(), assetClass, open.getValue(), cost, taxCost));
        }

        BigDecimal total = holdings.stream().map(Holding::cost).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Position> positions = holdings.stream()
                .sorted(Comparator.comparing(Holding::cost).reversed().thenComparing(Holding::ticker))
                .map(h -> new Position(
                        h.ticker,
                        h.name,
                        h.assetClass,
                        h.quantity,
                        Money.round(h.cost),
                        Money.round(h.taxCost),
                        h.cost.divide(h.quantity, Money.SCALE + 2, Money.ROUNDING),
                        weight(h.cost, total)))
                .toList();
        return new Portfolio(positions, byAssetClass(holdings, total), Money.round(total));
    }

    /**
     * Method of the instrument if classified and in a supported year, LIFO
     * otherwise.
     */
    private CostMethod costMethod(Transaction sale, Instrument instrument) {
        if (instrument == null || !instrument.isClassified() || sale.taxYear() < RuleSetFactory.FIRST_SUPPORTED_YEAR) {
            return CostMethod.LIFO;
        }
        return rules.forYear(sale.taxYear()).costMethod(instrument);
    }

    private static List<AssetClassWeight> byAssetClass(List<Holding> holdings, BigDecimal total) {
        Map<AssetClass, BigDecimal> costs = new LinkedHashMap<>();
        for (Holding h : holdings) {
            costs.merge(h.assetClass, h.cost, BigDecimal::add);
        }
        return costs.entrySet().stream()
                .map(e -> new AssetClassWeight(e.getKey(), Money.round(e.getValue()), weight(e.getValue(), total)))
                .sorted(Comparator.comparing(AssetClassWeight::cost).reversed())
                .toList();
    }

    private static BigDecimal weight(BigDecimal part, BigDecimal total) {
        return total.signum() == 0 ? BigDecimal.ZERO : part.divide(total, WEIGHT_SCALE, Money.ROUNDING);
    }

    /** Totals of one ticker before rounding. */
    private record Holding(
            String ticker,
            String name,
            AssetClass assetClass,
            BigDecimal quantity,
            BigDecimal cost,
            BigDecimal taxCost) {}
}
