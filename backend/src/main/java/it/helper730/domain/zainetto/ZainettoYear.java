package it.helper730.domain.zainetto;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * The Zainetto in one tax year: what was available, what was used, what expired
 * and what is
 * carried to the next year. Amounts are taxable amounts (government bonds
 * already weighted).
 *
 * @param year          tax year
 * @param opening       losses usable in this year
 * @param expired       losses whose last usable year was the previous one, with
 *                      what was left of them
 * @param gains         miscellaneous gains of the year, positive
 * @param losses        miscellaneous losses of the year, positive
 * @param compensations losses of previous years used this year, in order of use
 * @param realizedLoss  net loss of this year added to the Zainetto, or
 *                      {@code null} if none
 * @param closing       losses carried to the next year
 */
public record ZainettoYear(
        int year,
        List<CapitalLoss> opening,
        List<CapitalLoss> expired,
        BigDecimal gains,
        BigDecimal losses,
        List<Compensation> compensations,
        CapitalLoss realizedLoss,
        List<CapitalLoss> closing) {

    public ZainettoYear {
        opening = List.copyOf(opening);
        expired = List.copyOf(expired);
        compensations = List.copyOf(compensations);
        closing = List.copyOf(closing);
    }

    /** Net loss of this year added to the Zainetto, if any. */
    public Optional<CapitalLoss> newLoss() {
        return Optional.ofNullable(realizedLoss);
    }

    /** Gains minus losses of the year, before using the Zainetto. */
    public BigDecimal net() {
        return gains.subtract(losses);
    }

    /** Total of losses of previous years used this year. */
    public BigDecimal used() {
        return compensations.stream().map(Compensation::used).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Miscellaneous income subject to tax: the positive net result minus the losses
     * used.
     */
    public BigDecimal taxableAmount() {
        return net().max(BigDecimal.ZERO).subtract(used());
    }

    /** Total available at the start of the year. */
    public BigDecimal openingTotal() {
        return totalRemaining(opening);
    }

    /** Total expired at the start of the year. */
    public BigDecimal expiredTotal() {
        return totalRemaining(expired);
    }

    /** Total carried to the next year. */
    public BigDecimal closingTotal() {
        return totalRemaining(closing);
    }

    private static BigDecimal totalRemaining(Collection<CapitalLoss> losses) {
        return losses.stream().map(CapitalLoss::remaining).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
