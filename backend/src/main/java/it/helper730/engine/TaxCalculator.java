package it.helper730.engine;

import it.helper730.domain.Disposal;
import it.helper730.domain.IncomeCategory;
import it.helper730.domain.Instrument;
import it.helper730.domain.Money;
import it.helper730.domain.TaxYearResult;
import it.helper730.domain.TaxableEvent;
import it.helper730.domain.Transaction;
import it.helper730.domain.zainetto.ZainettoYear;
import it.helper730.engine.exceptions.UnclassifiedInstrumentsException;
import it.helper730.engine.rules.RuleSetFactory;
import it.helper730.engine.rules.TaxRuleSet;
import it.helper730.engine.zainetto.CompensationStrategy;
import it.helper730.engine.zainetto.Zainetto;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Computes the substitute tax of a year: replays the lots, classifies sales and
 * income, runs the Zainetto and applies rate and payment threshold. Pure
 * function of its inputs.
 */
public final class TaxCalculator {

    private final RuleSetFactory rules;
    private final Classifier classifier;
    private final Zainetto zainetto;

    public TaxCalculator(RuleSetFactory rules, CompensationStrategy strategy) {
        this.rules = Objects.requireNonNull(rules, "rules");
        this.classifier = new Classifier(rules);
        this.zainetto = new Zainetto(rules, strategy);
    }

    /**
     * Computes the given tax year.
     *
     * @param year          tax year
     * @param transactions  all stored operations; those after {@code year} are
     *                      ignored
     * @param instruments   instruments by ticker
     * @param carriedLosses losses entered by the user, as year → amount
     * @throws UnclassifiedInstrumentsException listing every instrument with
     *                                          tax-relevant operations
     *                                          up to {@code year} that is not
     *                                          classified
     */
    public TaxYearResult calculate(
            int year,
            Collection<Transaction> transactions,
            Map<String, Instrument> instruments,
            Map<Integer, BigDecimal> carriedLosses) {
        TaxRuleSet ruleSet = rules.forYear(year);
        List<Transaction> upToYear =
                transactions.stream().filter(t -> t.taxYear() <= year).toList();
        requireClassified(upToYear, instruments);

        LotBook book = LotBook.replay(upToYear, CostMethodPolicy.fromRules(rules, instruments));
        List<TaxableEvent> allEvents = classifier.classifyAll(book.disposals(), upToYear, instruments);
        List<TaxableEvent> events =
                allEvents.stream().filter(e -> e.taxYear() == year).toList();

        ZainettoYear zainettoYear = zainetto.history(allEvents, carriedLosses, year).stream()
                .filter(z -> z.year() == year)
                .findFirst()
                .orElse(ZainettoYear.empty(year));

        BigDecimal miscellaneousTaxable = Money.round(zainettoYear.taxableAmount());
        BigDecimal capitalTaxable = sumTaxable(events, IncomeCategory.CAPITAL);
        BigDecimal rate = ruleSet.rate();

        return new TaxYearResult(
                year,
                events,
                zainettoYear,
                miscellaneousTaxable,
                capitalTaxable,
                rate,
                Money.round(miscellaneousTaxable.multiply(rate)),
                Money.round(capitalTaxable.multiply(rate)),
                ruleSet.paymentThreshold(),
                economicResult(year, book.disposals(), upToYear));
    }

    private static void requireClassified(Collection<Transaction> transactions, Map<String, Instrument> instruments) {
        TreeSet<String> unclassified = new TreeSet<>();
        for (Transaction t : transactions) {
            if (t.type().isTaxRelevant()) {
                Instrument instrument = instruments.get(t.ticker());
                if (instrument == null || !instrument.isClassified()) {
                    unclassified.add(t.ticker());
                }
            }
        }
        if (!unclassified.isEmpty()) {
            throw new UnclassifiedInstrumentsException(unclassified);
        }
    }

    private static BigDecimal sumTaxable(Collection<TaxableEvent> events, IncomeCategory category) {
        return events.stream()
                .filter(e -> e.category() == category)
                .map(TaxableEvent::taxableAmount)
                .reduce(Money.round(BigDecimal.ZERO), BigDecimal::add);
    }

    /**
     * Cash gained or lost in the year: results of sales with fees, plus income
     * actually received.
     */
    private static BigDecimal economicResult(int year, List<Disposal> disposals, List<Transaction> transactions) {
        BigDecimal sales = disposals.stream()
                .filter(d -> d.sale().taxYear() == year)
                .map(Disposal::cashResult)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal income = transactions.stream()
                .filter(t -> t.type().isIncome() && t.taxYear() == year)
                .map(Transaction::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Money.round(sales.add(income));
    }
}
