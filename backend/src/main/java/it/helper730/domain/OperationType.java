package it.helper730.domain;

/**
 * Kind of operation found in a broker export.
 */
public enum OperationType {
    /**
     * Buy operation.
     */
    BUY,
    /**
     * Sell operation.
     */
    SELL,
    /**
     * Dividend operation.
     */
    DIVIDEND,
    /**
     * Coupon operation.
     */
    COUPON,
    /**
     * Cash top-up operation.
     */
    CASH_TOP_UP,
    /**
     * Cash withdrawal operation.
     */
    CASH_WITHDRAWAL,
    /**
     * Custody fee operation.
     */
    CUSTODY_FEE;

    /** Whether the operation buys or sells shares. */
    public boolean isTrade() {
        return this == BUY || this == SELL;
    }

    /** Whether the operation is an income payment. */
    public boolean isIncome() {
        return this == DIVIDEND || this == COUPON;
    }

    /** Whether the operation has tax effects; the others are only stored. */
    public boolean isTaxRelevant() {
        return isTrade() || isIncome();
    }
}
