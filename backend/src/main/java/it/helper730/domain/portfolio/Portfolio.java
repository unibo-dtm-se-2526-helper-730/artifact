package it.helper730.domain.portfolio;

import java.math.BigDecimal;
import java.util.List;

/**
 * The open positions at cost, with their weights.
 *
 * @param positions    positions, largest cost first
 * @param byAssetClass cost and weight per asset class, largest first
 * @param totalCost    cost of the whole portfolio, in cents
 */
public record Portfolio(List<Position> positions, List<AssetClassWeight> byAssetClass, BigDecimal totalCost) {

    public Portfolio {
        positions = List.copyOf(positions);
        byAssetClass = List.copyOf(byAssetClass);
    }

    /** Whether nothing is held. */
    public boolean isEmpty() {
        return positions.isEmpty();
    }
}
