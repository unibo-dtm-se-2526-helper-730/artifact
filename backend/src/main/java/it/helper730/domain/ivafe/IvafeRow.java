package it.helper730.domain.ivafe;

import it.helper730.domain.Msg;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * IVAFE on one holding period: shares of one purchase held from {@code from} to
 * {@code to}.
 *
 * @param ticker       instrument ticker
 * @param quantity     shares held in the period
 * @param from         first day held in the year: 1 January or the purchase
 *                     date
 * @param to           last day held in the year: the sale date or 31 December
 * @param sold         whether the period ends with a sale
 * @param yearEndPrice price at 31 December, for shares still held; {@code null}
 *                     if missing or sold
 * @param initialValue value at the start of the period, in cents
 * @param finalValue   value at the end of the period (sale value or year-end
 *                     value); {@code null}
 *                     if the year-end price is missing
 * @param holdingDays  days held in the year, both ends included
 * @param ivafe        tax due, in cents; {@code null} if the year-end price is
 *                     missing
 * @param notes        explanations for the user
 */
public record IvafeRow(
        String ticker,
        BigDecimal quantity,
        LocalDate from,
        LocalDate to,
        boolean sold,
        BigDecimal yearEndPrice,
        BigDecimal initialValue,
        BigDecimal finalValue,
        int holdingDays,
        BigDecimal ivafe,
        List<Msg> notes) {

    public IvafeRow {
        Objects.requireNonNull(ticker, "ticker");
        notes = List.copyOf(notes);
    }

    /**
     * Whether the row could be computed: false when the year-end price is missing.
     */
    public boolean isComputed() {
        return ivafe != null;
    }
}
