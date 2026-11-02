package it.helper730.domain;

/**
 * Defines the calculation method used to determine the fiscal acquisition cost
 * and sale proceeds of financial instruments for Italian tax reporting
 * purposes.
 *
 * The fiscal treatment depends on the classification of the income produced
 * (e.g., capital income vs. miscellaneous income) under the Italian
 * Consolidated Income Tax Act (TUIR).
 */
public enum CostMethod {
    /**
     * Last-In, First-Out (LIFO) method.
     *
     * Applicable to capital gains and miscellaneous income ("redditi diversi")
     *
     * Under this rule:
     * Securities acquired most recently are deemed to be disposed of first.
     * Accessory transaction costs and fees are included in the fiscal acquisition
     * cost and deducted from the gross disposal proceeds.
     */
    LIFO(true),
    /**
     * Weighted average cost ("costo medio ponderato") method.
     *
     * Applicable to fund units, SICAVs, and income from capital ("redditi di
     * capitale"),
     *
     * Under this rule:
     * The fiscal cost is computed as the weighted average price of purchases
     * Transaction fees and commissions are not tax-deductible; therefore, both
     * fiscal cost and sale proceeds are strictly derived as
     * {@code quantity * unit_price}.
     */
    WEIGHTED_AVERAGE(false);

    /**
     * Whether transaction fees and commissions are recognized fiscally in cost and
     * proceeds.
     */
    private final boolean includesFees;

    /**
     * Constructs a {@code CostMethod} enumeration constant.
     *
     * @param includesFees {@code true} if transaction fees are recognized in the
     *                     fiscal cost and proceeds calculation; {@code false}
     *                     otherwise.
     */
    CostMethod(boolean includesFees) {
        this.includesFees = includesFees;
    }

    /**
     * Indicates whether transaction fees are factored into the acquisition cost
     * and disposal proceeds under this method.
     *
     * @return {@code true} if transaction fees adjust the cost basis and gross
     *         proceeds;
     *         {@code false} if calculations rely exclusively on execution prices.
     */
    public boolean includesFees() {
        return includesFees;
    }
}
