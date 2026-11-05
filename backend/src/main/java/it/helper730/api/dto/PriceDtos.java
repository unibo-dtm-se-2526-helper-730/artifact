package it.helper730.api.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * PriceDtos.
 */
public final class PriceDtos {

    private PriceDtos() {}

    /**
     * PricesDto.
     *
     * Map keys are tickers: the snake_case naming strategy does not touch them.
     */
    public record PricesDto(int year, Map<String, String> prices) {}

    /**
     * PricesRequest.
     *
     * Map keys are tickers: the snake_case naming strategy does not touch them.
     */
    public record PricesRequest(Map<String, BigDecimal> prices) {}
}
