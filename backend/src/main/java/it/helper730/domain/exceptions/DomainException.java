package it.helper730.domain.exceptions;

import java.util.Objects;

import it.helper730.domain.Msg;

/**
 * Base class of the errors raised by the domain and the engine. Each subclass
 * has a stable machine-readable {@link #code()}, which the API maps to an HTTP
 * status, and carries a translatable {@link Msg} for the user.
 */
public abstract class DomainException extends RuntimeException {

    private final transient Msg msg;

    protected DomainException(Msg msg) {
        super(Objects.requireNonNull(msg, "msg").code());
        this.msg = msg;
    }

    /** Stable error code exposed by the API, e.g. {@code UNSUPPORTED_CURRENCY}. */
    public abstract String code();

    /** Translatable message for the user. */
    public Msg msg() {
        return msg;
    }
}
