package it.helper730.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Rounding rules for amounts.
 */
public final class Money {

    /** Cents. */
    public static final int SCALE = 2;

    /** Commercial rounding: half a cent rounds up. */
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private Money() {}

    /** Rounds an amount to cents. */
    public static BigDecimal round(BigDecimal amount) {
        return amount.setScale(SCALE, ROUNDING);
    }
}
