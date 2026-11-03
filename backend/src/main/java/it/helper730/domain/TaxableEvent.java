package it.helper730.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * A sale or an income payment with its tax treatment: the unit the tax
 * calculation works on.
 * The event keeps a reference to the source transaction, which is used to get
 * the ticker and the tax year (and for audit purposes).
 *
 * @param source       the sale, dividend or coupon transaction
 * @param type         what produced the event
 * @param category     income category, which decides offsetting rules
 * @param amount       gain (positive), loss (negative) or gross income, in
 *                     cents
 * @param weight       taxable share of the amount: 1, or less for government
 *                     bonds (48.08%)
 * @param foreignTax   tax withheld abroad on an income payment, in cents; zero
 *                     otherwise
 * @param consumptions lots consumed by a sale (the audit trail); empty for
 *                     income
 */
public record TaxableEvent(
        Transaction source,
        EventType type,
        IncomeCategory category,
        BigDecimal amount,
        BigDecimal weight,
        BigDecimal foreignTax,
        List<LotConsumption> consumptions) {

    public TaxableEvent {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(weight, "weight");
        Objects.requireNonNull(foreignTax, "foreignTax");
        consumptions = List.copyOf(consumptions);
    }

    /**
     * The part of the amount subject to tax ({@code amount * weight}), in cents;
     * signed like the amount.
     */
    public BigDecimal taxableAmount() {
        return Money.round(amount.multiply(weight));
    }

    /** Ticker of the instrument. */
    public String ticker() {
        return source.ticker();
    }

    /** Tax year in which the event happened. */
    public int taxYear() {
        return source.taxYear();
    }
}
