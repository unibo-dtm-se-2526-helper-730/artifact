package it.helper730.api.dto;

import java.math.BigDecimal;

/**
 * Decimals.
 *
 * BigDecimal values travel as strings in the JSON, so that no client turns them
 * into floating point.
 */
public final class Decimals {

    private Decimals() {}

    /**
     * Plain notation without trailing zeros: 189.3300 (NUMERIC(19,4) in the
     * database) becomes "189.33".
     */
    public static String str(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }
}
