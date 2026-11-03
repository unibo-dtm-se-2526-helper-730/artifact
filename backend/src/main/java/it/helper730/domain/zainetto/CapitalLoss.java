package it.helper730.domain.zainetto;

import it.helper730.domain.Money;
import it.helper730.domain.Msg;
import it.helper730.domain.exceptions.InvalidDataException;
import java.math.BigDecimal;

/**
 * A net loss of miscellaneous income (minusvalenza) kept in the Zainetto: it
 * can offset
 * miscellaneous gains of the following years, until it expires.
 *
 * @param year           year in which the loss was realized
 * @param amount         original deductible amount, positive, in cents
 * @param remaining      part not used yet, between zero and {@code amount}
 * @param lastUsableYear last year in which it can be used (31 December of that
 *                       year)
 */
public record CapitalLoss(int year, BigDecimal amount, BigDecimal remaining, int lastUsableYear) {

    public CapitalLoss {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidDataException(Msg.of("error.loss_not_positive", year));
        }
        if (remaining == null || remaining.signum() < 0 || remaining.compareTo(amount) > 0) {
            throw new IllegalArgumentException("Remaining must be between 0 and the amount: " + remaining);
        }
        if (lastUsableYear <= year) {
            throw new IllegalArgumentException("A loss is usable only in later years: " + lastUsableYear);
        }
    }

    /**
     * A loss realized in {@code year}, usable in the {@code carryForwardYears}
     * following years.
     */
    public static CapitalLoss realized(int year, BigDecimal amount, int carryForwardYears) {
        BigDecimal cents = Money.round(amount);
        return new CapitalLoss(year, cents, cents, year + carryForwardYears);
    }

    /** The same loss after using {@code used} euro of it. */
    public CapitalLoss use(BigDecimal used) {
        if (used.signum() <= 0 || used.compareTo(remaining) > 0) {
            throw new IllegalArgumentException("Used amount must be between 0 and the remaining: " + used);
        }
        return new CapitalLoss(year, amount, remaining.subtract(used), lastUsableYear);
    }
}
