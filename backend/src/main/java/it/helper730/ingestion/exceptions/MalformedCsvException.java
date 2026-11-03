package it.helper730.ingestion.exceptions;

import it.helper730.domain.Msg;
import it.helper730.domain.exceptions.DomainException;

/** Raised when a file is not valid CSV, so no row can be read. */
public class MalformedCsvException extends DomainException {

    public MalformedCsvException(Msg msg) {
        super(msg);
    }

    @Override
    public String code() {
        return "MALFORMED_CSV";
    }
}
