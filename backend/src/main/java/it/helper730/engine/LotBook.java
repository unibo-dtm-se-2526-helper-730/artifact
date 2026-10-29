package it.helper730.engine;

import it.helper730.domain.Disposal;
import it.helper730.domain.Lot;
import it.helper730.domain.LotConsumption;
import it.helper730.domain.Money;
import it.helper730.domain.OperationType;
import it.helper730.domain.Transaction;
import it.helper730.domain.exceptions.UncoveredSaleException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Open lots of every instrument, updated by the purchases and sales in
 * chrnological order.
 * Sales are matched to lots with the LIFO criterion: the most recent purchase
 * is sold first.
 */
public class LotBook {

    /**
     * Chronological order of operations; the file row breaks ties between equal
     * instants.
     */
    public static final Comparator<Transaction> CHRONOLOGICAL = Comparator.comparing(Transaction::executedAt)
            .thenComparing(Transaction::id);

    /** Open lots per ticker; the head of each deque is the most recent lot */
    private final Map<String, Deque<Lot>> openLots = new TreeMap<>();

    /**
     * Disposals (sales matched to the lots they consumed) in chronological order.
     */
    private final List<Disposal> disposals = new ArrayList<>();

    /**
     * Last transaction applied to the lot book.
     */
    private Transaction lastTransactionApplied;

    /** Builds the book by applying the given operations in chronological order. */
    public static LotBook replay(Collection<Transaction> transactions) {
        LotBook book = new LotBook();
        transactions.stream().sorted(CHRONOLOGICAL).forEach(book::apply);
        return book;
    }

    /**
     * Applies one operation of type purchase or sale.
     * A purchase opens a lot, a sale consumes a lot and returns the resulting
     * disposal.
     *
     * @throws IllegalArgumentException if the operation precedes the last one
     *                                  applied
     */
    public Optional<Disposal> apply(Transaction t) {
        if (lastTransactionApplied != null && CHRONOLOGICAL.compare(t, lastTransactionApplied) < 0) {
            throw new IllegalArgumentException("Operations must be applied in chronological order: row " + t.fileRow()
                    + " precedes row " + lastTransactionApplied.fileRow());
        }

        lastTransactionApplied = t;
        if (t.type() == OperationType.BUY) {
            openLots.computeIfAbsent(t.ticker(), k -> new ArrayDeque<>()).addFirst(Lot.of(t));
            return Optional.empty();
        }
        if (t.type() == OperationType.SELL) {
            Disposal disposal = sell(t);
            disposals.add(disposal);
            return Optional.of(disposal);
        }
        return Optional.empty();
    }

    /**
     * Performs a sale by consuming the open lots of the given ticker in LIFO order.
     *
     * @throws UncoveredSaleException if a sale exceeds the shares held; the book is
     *                                left unchanged
     */
    private Disposal sell(Transaction sale) {
        Deque<Lot> lots = openLots.getOrDefault(sale.ticker(), new ArrayDeque<>());

        BigDecimal available = quantityOf(lots);
        if (sale.quantity().compareTo(available) > 0) {
            throw new UncoveredSaleException(sale.ticker(), sale.fileRow(), sale.quantity(), available);
        }

        List<LotConsumption> consumptions = new ArrayList<>();
        BigDecimal toSell = sale.quantity();
        BigDecimal proceedsLeft = sale.totalAmount();
        while (toSell.signum() > 0) {
            Lot lot = lots.removeFirst();
            BigDecimal taken = toSell.min(lot.quantity());
            BigDecimal cost = lot.costOf(taken);
            toSell = toSell.subtract(taken);

            // the last consumption takes what is left, so the parts add up to the sale
            // amount exactly
            BigDecimal proceeds = toSell.signum() == 0 ? proceedsLeft
                    : Money.share(sale.totalAmount(), taken, sale.quantity());
            proceedsLeft = proceedsLeft.subtract(proceeds);
            consumptions.add(new LotConsumption(lot.buyTransactionId(), lot.acquiredAt(), taken, cost, proceeds));

            BigDecimal remaining = lot.quantity().subtract(taken);
            if (remaining.signum() > 0) {
                lots.addFirst(new Lot(
                        lot.buyTransactionId(),
                        lot.ticker(),
                        lot.acquiredAt(),
                        remaining,
                        lot.cost().subtract(cost)));
            }
        }
        if (lots.isEmpty()) {
            openLots.remove(sale.ticker());
        }

        return new Disposal(sale, consumptions);
    }

    /** Returns a list of all disposals. */
    public List<Disposal> disposals() {
        return List.copyOf(disposals);
    }

    /** Returns a list of all open lots for the given ticker, in LIFO order. */
    public List<Lot> openLots(String ticker) {
        List<Lot> lots = new ArrayList<>(openLots.getOrDefault(ticker, new ArrayDeque<>()));
        return List.copyOf(lots.reversed());
    }

    /** Returns a map of all open quantities by ticker. */
    public Map<String, BigDecimal> openQuantities() {
        Map<String, BigDecimal> result = new TreeMap<>();
        openLots.forEach((ticker, lots) -> result.put(ticker, quantityOf(lots)));
        return result;
    }

    /** Returns the instant at which the last transaction was applied. */
    public Optional<Instant> lastTransactionAppliedAt() {
        return Optional.ofNullable(lastTransactionApplied).map(Transaction::executedAt);
    }

    /**
     * Returns the total quantity of lots in the given lots.
     */
    private static BigDecimal quantityOf(Collection<Lot> lots) {
        return lots.stream().map(Lot::quantity).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
