package it.helper730.engine;

import it.helper730.domain.CostMethod;
import it.helper730.domain.Instrument;
import it.helper730.domain.Transaction;
import it.helper730.engine.exceptions.UnclassifiedInstrumentsException;
import it.helper730.engine.rules.RuleSetFactory;
import java.util.List;
import java.util.Map;

/** Decides the cost method of each sale. */
@FunctionalInterface
public interface CostMethodPolicy {

    /** Cost method to apply to the given sale. */
    CostMethod forSale(Transaction sale);

    /** The same method for every sale. */
    static CostMethodPolicy always(CostMethod method) {
        return sale -> method;
    }

    /**
     * The method required by the tax rules of the sale's year for the instrument's
     * asset class.
     *
     * @throws UnclassifiedInstrumentsException when a sale involves an instrument
     *                                          not classified
     */
    static CostMethodPolicy fromRules(RuleSetFactory rules, Map<String, Instrument> instruments) {
        return sale -> {
            Instrument instrument = instruments.get(sale.ticker());
            if (instrument == null || !instrument.isClassified()) {
                throw new UnclassifiedInstrumentsException(List.of(sale.ticker()));
            }
            return rules.forYear(sale.taxYear()).costMethod(instrument);
        };
    }
}
