package it.helper730.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Shares of one instrument bought with a single purchase and not yet sold.
 *
 * @param buyTransactionId id of the purchase that created the lot
 * @param ticker           instrument ticker
 * @param acquiredAt       purchase instant
 * @param quantity         shares still open
 * @param cost             tax cost of the open shares: what was actually paid,
 *                         fees included
 */
public record Lot(String buyTransactionId, String ticker, Instant acquiredAt, BigDecimal quantity, BigDecimal cost) {

    public Lot {
        Objects.requireNonNull(buyTransactionId, "buyTransactionId");
        Objects.requireNonNull(ticker, "ticker");
        Objects.requireNonNull(acquiredAt, "acquiredAt");
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException("Lot quantity must be positive: " + quantity);
        }
        if (cost == null || cost.signum() < 0) {
            throw new IllegalArgumentException("Lot cost must not be negative: " + cost);
        }
    }

    /** Opens a lot from a purchase: its cost is the total amount paid. */
    public static Lot of(Transaction buy) {
        if (buy.type() != OperationType.BUY) {
            throw new IllegalArgumentException("A lot is opened only by a purchase, not by " + buy.type());
        }
        return new Lot(buy.id(), buy.ticker(), buy.executedAt(), buy.quantity(), buy.totalAmount());
    }

    /**
     * Cost of {@code part} shares of this lot. Taking the whole lot returns exactly
     * {@link #cost()}, so the costs of successive partial sales always add up to
     * the original cost.
     */
    public BigDecimal costOf(BigDecimal part) {
        return part.compareTo(quantity) == 0 ? cost : Money.share(cost, part, quantity);
    }
}
