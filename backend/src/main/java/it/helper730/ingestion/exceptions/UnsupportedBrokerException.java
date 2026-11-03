package it.helper730.ingestion.exceptions;

import it.helper730.domain.Msg;
import it.helper730.domain.exceptions.DomainException;

/** Raised when no parser recognizes the header of a file. */
public class UnsupportedBrokerException extends DomainException {

    public UnsupportedBrokerException(String header) {
        super(Msg.of("error.unsupported_broker", header));
    }

    @Override
    public String code() {
        return "UNSUPPORTED_BROKER";
    }
}
