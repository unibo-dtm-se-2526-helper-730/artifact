package it.helper730.domain.exceptions;

import it.helper730.domain.Msg;

/**
 * Raised for amounts in a currency other than EUR, which the project does not
 * support.
 */
public class UnsupportedCurrencyException extends DomainException {

    public UnsupportedCurrencyException(String currency, int fileRow) {
        super(Msg.of("error.unsupported_currency", currency, fileRow));
    }

    @Override
    public String code() {
        return "UNSUPPORTED_CURRENCY";
    }
}
