package it.helper730.domain;

import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

/**
 * Raised when the calculation needs instruments the user has not classified
 * yet.
 */
public class UnclassifiedInstrumentsException extends DomainException {

    private final transient List<String> tickers;

    public UnclassifiedInstrumentsException(Collection<String> tickers) {
        this(List.copyOf(new TreeSet<>(tickers)));
    }

    private UnclassifiedInstrumentsException(List<String> sortedTickers) {
        super(Msg.of("error.unclassified_instruments", String.join(", ", sortedTickers)));
        this.tickers = sortedTickers;
    }

    /** Tickers to classify, sorted. */
    public List<String> tickers() {
        return tickers;
    }

    @Override
    public String code() {
        return "UNCLASSIFIED_INSTRUMENTS";
    }
}
