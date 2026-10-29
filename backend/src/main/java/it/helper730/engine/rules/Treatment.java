package it.helper730.engine.rules;

import it.helper730.domain.IncomeCategory;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * How an amount is taxed: its income category and the share of it that is
 * taxable.
 *
 * @param category income category
 * @param weight   taxable share, between 0 and 1
 */
public record Treatment(IncomeCategory category, BigDecimal weight) {

    public Treatment {
        Objects.requireNonNull(category, "category");
        if (weight == null || weight.signum() < 0 || weight.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Weight must be between 0 and 1: " + weight);
        }
    }

    /** Fully taxable amount of the given category. */
    public static Treatment full(IncomeCategory category) {
        return new Treatment(category, BigDecimal.ONE);
    }

    /** Amount outside the substitute tax. */
    public static Treatment excluded() {
        return new Treatment(IncomeCategory.EXCLUDED, BigDecimal.ZERO);
    }
}
