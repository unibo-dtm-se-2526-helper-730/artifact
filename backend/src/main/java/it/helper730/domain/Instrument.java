package it.helper730.domain;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * A financial instrument identified by its ticker.
 *
 * @param ticker                 ticker as written in the broker export
 * @param isin                   ISIN code, optional
 * @param name                   full name, optional
 * @param assetClass             tax classification; {@code null} until the user
 *                               classifies the instrument
 * @param governmentBondShare    share of government bonds held by a UCITS ETF,
 *                               between 0 and 1
 * @param foreignWithholdingRate withholding applied abroad on dividends or
 *                               coupons, between 0 and 1
 */
public record Instrument(
        String ticker,
        String isin,
        String name,
        AssetClass assetClass,
        BigDecimal governmentBondShare,
        BigDecimal foreignWithholdingRate) {

    private static final Pattern ISIN_FORMAT = Pattern.compile("[A-Z]{2}[A-Z0-9]{9}[0-9]");

    /**
     * Constructor with validation.
     */
    public Instrument {
        if (ticker == null || ticker.isBlank()) {
            throw new InvalidDataException(Msg.of("error.ticker_required"));
        }
        if (isin != null && !isValidIsin(isin)) {
            throw new InvalidDataException(Msg.of("error.invalid_isin", isin));
        }

        requireRate(governmentBondShare, ticker);
        requireRate(foreignWithholdingRate, ticker);

        if (governmentBondShare != null && (assetClass == null || !assetClass.allowsGovernmentBondShare())) {
            throw new InvalidDataException(Msg.of("error.government_bond_share_not_allowed", ticker));
        }
        if (foreignWithholdingRate != null && (assetClass == null || !assetClass.allowsForeignWithholding())) {
            throw new InvalidDataException(Msg.of("error.withholding_not_allowed", ticker));
        }
    }

    /**
     * An instrument seen in an import but not yet classified by the user.
     */
    public static Instrument unclassified(String ticker) {
        return new Instrument(ticker, null, null, null, null, null);
    }

    /**
     * Whether the user has chosen an asset class; the tax calculation requires it.
     */
    public boolean isClassified() {
        return assetClass != null;
    }

    /**
     * Verifies that the rate is included in the close interval [0, 1].
     */
    private static void requireRate(BigDecimal rate, String ticker) {
        if (rate != null && (rate.signum() < 0 || rate.compareTo(BigDecimal.ONE) > 0)) {
            throw new InvalidDataException(Msg.of("error.rate_out_of_range", ticker, rate));
        }
    }

    /**
     * Checks format and check digit of an ISIN (ISO 6166): letters become numbers
     * (A=10 ... Z=35) and the resulting digit string must pass the Luhn algorithm.
     */
    static boolean isValidIsin(String isin) {
        if (!ISIN_FORMAT.matcher(isin).matches()) {
            return false;
        }
        StringBuilder digits = new StringBuilder();
        for (char c : isin.toCharArray()) {
            digits.append(Character.getNumericValue(c));
        }
        int sum = 0;
        boolean doubleIt = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (doubleIt) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }
            sum += d;
            doubleIt = !doubleIt;
        }
        return sum % 10 == 0;
    }
}
