package it.helper730.domain;

/**
 * Category of income under Italian tax law, which decides what can be offset
 * against what.
 */
public enum IncomeCategory {
    /**
     * Redditi diversi (art. 67 TUIR): gains and losses can offset each other and
     * losses go into the Zainetto.
     */
    MISCELLANEOUS,
    /**
     * Redditi di capitale (art. 44 TUIR): taxed on the gross amount, never offset
     * by losses.
     */
    CAPITAL,
    /**
     * Outside the substitute tax (e.g. non-UCITS ETFs, taxed under ordinary IRPEF):
     * flagged, not computed.
     */
    EXCLUDED
}
