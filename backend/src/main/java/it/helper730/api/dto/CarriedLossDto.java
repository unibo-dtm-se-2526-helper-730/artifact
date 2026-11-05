package it.helper730.api.dto;

/**
 * CarriedLossDto.
 *
 * Amount as a string, like every BigDecimal in the API.
 */
public record CarriedLossDto(int lossYear, String amount) {}
