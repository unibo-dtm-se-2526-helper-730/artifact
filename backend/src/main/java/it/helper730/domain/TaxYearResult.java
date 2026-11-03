package it.helper730.domain;

import it.helper730.domain.zainetto.ZainettoYear;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Result of the tax calculation for one year.
 *
 * @param year                 tax year
 * @param events               taxable events of the year, in chronological
 *                             order, all categories
 * @param zainetto             the Zainetto in the year
 * @param miscellaneousTaxable miscellaneous income subject to tax, after the
 *                             Zainetto
 * @param capitalTaxable       capital income subject to tax
 * @param rate                 substitute tax rate
 * @param miscellaneousTax     tax on miscellaneous income, in cents
 * @param capitalTax           tax on capital income, in cents
 * @param paymentThreshold     total tax below which nothing is paid
 * @param economicResult       money actually gained or lost in the year, fees
 *                             included
 */
public record TaxYearResult(
        int year,
        List<TaxableEvent> events,
        ZainettoYear zainetto,
        BigDecimal miscellaneousTaxable,
        BigDecimal capitalTaxable,
        BigDecimal rate,
        BigDecimal miscellaneousTax,
        BigDecimal capitalTax,
        BigDecimal paymentThreshold,
        BigDecimal economicResult) {

    public TaxYearResult {
        events = List.copyOf(events);
        Objects.requireNonNull(zainetto, "zainetto");
    }

    /** Total substitute tax of the year. */
    public BigDecimal totalTax() {
        return miscellaneousTax.add(capitalTax);
    }

    /** Whether the total is below the threshold, so nothing is paid. */
    public boolean belowThreshold() {
        return totalTax().compareTo(paymentThreshold) < 0;
    }

    /** Tax actually due: the total, or zero below the threshold. */
    public BigDecimal payableTax() {
        return belowThreshold() ? Money.round(BigDecimal.ZERO) : totalTax();
    }

    /**
     * Events outside the substitute tax (e.g. non-UCITS ETFs), to be shown as
     * warnings.
     */
    public List<TaxableEvent> excludedEvents() {
        return events.stream()
                .filter(e -> e.category() == IncomeCategory.EXCLUDED)
                .toList();
    }
}
