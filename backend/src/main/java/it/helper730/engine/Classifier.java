package it.helper730.engine;

import it.helper730.domain.Disposal;
import it.helper730.domain.EventType;
import it.helper730.domain.Instrument;
import it.helper730.domain.Money;
import it.helper730.domain.OperationType;
import it.helper730.domain.TaxableEvent;
import it.helper730.domain.Transaction;
import it.helper730.domain.UnclassifiedInstrumentsException;
import it.helper730.engine.rules.RuleSetFactory;
import it.helper730.engine.rules.TaxRuleSet;
import it.helper730.engine.rules.Treatment;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Turns sales and income payments into taxable events, applying the rules of
 * each tax year.
 */
public final class Classifier {

    private final RuleSetFactory rules;

    public Classifier(RuleSetFactory rules) {
        this.rules = Objects.requireNonNull(rules, "rules");
    }

    /**
     * Classifies all the given sales and income payments, in chronological order.
     *
     * @param disposals   sales matched to their lots
     * @param incomes     dividends and coupons; other operations are ignored
     * @param instruments instruments by ticker
     * @throws UnclassifiedInstrumentsException listing every ticker involved that
     *                                          is missing or not
     *                                          classified, so the user can fix them
     *                                          all at once
     */
    public List<TaxableEvent> classifyAll(
            Collection<Disposal> disposals, Collection<Transaction> incomes, Map<String, Instrument> instruments) {
        List<Transaction> incomePayments =
                incomes.stream().filter(t -> t.type().isIncome()).toList();

        TreeSet<String> unclassified = new TreeSet<>();
        disposals.forEach(d -> checkClassified(d.sale().ticker(), instruments, unclassified));
        incomePayments.forEach(t -> checkClassified(t.ticker(), instruments, unclassified));
        if (!unclassified.isEmpty()) {
            throw new UnclassifiedInstrumentsException(unclassified);
        }

        List<TaxableEvent> events = new ArrayList<>();
        disposals.forEach(d -> events.add(classify(d, instruments.get(d.sale().ticker()))));
        incomePayments.forEach(t -> events.add(classify(t, instruments.get(t.ticker()))));
        events.sort(Comparator.comparing(TaxableEvent::source, LotBook.CHRONOLOGICAL));
        return List.copyOf(events);
    }

    /** Classifies a sale: a gain or a loss, with the lots it consumed. */
    public TaxableEvent classify(Disposal disposal, Instrument instrument) {
        TaxRuleSet ruleSet = rules.forYear(disposal.sale().taxYear());
        BigDecimal gain = disposal.gain();
        boolean isGain = gain.signum() >= 0;
        Treatment treatment = ruleSet.disposalTreatment(instrument, isGain);
        return new TaxableEvent(
                disposal.sale(),
                isGain ? EventType.CAPITAL_GAIN : EventType.CAPITAL_LOSS,
                treatment.category(),
                gain,
                treatment.weight(),
                Money.round(BigDecimal.ZERO),
                disposal.consumptions());
    }

    /**
     * Classifies a dividend or a coupon. The amount received is net of the tax
     * withheld abroad;
     * the tax is due on the gross amount, so it is grossed up with the instrument's
     * withholding rate.
     */
    public TaxableEvent classify(Transaction income, Instrument instrument) {
        if (!income.type().isIncome()) {
            throw new IllegalArgumentException("Not an income payment: " + income.type());
        }
        TaxRuleSet ruleSet = rules.forYear(income.taxYear());
        Treatment treatment = ruleSet.incomeTreatment(instrument, income.type());

        BigDecimal net = income.totalAmount();
        BigDecimal withholding = instrument.foreignWithholdingRate();
        BigDecimal gross = withholding == null || withholding.signum() == 0
                ? Money.round(net)
                : net.divide(BigDecimal.ONE.subtract(withholding), Money.SCALE, Money.ROUNDING);

        return new TaxableEvent(
                income,
                income.type() == OperationType.DIVIDEND ? EventType.DIVIDEND : EventType.COUPON,
                treatment.category(),
                gross,
                treatment.weight(),
                gross.subtract(Money.round(net)),
                List.of());
    }

    /**
     * Checks if an instrument is classified.
     *
     * @param ticker      the instrument's ticker
     * @param instruments map of instruments by ticker
     * @param out         collection to add the ticker to if it is not classified
     */
    private static void checkClassified(String ticker, Map<String, Instrument> instruments, Collection<String> out) {
        Instrument instrument = instruments.get(ticker);
        if (instrument == null || !instrument.isClassified()) {
            out.add(ticker);
        }
    }
}
