package it.helper730.engine.rules;

import it.helper730.domain.AssetClass;
import it.helper730.domain.CostMethod;
import it.helper730.domain.IncomeCategory;
import it.helper730.domain.Instrument;
import it.helper730.domain.Msg;
import it.helper730.domain.OperationType;
import it.helper730.domain.exceptions.InvalidDataException;
import java.math.BigDecimal;

/**
 * Tax Rules in force since 1 July 2014 (DL 66/2014): 26% substitute tax; income
 * from white-listed government bonds counted at 48.08%, so that its effective
 * rate is 12.5%.
 */
public final class CurrentRegime implements TaxRuleSet {

    /** 26% tax rate. */
    public static final BigDecimal RATE = new BigDecimal("0.26");

    /** Share of government bond income that is taxable: 12.5 / 26 = 48.08%. */
    public static final BigDecimal GOVERNMENT_BOND_WEIGHT = new BigDecimal("0.4808");

    /**
     * Losses can be used until the fourth following year.
     */
    public static final int LOSS_CARRY_FORWARD_YEARS = 4;

    @Override
    public BigDecimal rate() {
        return RATE;
    }

    @Override
    public CostMethod costMethod(Instrument instrument) {
        return switch (requireClass(instrument)) {
            // fund units: gains are capital income, computed on the weighted average cost
            // without fees
            case UCITS_ETF, NON_UCITS_ETF -> CostMethod.WEIGHTED_AVERAGE;
            case STOCK, BOND, GOVERNMENT_BOND_WL, GOVERNMENT_BOND_NON_WL, ETC_ETN -> CostMethod.LIFO;
        };
    }

    /**
     * Returns the treatment for the disposal of an instrument.
     *
     * @param instrument the instrument being disposed of
     *
     * @param gain       {@code true} if the disposal results in a gain,
     *                   {@code false} otherwise
     *
     * @return the treatment for the disposal
     */
    @Override
    public Treatment disposalTreatment(Instrument instrument, boolean gain) {
        return switch (requireClass(instrument)) {
            case STOCK, BOND, GOVERNMENT_BOND_NON_WL, ETC_ETN -> Treatment.full(IncomeCategory.MISCELLANEOUS);
            case GOVERNMENT_BOND_WL -> new Treatment(IncomeCategory.MISCELLANEOUS, GOVERNMENT_BOND_WEIGHT);
            // UCITS fund units: a gain is capital income (art. 44 TUIR), a loss is
            // miscellaneous income
            case UCITS_ETF ->
                gain
                        ? new Treatment(IncomeCategory.CAPITAL, ucitsWeight(instrument))
                        : Treatment.full(IncomeCategory.MISCELLANEOUS);
            case NON_UCITS_ETF -> Treatment.excluded();
        };
    }

    /**
     * Returns the treatment for income from an instrument.
     *
     * @param instrument the instrument generating income
     * @param type       the type of income
     * @return the treatment for the income
     */
    @Override
    public Treatment incomeTreatment(Instrument instrument, OperationType type) {
        return switch (requireClass(instrument)) {
            case STOCK, BOND, GOVERNMENT_BOND_NON_WL -> Treatment.full(IncomeCategory.CAPITAL);
            case GOVERNMENT_BOND_WL -> new Treatment(IncomeCategory.CAPITAL, GOVERNMENT_BOND_WEIGHT);
            case UCITS_ETF -> new Treatment(IncomeCategory.CAPITAL, ucitsWeight(instrument));
            case NON_UCITS_ETF -> Treatment.excluded();
            case ETC_ETN ->
                throw new InvalidDataException(Msg.of("error.income_not_expected", instrument.ticker(), type.name()));
        };
    }

    @Override
    public int lossCarryForwardYears() {
        return LOSS_CARRY_FORWARD_YEARS;
    }

    /** Payments below 12 euro are not due. */
    public static final BigDecimal PAYMENT_THRESHOLD = new BigDecimal("12.00");

    @Override
    public BigDecimal paymentThreshold() {
        return PAYMENT_THRESHOLD;
    }

    /**
     * Returns the taxable share of income from a UCITS fund, based on the share of
     * its assets invested in government bonds.
     *
     * Taxable share of income from a UCITS fund: the part invested in government
     * bonds counts at 48.08%, the rest in full.
     * With 40% in government bonds: 1 − 0.40 * (1 − 0.4808) = 0.79232.
     */
    private static BigDecimal ucitsWeight(Instrument instrument) {
        BigDecimal share = instrument.governmentBondShare();
        if (share == null || share.signum() == 0) {
            return BigDecimal.ONE;
        }
        return BigDecimal.ONE.subtract(share.multiply(BigDecimal.ONE.subtract(GOVERNMENT_BOND_WEIGHT)));
    }

    /**
     * Returns the asset class of an instrument, throwing an exception if it is not
     * classified.
     *
     * @param instrument
     * @return the asset class of the instrument
     * @throws IllegalArgumentException if the instrument is not classified
     */
    private static AssetClass requireClass(Instrument instrument) {
        if (!instrument.isClassified()) {
            throw new IllegalArgumentException("Instrument not classified: " + instrument.ticker());
        }
        return instrument.assetClass();
    }
}
