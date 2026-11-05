package it.helper730.api.dto;

import it.helper730.domain.AssetClass;
import java.math.BigDecimal;

/**
 * InstrumentDtos.
 */
public final class InstrumentDtos {

    private InstrumentDtos() {}

    /**
     * InstrumentDto.
     *
     * Response body for GET /instruments/{ticker}.
     */
    public record InstrumentDto(
            String ticker,
            String isin,
            String name,
            AssetClass assetClass,
            String governmentBondShare,
            String foreignWithholdingRate,
            boolean classified) {}

    /**
     * InstrumentRequest.
     *
     * Body of PUT /instruments/{ticker}.
     * The ticker comes from the path, not from the body.
     */
    public record InstrumentRequest(
            String isin,
            String name,
            AssetClass assetClass,
            BigDecimal governmentBondShare,
            BigDecimal foreignWithholdingRate) {}
}
