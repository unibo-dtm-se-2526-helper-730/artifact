package it.helper730.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Shares of one instrument bought with a single purchase and not yet sold. The
 * lot keeps both
 * costs, with and without fees, because the cost method of the sale decides
 * which one counts.
 *
 * @param buyTransactionId  id of the purchase that created the lot
 * @param ticker            instrument ticker
 * @param acquiredAt        purchase instant
 * @param quantity          shares still open, always positive
 * @param cost              cost of the open shares including fees: what was
 *                          actually paid
 * @param costExcludingFees cost of the open shares as quantity × price
 */
public record Lot(
        String buyTransactionId,
        String ticker,
        Instant acquiredAt,
        BigDecimal quantity,
        BigDecimal cost,
        BigDecimal costExcludingFees) {

    public Lot {
        Objects.requireNonNull(buyTransactionId, "buyTransactionId");
        Objects.requireNonNull(ticker, "ticker");
        Objects.requireNonNull(acquiredAt, "acquiredAt");
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException("Lot quantity must be positive: " + quantity);
        }
        if (cost == null || cost.signum() < 0 || costExcludingFees == null || costExcludingFees.signum() < 0) {
            throw new IllegalArgumentException("Lot costs must not be negative: " + cost + ", " + costExcludingFees);
        }
    }

    /** Opens a lot from a purchase. */
    public static Lot of(Transaction buy) {
        if (buy.type() != OperationType.BUY) {
            throw new IllegalArgumentException("A lot is opened only by a purchase, not by " + buy.type());
        }
        return new Lot(
                buy.id(),
                buy.ticker(),
                buy.executedAt(),
                buy.quantity(),
                buy.totalAmount(),
                buy.quantity().multiply(buy.pricePerShare()));
    }

    /**
     * Cost of {@code part} shares of this lot. Taking the whole lot returns exactly
     * the full cost,
     * so the costs of successive partial sales always add up to the original cost.
     *
     * @param includeFees whether to use the cost with fees or the one without
     */
    public BigDecimal costOf(BigDecimal part, boolean includeFees) {
        BigDecimal total = includeFees ? cost : costExcludingFees;
        return part.compareTo(quantity) == 0 ? total : Money.share(total, part, quantity);
    }

    /**
     * What is left of this lot after selling {@code part} shares, less than the
     * whole lot.
     */
    public Lot remainderAfter(BigDecimal part) {
        if (part.signum() <= 0 || part.compareTo(quantity) >= 0) {
            throw new IllegalArgumentException("Part must be between 0 and the lot quantity: " + part);
        }
        return new Lot(
                buyTransactionId,
                ticker,
                acquiredAt,
                quantity.subtract(part),
                cost.subtract(costOf(part, true)),
                costExcludingFees.subtract(costOf(part, false)));
    }
}
