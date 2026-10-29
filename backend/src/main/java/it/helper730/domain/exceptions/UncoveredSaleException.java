package it.helper730.domain;

import java.math.BigDecimal;

/**
 * Raised when a sale exceeds the shares held: the export is incomplete or
 * contains a corporate action.
 */
public class UncoveredSaleException extends DomainException {

    public UncoveredSaleException(String ticker, int fileRow, BigDecimal requested, BigDecimal available) {
        super(Msg.of("error.uncovered_sale", ticker, fileRow, requested.toPlainString(), available.toPlainString()));
    }

    @Override
    public String code() {
        return "UNCOVERED_SALE";
    }
}
