package it.helper730.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A user-facing message as a translatable key plus parameters.
 *
 * @param code   message key in {@code messages_*.properties}, e.g.
 *               {@code error.invalid_isin}
 * @param params values substituted into the message, in order
 */
public record Msg(String code, List<Object> params) {

    public Msg {
        Objects.requireNonNull(code, "code");
        params = List.copyOf(params);
    }

    /** Creates a message */
    public static Msg of(String code, Object... params) {
        return new Msg(code, Arrays.asList(params));
    }
}
