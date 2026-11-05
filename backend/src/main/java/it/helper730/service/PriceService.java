package it.helper730.service;

import it.helper730.domain.Msg;
import it.helper730.domain.exceptions.InvalidDataException;
import it.helper730.persistence.entity.YearEndPriceEntity;
import it.helper730.persistence.repository.InstrumentRepository;
import it.helper730.persistence.repository.YearEndPriceRepository;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Prices at 31/12 entered by the user, used to value holdings for IVAFE. */
@Service
@RequiredArgsConstructor
public class PriceService {

    private final YearEndPriceRepository prices;
    private final InstrumentRepository instruments;

    /** Prices of one year, ordered by ticker. */
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> get(int year) {
        Map<String, BigDecimal> result = new TreeMap<>();
        prices.findByTaxYearOrderByTickerAsc(year).forEach(p -> result.put(p.getTicker(), p.getPrice()));
        return result;
    }

    /**
     * Replaces all the prices of a year. Tickers must exist in the instrument
     * registry (foreign key) and prices must be positive (check constraint): both
     * are verified here first, so the user gets a clear message instead of a
     * database error.
     */
    @Transactional
    public Map<String, BigDecimal> replace(int year, Map<String, BigDecimal> newPrices) {
        Set<String> unknown = new TreeSet<>(newPrices.keySet());
        instruments.findAllById(newPrices.keySet()).forEach(i -> unknown.remove(i.getTicker()));
        if (!unknown.isEmpty()) {
            throw new InvalidDataException(Msg.of("error.unknown_tickers", String.join(", ", unknown)));
        }
        newPrices.forEach((ticker, price) -> {
            if (price == null || price.signum() <= 0) {
                throw new InvalidDataException(Msg.of("error.price_not_positive", ticker, price));
            }
        });

        prices.deleteByTaxYear(year);
        prices.saveAll(newPrices.entrySet().stream()
                .map(e -> new YearEndPriceEntity(year, e.getKey(), e.getValue()))
                .toList());
        return new TreeMap<>(newPrices);
    }
}
