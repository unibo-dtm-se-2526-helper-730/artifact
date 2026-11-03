package it.helper730.domain.ivafe;

import it.helper730.domain.Money;
import java.math.BigDecimal;
import java.util.List;
import java.util.TreeSet;

/**
 * IVAFE of a year.
 *
 * @param year             tax year
 * @param rate             yearly rate
 * @param rows             one row per holding period, sorted by ticker and
 *                         start date
 * @param paymentThreshold total below which nothing is paid
 */
public record IvafeResult(int year, BigDecimal rate, List<IvafeRow> rows, BigDecimal paymentThreshold) {

    public IvafeResult {
        rows = List.copyOf(rows);
    }

    /** Total of the computed rows. */
    public BigDecimal total() {
        return rows.stream()
                .filter(IvafeRow::isComputed)
                .map(IvafeRow::ivafe)
                .reduce(Money.round(BigDecimal.ZERO), BigDecimal::add);
    }

    /**
     * Tickers held at 31 December without a year-end price: the total is incomplete
     * until they are entered.
     */
    public List<String> missingPrices() {
        TreeSet<String> missing = new TreeSet<>();
        rows.stream().filter(r -> !r.isComputed()).forEach(r -> missing.add(r.ticker()));
        return List.copyOf(missing);
    }

    /** Whether every row could be computed. */
    public boolean isComplete() {
        return missingPrices().isEmpty();
    }

    /** Whether the total is below the threshold, so nothing is paid. */
    public boolean belowThreshold() {
        return total().compareTo(paymentThreshold) < 0;
    }
}
