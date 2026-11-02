package it.helper730.engine.rules.exceptions;

import it.helper730.domain.Msg;
import it.helper730.domain.exceptions.DomainException;

/** Raised for a tax year whose rules are not implemented. */
public class UnsupportedTaxYearException extends DomainException {

    public UnsupportedTaxYearException(int year) {
        super(Msg.of("error.unsupported_tax_year", year));
    }

    @Override
    public String code() {
        return "UNSUPPORTED_TAX_YEAR";
    }
}
