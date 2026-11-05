package it.helper730.api.dto;

import java.math.BigDecimal;

/**
 * CarriedLossDtos.
 *
 * Bodies of the carried loss endpoints.
 */
public final class CarriedLossDtos {

    private CarriedLossDtos() {}

    /** Response: amount as a string, like every BigDecimal in the API. */
    public record CarriedLossDto(int lossYear, String amount) {}

    /** Request: Jackson rejects a non-numeric amount with a 400. */
    public record CarriedLossRequest(int lossYear, BigDecimal amount) {}
}
