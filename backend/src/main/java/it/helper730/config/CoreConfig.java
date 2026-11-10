package it.helper730.config;

import it.helper730.domain.Transaction;
import it.helper730.engine.PortfolioCalculator;
import it.helper730.engine.TaxCalculator;
import it.helper730.engine.ivafe.IvafeCalculator;
import it.helper730.engine.rules.RuleSetFactory;
import it.helper730.engine.zainetto.CompensationStrategy;
import it.helper730.ingestion.BrokerParserRegistry;
import it.helper730.ingestion.CsvReader;
import it.helper730.ingestion.revolut.RevolutParser;
import it.helper730.ingestion.traderepublic.TradeRepublicParser;
import it.helper730.ingestion.validation.FutureDateValidator;
import it.helper730.ingestion.validation.ImpliedFeeValidator;
import it.helper730.ingestion.validation.ValidationChain;
import it.helper730.reporting.DeclarationBuilder;
import java.time.Clock;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * CoreConfig.
 *
 * Exposes the framework-free core (ingestion, engine) as Spring beans.
 */
@Configuration
public class CoreConfig {

    /** Single source of "now": tests can replace it with a fixed clock. */
    @Bean
    Clock clock() {
        return Clock.system(Transaction.TAX_ZONE);
    }

    @Bean
    BrokerParserRegistry brokerParserRegistry(ValidationChain validation) {
        return new BrokerParserRegistry(
                new CsvReader(), List.of(new RevolutParser(validation), new TradeRepublicParser(validation)));
    }

    @Bean
    ValidationChain validationChain(Clock clock) {
        return new ValidationChain(List.of(new FutureDateValidator(clock), new ImpliedFeeValidator()));
    }

    @Bean
    RuleSetFactory ruleSetFactory() {
        return new RuleSetFactory();
    }

    /**
     * Default Zainetto policy: the oldest losses are used first, as they expire
     * first.
     */
    @Bean
    CompensationStrategy compensationStrategy() {
        return CompensationStrategy.OLDEST_FIRST;
    }

    @Bean
    TaxCalculator taxCalculator(RuleSetFactory rules, CompensationStrategy strategy) {
        return new TaxCalculator(rules, strategy);
    }

    @Bean
    IvafeCalculator ivafeCalculator(RuleSetFactory rules) {
        return new IvafeCalculator(rules);
    }

    @Bean
    PortfolioCalculator portfolioCalculator(RuleSetFactory rules) {
        return new PortfolioCalculator(rules);
    }

    @Bean
    DeclarationBuilder declarationBuilder() {
        return new DeclarationBuilder();
    }
}
