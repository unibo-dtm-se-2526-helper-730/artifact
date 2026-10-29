package it.helper730.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * A sale matched to the losts it consumed.
 */
public record Disposal(Transaction sale, List<LotConsumption> consumptions) {

    public Disposal {
        Objects.requireNonNull(sale, "sale");
        consumptions = List.copyOf(consumptions);
    }

    /** Amount received from the sale */
    public BigDecimal proceeds() {
        return sale.totalAmount();
    }

    /** Total tax cost of the shared sold */
    public BigDecimal cost() {
        return consumptions.stream().map(LotConsumption::cost).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal gain() {
        return Money.round(proceeds().subtract(cost()));
    }
}
