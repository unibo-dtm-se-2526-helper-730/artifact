package it.helper730.config;

import it.helper730.domain.Transaction;
import it.helper730.ingestion.BrokerParserRegistry;
import it.helper730.ingestion.CsvReader;
import it.helper730.ingestion.revolut.RevolutParser;
import it.helper730.ingestion.validation.FutureDateValidator;
import it.helper730.ingestion.validation.ImpliedFeeValidator;
import it.helper730.ingestion.validation.ValidationChain;
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
        return new BrokerParserRegistry(new CsvReader(), List.of(new RevolutParser(validation))); // FIXME -- add new
        // parsers here
    }

    @Bean
    ValidationChain validationChain(Clock clock) {
        return new ValidationChain(List.of(new FutureDateValidator(clock), new ImpliedFeeValidator()));
    }
}
