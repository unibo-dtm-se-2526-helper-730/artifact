package it.helper730.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The part of a sale matched to one lot: the line of the audit trail that
 * explains a gain.
 *
 * @param buyTransactionId purchase that created the lot
 * @param acquiredAt       purchase instant
 * @param quantity         shares taken from the lot
 * @param cost             tax cost of those shares
 * @param proceeds         part of the sale amount attributed to those shares
 */
public record LotConsumption(
        String buyTransactionId, Instant acquiredAt, BigDecimal quantity, BigDecimal cost, BigDecimal proceeds) {

    /** Gain (positive) or loss (negative) on these shares, not rounded. */
    public BigDecimal gain() {
        return proceeds.subtract(cost);
    }
}
