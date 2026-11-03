package it.helper730.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Rounding rules for amounts.
 */
public final class Money {

    /** Euro cents: the scale of every amount shown to the user. */
    public static final int SCALE = 2;

    /**
     * Scale of intermediate results (shares of a lot cost, of a sale amount):
     * rounded to cents only at the end.
     */
    public static final int CALC_SCALE = 10;

    /** Commercial rounding: half a cent rounds up. */
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private Money() {}

    /** Rounds an amount to cents. */
    public static BigDecimal round(BigDecimal amount) {
        return amount.setScale(SCALE, ROUNDING);
    }

    /**
     * Returns {@code amount * part / whole} at {@link #CALC_SCALE}: the share of an
     * amount for part of a quantity.
     */
    public static BigDecimal share(BigDecimal amount, BigDecimal part, BigDecimal whole) {
        return amount.multiply(part).divide(whole, CALC_SCALE, ROUNDING);
    }
}
