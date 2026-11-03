package it.helper730.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * A sale matched to the lots it consumed.
 *
 * @param sale         the sale transaction
 * @param costMethod   method used to determine the cost of the shares sold
 * @param consumptions consumed lots, newest purchase first
 */
public record Disposal(Transaction sale, CostMethod costMethod, List<LotConsumption> consumptions) {

    public Disposal {
        Objects.requireNonNull(sale, "sale");
        Objects.requireNonNull(costMethod, "costMethod");
        consumptions = List.copyOf(consumptions);
    }

    /**
     * Proceeds recognized for tax: the amount received (LIFO) or quantity × price
     * (weighted average).
     */
    public BigDecimal proceeds() {
        return consumptions.stream().map(LotConsumption::proceeds).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Total tax cost of the shares sold. */
    public BigDecimal cost() {
        return consumptions.stream().map(LotConsumption::cost).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Gain (positive) or loss (negative), rounded to cents. */
    public BigDecimal gain() {
        return Money.round(proceeds().subtract(cost()));
    }

    /**
     * Money actually gained or lost: the amount received minus what was paid for
     * the shares sold, fees included. Differs from {@link #gain()} when the cost
     * method ignores fees.
     */
    public BigDecimal cashResult() {
        BigDecimal paid = consumptions.stream().map(LotConsumption::cashCost).reduce(BigDecimal.ZERO, BigDecimal::add);
        return Money.round(sale.totalAmount().subtract(paid));
    }
}
