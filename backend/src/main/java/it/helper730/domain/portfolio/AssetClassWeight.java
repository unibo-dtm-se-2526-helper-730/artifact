package it.helper730.domain.portfolio;

import it.helper730.domain.AssetClass;
import java.math.BigDecimal;

/**
 * The part of the portfolio in one asset class.
 *
 * @param assetClass asset class; {@code null} for instruments not classified
 *                   yet
 * @param cost       cost of the positions of the class, in cents
 * @param weight     share of the portfolio cost, between 0 and 1
 */
public record AssetClassWeight(AssetClass assetClass, BigDecimal cost, BigDecimal weight) {}
