package it.helper730.domain;

/**
 * Asset Class of an instrument.
 */
public enum AssetClass {
    /**
     * Stock.
     */
    STOCK,
    /**
     * Bond.
     */
    BOND,
    /**
     * Government bond of a white-listed state.
     */
    GOVERNMENT_BOND_WL,
    /**
     * Government bond of a non-white-listed state.
     */
    GOVERNMENT_BOND_NON_WL,
    /**
     * UCITS Exchange Traded Fund.
     */
    UCITS_ETF,
    /**
     * Non UCITS Exchange Traded Fund.
     */
    NON_UCITS_ETF,
    /**
     * Exchange Traded Commodity / Exchange Traded Note.
     */
    ETC_ETN;

    /**
     * Whether a government bond share can be set.
     */
    public boolean allowsGovernmentBondShare() {
        return this == UCITS_ETF;
    }

    /**
     * Whether a foreign withholding rate can be set.
     */
    public boolean allowsForeignWithholding() {
        return this == STOCK || this == BOND || this == GOVERNMENT_BOND_WL || this == GOVERNMENT_BOND_NON_WL;
    }
}
