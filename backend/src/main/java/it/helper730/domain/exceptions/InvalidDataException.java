package it.helper730.domain.exceptions;

import it.helper730.domain.Msg;

/**
 * Raised when a value violates a domain rule (malformed ISIN, negative
 * quantity, ...).
 */
public class InvalidDataException extends DomainException {

    public InvalidDataException(Msg msg) {
        super(msg);
    }

    @Override
    public String code() {
        return "INVALID_DATA";
    }
}
