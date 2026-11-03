package it.helper730.engine.zainetto;

import it.helper730.domain.zainetto.CapitalLoss;
import java.util.Comparator;
import java.util.List;

/** Decides in which order the losses available in a year are used. */
@FunctionalInterface
public interface CompensationStrategy {

    /**
     * Losses used first are those that expire first: none is lost while a newer one
     * is used.
     */
    CompensationStrategy OLDEST_FIRST = available -> available.stream()
            .sorted(Comparator.comparingInt(CapitalLoss::lastUsableYear).thenComparingInt(CapitalLoss::year))
            .toList();

    /** Returns the available losses in the order in which they are to be used. */
    List<CapitalLoss> order(List<CapitalLoss> available);
}
