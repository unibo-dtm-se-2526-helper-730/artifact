package it.helper730.engine.zainetto;

import it.helper730.domain.IncomeCategory;
import it.helper730.domain.TaxableEvent;
import it.helper730.domain.zainetto.CapitalLoss;
import it.helper730.domain.zainetto.Compensation;
import it.helper730.domain.zainetto.ZainettoYear;
import it.helper730.engine.rules.RuleSetFactory;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Computes the Zainetto year by year. In each year the miscellaneous gains and
 * losses are netted;
 * a positive net is offset with the losses of previous years still usable, a
 * negative net becomes a new loss usable from the following year. Capital
 * income is never offset.
 */
public final class Zainetto {

    private final RuleSetFactory rules;
    private final CompensationStrategy strategy;

    public Zainetto(RuleSetFactory rules, CompensationStrategy strategy) {
        this.rules = Objects.requireNonNull(rules, "rules");
        this.strategy = Objects.requireNonNull(strategy, "strategy");
    }

    /**
     * Computes every year from the first one with events or losses to
     * {@code lastYear}, so that
     * losses expire even in years without operations.
     *
     * @param events        taxable events of all years; only miscellaneous ones
     *                      count
     * @param carriedLosses losses of other years entered by the user, as year →
     *                      amount
     * @param lastYear      last year to compute
     * @return one result per year, in order; empty if there is nothing up to
     *         {@code lastYear}
     */
    public List<ZainettoYear> history(
            Collection<TaxableEvent> events, Map<Integer, BigDecimal> carriedLosses, int lastYear) {
        Map<Integer, List<TaxableEvent>> eventsByYear = events.stream()
                .filter(e -> e.category() == IncomeCategory.MISCELLANEOUS)
                .collect(Collectors.groupingBy(TaxableEvent::taxYear));
        int firstYear = Stream.concat(eventsByYear.keySet().stream(), carriedLosses.keySet().stream())
                .min(Integer::compare)
                .orElse(lastYear + 1);

        List<ZainettoYear> years = new ArrayList<>();
        List<CapitalLoss> available = List.of();
        for (int year = firstYear; year <= lastYear; year++) {
            ZainettoYear result =
                    computeYear(year, available, eventsByYear.getOrDefault(year, List.of()), carriedLosses.get(year));
            years.add(result);
            available = result.closing();
        }
        return List.copyOf(years);
    }

    private ZainettoYear computeYear(
            int year, List<CapitalLoss> previousClosing, List<TaxableEvent> events, BigDecimal carriedLoss) {
        final int currentYear = year;
        List<CapitalLoss> expired = previousClosing.stream()
                .filter(l -> l.lastUsableYear() < currentYear)
                .toList();
        List<CapitalLoss> opening = previousClosing.stream()
                .filter(l -> l.lastUsableYear() >= currentYear)
                .toList();

        BigDecimal gains = BigDecimal.ZERO;
        BigDecimal losses = BigDecimal.ZERO;
        for (TaxableEvent event : events) {
            BigDecimal taxable = event.taxableAmount();
            if (taxable.signum() > 0) {
                gains = gains.add(taxable);
            } else {
                losses = losses.add(taxable.negate());
            }
        }
        BigDecimal net = gains.subtract(losses);

        List<CapitalLoss> remaining = new ArrayList<>(opening);
        List<Compensation> compensations = new ArrayList<>();
        BigDecimal toCover = net.max(BigDecimal.ZERO);
        for (CapitalLoss loss : strategy.order(opening)) {
            if (toCover.signum() == 0) {
                break;
            }
            BigDecimal used = loss.remaining().min(toCover);
            remaining.set(remaining.indexOf(loss), loss.use(used));
            compensations.add(new Compensation(loss.year(), used));
            toCover = toCover.subtract(used);
        }

        List<CapitalLoss> closing = new ArrayList<>(
                remaining.stream().filter(l -> l.remaining().signum() > 0).toList());
        CapitalLoss realized = null;
        if (net.signum() < 0) {
            realized = CapitalLoss.realized(year, net.negate(), carryForwardYears(year));
            closing.add(realized);
        }
        if (carriedLoss != null) {
            closing.add(CapitalLoss.realized(year, carriedLoss, carryForwardYears(year)));
        }
        closing.sort(Comparator.comparingInt(CapitalLoss::year));

        return new ZainettoYear(year, opening, expired, gains, losses, compensations, realized, closing);
    }

    /**
     * Years a loss can be carried forward. For losses entered by the user for years
     * before the
     * supported ones, the current rule applies: the four-year limit dates back to
     * 1998.
     */
    private int carryForwardYears(int lossYear) {
        return rules.forYear(Math.max(lossYear, RuleSetFactory.FIRST_SUPPORTED_YEAR))
                .lossCarryForwardYears();
    }
}
