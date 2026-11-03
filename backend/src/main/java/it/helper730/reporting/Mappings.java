package it.helper730.reporting;

import java.util.Map;
import java.util.Optional;

/**
 * Where each value goes in the Modello 730, by tax year. The layout is the one
 * of the 730/2026 (tax year 2025); when the Agenzia delle Entrate changes righi
 * or codes, a new layout is added here for the years it applies to.
 */
public final class Mappings {

    /**
     * Righi and codes of one version of the form.
     *
     * @param proceedsRigo        quadro T, total proceeds of sales (miscellaneous
     *                            income)
     * @param costRigo            quadro T, total cost of the shares sold
     * @param carriedLossesRigo   quadro T, losses of the four previous years, one
     *                            column per year
     * @param capitalIncomeRigo   quadro M, foreign capital income taxed by the
     *                            substitute tax
     * @param foreignAssetsPrefix quadro W, prefix of the righi: W1, W2, ...
     */
    public record Layout(
            String proceedsRigo,
            String costRigo,
            String carriedLossesRigo,
            String capitalIncomeRigo,
            String foreignAssetsPrefix) {}

    /**
     * Righi of the 730/2026, assumed for later years until a new form is published.
     */
    static final Layout MODEL_730_2026 = new Layout("T11 col. 1", "T11 col. 2", "T102", "M31", "W");

    /** Quadro M, column 1: dividends from foreign shares. */
    public static final String CODE_FOREIGN_DIVIDENDS = "H";

    /** Quadro M, column 1: income from harmonized funds (UCITS ETFs). */
    public static final String CODE_FUND_INCOME = "B";

    /** Quadro M, column 1: interest from foreign bonds. */
    public static final String CODE_BOND_INTEREST = "A";

    /** Quadro W, column 1: full ownership. */
    public static final String W_OWNERSHIP = "1";

    /** Quadro W, column 3: securities account. */
    public static final String W_SECURITIES = "20";

    /** Quadro W, column 6: market value. */
    public static final String W_MARKET_VALUE = "1";

    /** Codes of foreign states in the declaration, by ISO 3166 alpha-2 code. */
    private static final Map<String, String> STATE_CODES = Map.ofEntries(
            Map.entry("AT", "008"),
            Map.entry("BE", "009"),
            Map.entry("CH", "071"),
            Map.entry("DE", "094"),
            Map.entry("ES", "067"),
            Map.entry("FR", "029"),
            Map.entry("GB", "031"),
            Map.entry("IE", "040"),
            Map.entry("LT", "259"),
            Map.entry("LU", "092"),
            Map.entry("NL", "050"),
            Map.entry("US", "069"));

    private Mappings() {}

    /** Layout to use for a tax year. */
    public static Layout forYear(int taxYear) {
        return MODEL_730_2026;
    }

    /** Name of the form for a tax year: the 730 is filed the year after. */
    public static String modelName(int taxYear) {
        return "730/" + (taxYear + 1);
    }

    /**
     * Code of a foreign state in the declaration, from its ISO 3166 alpha-2 code.
     */
    public static Optional<String> stateCode(String isoCountry) {
        return Optional.ofNullable(isoCountry).map(STATE_CODES::get);
    }
}
