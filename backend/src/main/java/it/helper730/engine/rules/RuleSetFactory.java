package it.helper730.engine.rules;

import it.helper730.domain.UnsupportedTaxYearException;

/**
 * Chooses the tax rules of a tax year. A future change in the law becomes a new
 * {@link TaxRuleSet} selected here by year.
 */
public class RuleSetFactory {

    /**
     * First year fully under the current regime (26% tax rate from 1 July 2014).
     */
    public static final int FIRST_SUPPORTED_YEAR = 2015;

    private final TaxRuleSet current = new CurrentRegime();

    /** Rules of the given tax year. */
    public TaxRuleSet forYear(int year) {
        if (year < FIRST_SUPPORTED_YEAR) {
            throw new UnsupportedTaxYearException(year);
        }
        return current;
    }
}
