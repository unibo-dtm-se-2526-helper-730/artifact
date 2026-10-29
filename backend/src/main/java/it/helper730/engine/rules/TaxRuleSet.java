package it.helper730.engine.rules;

import it.helper730.domain.Instrument;
import it.helper730.domain.OperationType;
import java.math.BigDecimal;

/** The tax rules in force in a given period. One implementation per regime. */
public interface TaxRuleSet {

    /** Rate of the substitute tax on the taxable amount. */
    BigDecimal rate();

    /**
     * Treatment of the result of a sale.
     *
     * @param instrument a classified instrument
     * @param gain       {@code true} for a gain (or zero result), {@code false} for
     *                   a loss
     */
    Treatment disposalTreatment(Instrument instrument, boolean gain);

    /**
     * Treatment of a dividend or a coupon.
     *
     * @param instrument a classified instrument
     * @param type       {@link OperationType#DIVIDEND} or
     *                   {@link OperationType#COUPON}
     */
    Treatment incomeTreatment(Instrument instrument, OperationType type);
}
