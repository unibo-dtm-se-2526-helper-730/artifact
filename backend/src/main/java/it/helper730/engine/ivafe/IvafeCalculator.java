package it.helper730.engine.ivafe;

import it.helper730.domain.CostMethod;
import it.helper730.domain.Disposal;
import it.helper730.domain.Lot;
import it.helper730.domain.LotConsumption;
import it.helper730.domain.Money;
import it.helper730.domain.Msg;
import it.helper730.domain.Transaction;
import it.helper730.domain.ivafe.IvafeResult;
import it.helper730.domain.ivafe.IvafeRow;
import it.helper730.engine.CostMethodPolicy;
import it.helper730.engine.LotBook;
import it.helper730.engine.rules.RuleSetFactory;
import it.helper730.engine.rules.TaxRuleSet;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Computes the IVAFE of a year, holding period by holding period: value × rate
 * × days held / days
 * of the year. The value is the sale value for shares sold during the year, the
 * year-end value for
 * shares still held. Holding periods are identified with LIFO, independently of
 * the tax cost
 * method, so no classification is needed.
 */
public final class IvafeCalculator {

    private final RuleSetFactory rules;

    public IvafeCalculator(RuleSetFactory rules) {
        this.rules = Objects.requireNonNull(rules, "rules");
    }

    /**
     * Computes the IVAFE of the given year.
     *
     * @param year                  tax year
     * @param transactions          all stored operations; those after {@code year}
     *                              are ignored
     * @param yearEndPrices         prices at 31 December of {@code year}, by ticker
     * @param previousYearEndPrices prices at 31 December of the previous year, by
     *                              ticker, used as
     *                              initial value of shares held on 1 January
     */
    public IvafeResult calculate(
            int year,
            Collection<Transaction> transactions,
            Map<String, BigDecimal> yearEndPrices,
            Map<String, BigDecimal> previousYearEndPrices) {
        TaxRuleSet ruleSet = rules.forYear(year);
        Period period = new Period(year, ruleSet.ivafeRate());
        List<Transaction> upToYear =
                transactions.stream().filter(t -> t.taxYear() <= year).toList();
        LotBook book = LotBook.replay(upToYear, CostMethodPolicy.always(CostMethod.LIFO));

        List<IvafeRow> rows = new ArrayList<>();
        for (Disposal disposal : book.disposals()) {
            Transaction sale = disposal.sale();
            if (sale.taxYear() != year) {
                continue;
            }
            for (LotConsumption c : disposal.consumptions()) {
                BigDecimal finalValue = Money.round(c.quantity().multiply(sale.pricePerShare()));
                rows.add(period.row(
                        sale.ticker(),
                        c.quantity(),
                        c.acquiredAt(),
                        localDate(sale.executedAt()),
                        true,
                        null,
                        c.cashCost(),
                        finalValue,
                        previousYearEndPrices));
            }
        }
        for (String ticker : book.openQuantities().keySet()) {
            BigDecimal price = yearEndPrices.get(ticker);
            for (Lot lot : book.openLots(ticker)) {
                BigDecimal finalValue =
                        price == null ? null : Money.round(lot.quantity().multiply(price));
                rows.add(period.row(
                        ticker,
                        lot.quantity(),
                        lot.acquiredAt(),
                        period.lastDay,
                        false,
                        price,
                        lot.cost(),
                        finalValue,
                        previousYearEndPrices));
            }
        }
        rows.sort(Comparator.comparing(IvafeRow::ticker).thenComparing(IvafeRow::from));
        return new IvafeResult(year, ruleSet.ivafeRate(), rows, ruleSet.paymentThreshold());
    }

    private static LocalDate localDate(Instant instant) {
        return instant.atZone(Transaction.TAX_ZONE).toLocalDate();
    }

    /** The calendar of a tax year and the arithmetic of one row. */
    private static final class Period {
        private final LocalDate firstDay;
        private final LocalDate lastDay;
        private final int daysInYear;
        private final BigDecimal rate;

        Period(int year, BigDecimal rate) {
            this.firstDay = LocalDate.of(year, 1, 1);
            this.lastDay = LocalDate.of(year, 12, 31);
            this.daysInYear = Year.of(year).length();
            this.rate = rate;
        }

        IvafeRow row(
                String ticker,
                BigDecimal quantity,
                Instant acquiredAt,
                LocalDate to,
                boolean sold,
                BigDecimal yearEndPrice,
                BigDecimal purchaseCost,
                BigDecimal finalValue,
                Map<String, BigDecimal> previousYearEndPrices) {
            List<Msg> notes = new ArrayList<>();
            LocalDate acquired = localDate(acquiredAt);
            LocalDate from = acquired.isBefore(firstDay) ? firstDay : acquired;
            int days = (int) ChronoUnit.DAYS.between(from, to) + 1;

            BigDecimal initialValue = Money.round(purchaseCost);
            if (acquired.isBefore(firstDay)) {
                BigDecimal previousPrice = previousYearEndPrices.get(ticker);
                if (previousPrice != null) {
                    initialValue = Money.round(quantity.multiply(previousPrice));
                } else {
                    notes.add(Msg.of("ivafe.initial_value_from_cost", ticker));
                }
            }
            if (sold) {
                notes.add(Msg.of("ivafe.sale_value", ticker));
            }

            BigDecimal ivafe = null;
            if (finalValue == null) {
                notes.add(Msg.of("ivafe.missing_price", ticker));
            } else {
                ivafe = finalValue
                        .multiply(rate)
                        .multiply(BigDecimal.valueOf(days))
                        .divide(BigDecimal.valueOf(daysInYear), Money.SCALE, Money.ROUNDING);
            }
            return new IvafeRow(
                    ticker, quantity, from, to, sold, yearEndPrice, initialValue, finalValue, days, ivafe, notes);
        }
    }
}
