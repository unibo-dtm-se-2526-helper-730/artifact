package it.helper730.domain.portfolio;

import it.helper730.domain.AssetClass;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * An open position: the shares of one instrument still held.
 *
 * @param ticker      instrument ticker
 * @param name        instrument name; may be {@code null}
 * @param assetClass  tax classification; {@code null} if not classified yet
 * @param quantity    shares held
 * @param cost        what was paid for the shares held, fees included, in cents
 * @param taxCost     tax cost of the shares held, as the cost method requires,
 *                    in cents
 * @param averageCost cost per share
 * @param weight      share of the portfolio cost, between 0 and 1
 */
public record Position(
        String ticker,
        String name,
        AssetClass assetClass,
        BigDecimal quantity,
        BigDecimal cost,
        BigDecimal taxCost,
        BigDecimal averageCost,
        BigDecimal weight) {

    public Position {
        Objects.requireNonNull(ticker, "ticker");
        Objects.requireNonNull(quantity, "quantity");
        Objects.requireNonNull(cost, "cost");
        Objects.requireNonNull(taxCost, "taxCost");
        Objects.requireNonNull(averageCost, "averageCost");
        Objects.requireNonNull(weight, "weight");
    }
}
