package it.helper730.service;

import it.helper730.domain.Instrument;
import it.helper730.domain.TaxYearResult;
import it.helper730.domain.Transaction;
import it.helper730.domain.ivafe.IvafeResult;
import it.helper730.domain.portfolio.Portfolio;
import it.helper730.engine.PortfolioCalculator;
import it.helper730.engine.TaxCalculator;
import it.helper730.engine.ivafe.IvafeCalculator;
import it.helper730.persistence.EntityMapper;
import it.helper730.persistence.repository.CarriedLossRepository;
import it.helper730.persistence.repository.InstrumentRepository;
import it.helper730.persistence.repository.TransactionRepository;
import it.helper730.persistence.repository.YearEndPriceRepository;
import it.helper730.reporting.Declaration;
import it.helper730.reporting.DeclarationBuilder;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads the stored data and runs the tax engine. Every calculation starts from
 * the whole history, because lots (LIFO) and the Zainetto depend on earlier
 * years.
 */
@Service
@Transactional(readOnly = true)
public class TaxService {

    private final TransactionRepository transactions;
    private final InstrumentRepository instruments;
    private final YearEndPriceRepository prices;
    private final CarriedLossRepository carriedLosses;
    private final TaxCalculator taxCalculator;
    private final IvafeCalculator ivafeCalculator;
    private final PortfolioCalculator portfolioCalculator;
    private final DeclarationBuilder declarationBuilder;
    private final String brokerCountry;

    public TaxService(
            TransactionRepository transactions,
            InstrumentRepository instruments,
            YearEndPriceRepository prices,
            CarriedLossRepository carriedLosses,
            TaxCalculator taxCalculator,
            IvafeCalculator ivafeCalculator,
            PortfolioCalculator portfolioCalculator,
            DeclarationBuilder declarationBuilder,
            @Value("${helper730.broker-country}") String brokerCountry) {
        this.transactions = transactions;
        this.instruments = instruments;
        this.prices = prices;
        this.carriedLosses = carriedLosses;
        this.taxCalculator = taxCalculator;
        this.ivafeCalculator = ivafeCalculator;
        this.portfolioCalculator = portfolioCalculator;
        this.declarationBuilder = declarationBuilder;
        this.brokerCountry = brokerCountry;
    }

    /**
     * Tax years with at least one tax-relevant operation, most recent first (year
     * menu).
     */
    public List<Integer> years() {
        return allTransactions().stream()
                .filter(t -> t.type().isTaxRelevant())
                .map(Transaction::taxYear)
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    /**
     * Substitute tax and Zainetto of one year. Throws if instruments are
     * unclassified.
     */
    public TaxYearResult tax(int year) {
        return taxCalculator.calculate(year, allTransactions(), instrumentMap(), carriedLossMap());
    }

    /**
     * IVAFE of one year, using the prices at 31/12 of that year and of the previous
     * one.
     */
    public IvafeResult ivafe(int year) {
        return ivafeCalculator.calculate(year, allTransactions(), priceMap(year), priceMap(year - 1));
    }

    /**
     * Modello 730 summary (quadri T, M, W) built from tax and IVAFE of the same
     * year.
     */
    public Declaration declaration(int year) {
        return declarationBuilder.build(tax(year), ivafe(year), instrumentMap(), brokerCountry);
    }

    /** Current holdings at cost; unclassified instruments are allowed here. */
    public Portfolio portfolio() {
        return portfolioCalculator.calculate(allTransactions(), instrumentMap());
    }

    private List<Transaction> allTransactions() {
        return transactions.findAllByOrderByExecutedAtAscIdAsc().stream()
                .map(EntityMapper::toDomain)
                .toList();
    }

    private Map<String, Instrument> instrumentMap() {
        return instruments.findAll().stream()
                .map(EntityMapper::toDomain)
                .collect(Collectors.toMap(Instrument::ticker, Function.identity()));
    }

    private Map<Integer, BigDecimal> carriedLossMap() {
        return carriedLosses.findAll().stream().collect(Collectors.toMap(e -> e.lossYear(), e -> e.amount()));
    }

    private Map<String, BigDecimal> priceMap(int year) {
        return prices.findByTaxYearOrderByTickerAsc(year).stream()
                .collect(Collectors.toMap(p -> p.ticker(), p -> p.price()));
    }
}
