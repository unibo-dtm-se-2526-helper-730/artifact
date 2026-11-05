package it.helper730.config;

import it.helper730.i18n.ApiLocaleResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;

/**
 * I18nConfig.
 *
 * Language of each request (see {@link ApiLocaleResolver}).
 */
@Configuration
public class I18nConfig {

    /**
     * The bean name must be "localeResolver".
     * It is the one the DispatcherServlet looks for.
     */
    @Bean
    LocaleResolver localeResolver() {
        return new ApiLocaleResolver();
    }
}
