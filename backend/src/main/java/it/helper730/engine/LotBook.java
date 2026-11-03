package it.helper730.engine;

import it.helper730.domain.CostMethod;
import it.helper730.domain.Disposal;
import it.helper730.domain.Lot;
import it.helper730.domain.LotConsumption;
import it.helper730.domain.Money;
import it.helper730.domain.OperationType;
import it.helper730.domain.Transaction;
import it.helper730.engine.exceptions.UncoveredSaleException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Open lots of every instrument, updated by purchases and sales in
 * chronological order. Each sale
 * consumes lots with the cost method chosen by the {@link CostMethodPolicy}:
 * <ul>
 * <li>LIFO: the most recent lots first, costs with fees;
 * <li>weighted average: every open lot in proportion to its quantity, costs
 * without fees. Taking the
 * same fraction of each lot gives exactly the average cost and leaves the
 * average unchanged.
 * </ul>
 */
public final class LotBook {

    /**
     * Chronological order of operations; the file row breaks ties between equal
     * instants.
     */
    public static final Comparator<Transaction> CHRONOLOGICAL =
            Comparator.comparing(Transaction::executedAt).thenComparingInt(Transaction::fileRow);

    private final CostMethodPolicy costMethods;

    /** Open lots per ticker; the head of each deque is the most recent lot. */
    private final Map<String, Deque<Lot>> openLots = new TreeMap<>();

    private final List<Disposal> disposals = new ArrayList<>();
    private Transaction lastApplied;

    public LotBook(CostMethodPolicy costMethods) {
        this.costMethods = Objects.requireNonNull(costMethods, "costMethods");
    }

    /** Builds the book by applying the given operations in chronological order. */
    public static LotBook replay(Collection<Transaction> transactions, CostMethodPolicy costMethods) {
        LotBook book = new LotBook(costMethods);
        transactions.stream().sorted(CHRONOLOGICAL).forEach(book::apply);
        return book;
    }

    /**
     * Applies one operation. A purchase opens a lot, a sale consumes lots and
     * returns the resulting
     * disposal; other operations do not change the lots.
     *
     * @throws IllegalArgumentException if the operation precedes the last one
     *                                  applied
     * @throws UncoveredSaleException   if a sale exceeds the shares held; the book
     *                                  is left unchanged
     */
    public Optional<Disposal> apply(Transaction t) {
        if (lastApplied != null && CHRONOLOGICAL.compare(t, lastApplied) < 0) {
            throw new IllegalArgumentException("Operations must be applied in chronological order: row " + t.fileRow()
                    + " precedes row " + lastApplied.fileRow());
        }
        lastApplied = t;
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

    private Disposal sell(Transaction sale) {
        List<Lot> lots = new ArrayList<>(openLots.getOrDefault(sale.ticker(), new ArrayDeque<>()));
        BigDecimal available = quantityOf(lots);
        if (sale.quantity().compareTo(available) > 0) {
            throw new UncoveredSaleException(sale.ticker(), sale.fileRow(), sale.quantity(), available);
        }
        CostMethod method = costMethods.forSale(sale);
        List<BigDecimal> taken = method == CostMethod.LIFO
                ? lifoTakes(lots, sale.quantity())
                : proportionalTakes(lots, sale.quantity(), available);

        BigDecimal saleBase =
                method.includesFees() ? sale.totalAmount() : sale.quantity().multiply(sale.pricePerShare());
        int lastConsumed = lastNonZero(taken);
        BigDecimal proceedsLeft = saleBase;
        List<LotConsumption> consumptions = new ArrayList<>();
        Deque<Lot> remaining = new ArrayDeque<>();

        for (int i = 0; i < lots.size(); i++) {
            Lot lot = lots.get(i);
            BigDecimal part = taken.get(i);
            if (part.signum() == 0) {
                remaining.addLast(lot);
                continue;
            }
            // the last consumption takes what is left, so the parts add up to the sale base
            // exactly
            BigDecimal proceeds = i == lastConsumed ? proceedsLeft : Money.share(saleBase, part, sale.quantity());
            proceedsLeft = proceedsLeft.subtract(proceeds);
            consumptions.add(new LotConsumption(
                    lot.buyTransactionId(),
                    lot.acquiredAt(),
                    part,
                    lot.costOf(part, method.includesFees()),
                    lot.costOf(part, true),
                    proceeds));
            if (part.compareTo(lot.quantity()) < 0) {
                remaining.addLast(lot.remainderAfter(part));
            }
        }

        if (remaining.isEmpty()) {
            openLots.remove(sale.ticker());
        } else {
            openLots.put(sale.ticker(), remaining);
        }
        return new Disposal(sale, method, consumptions);
    }

    /**
     * LIFO: lots are listed newest first, so take from the head until the sale is
     * covered.
     */
    private static List<BigDecimal> lifoTakes(List<Lot> lots, BigDecimal toSell) {
        List<BigDecimal> taken = new ArrayList<>();
        BigDecimal left = toSell;
        for (Lot lot : lots) {
            BigDecimal part = left.min(lot.quantity());
            taken.add(part);
            left = left.subtract(part);
        }
        return taken;
    }

    /**
     * Weighted average: the same fraction (sold / held) of every lot; the last lot
     * takes what is
     * left so that the parts add up to the quantity sold exactly.
     */
    private static List<BigDecimal> proportionalTakes(List<Lot> lots, BigDecimal toSell, BigDecimal held) {
        List<BigDecimal> taken = new ArrayList<>();
        if (toSell.compareTo(held) == 0) {
            lots.forEach(lot -> taken.add(lot.quantity()));
            return taken;
        }
        BigDecimal assigned = BigDecimal.ZERO;
        for (int i = 0; i < lots.size(); i++) {
            BigDecimal part = i == lots.size() - 1
                    ? toSell.subtract(assigned)
                    : Money.share(lots.get(i).quantity(), toSell, held);
            if (part.compareTo(lots.get(i).quantity()) > 0) {
                throw new IllegalStateException("Proportional share exceeds the lot: " + part);
            }
            taken.add(part);
            assigned = assigned.add(part);
        }
        return taken;
    }

    private static int lastNonZero(List<BigDecimal> values) {
        for (int i = values.size() - 1; i >= 0; i--) {
            if (values.get(i).signum() > 0) {
                return i;
            }
        }
        return -1;
    }

    /** Sales applied so far, in chronological order. */
    public List<Disposal> disposals() {
        return List.copyOf(disposals);
    }

    /** Open lots of an instrument, oldest first; empty if none. */
    public List<Lot> openLots(String ticker) {
        List<Lot> lots = new ArrayList<>(openLots.getOrDefault(ticker, new ArrayDeque<>()));
        return List.copyOf(lots.reversed());
    }

    /**
     * Shares held per ticker, for the instruments with at least one open lot,
     * sorted by ticker.
     */
    public Map<String, BigDecimal> openQuantities() {
        Map<String, BigDecimal> result = new TreeMap<>();
        openLots.forEach((ticker, lots) -> result.put(ticker, quantityOf(lots)));
        return result;
    }

    /** Instant of the last operation applied, if any. */
    public Optional<Instant> lastAppliedAt() {
        return Optional.ofNullable(lastApplied).map(Transaction::executedAt);
    }

    private static BigDecimal quantityOf(Collection<Lot> lots) {
        return lots.stream().map(Lot::quantity).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
