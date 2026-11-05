package it.helper730.config;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadata of the generated OpenAPI document (served at /v3/api-docs, UI at
 * /swagger-ui.html).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI helper730OpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("helper-730 API")
                        .version("v1")
                        .description("""
                                Substitute tax, Zainetto and IVAFE for Italian investors using Revolut \
                                under the declarative regime. Amounts are strings with a decimal point; \
                                the language of messages is chosen with ?lang=it|en or Accept-Language. \
                                Errors follow RFC 9457 (application/problem+json) with a stable "code".""")
                        .license(new License().name("See LICENSE in the repository")));
    }

    /**
     * Swagger builds the schemas with its own Jackson 2 mapper: give it the same
     * naming as the API.
     */
    @Bean
    ModelResolver snakeCaseModelResolver() {
        return new ModelResolver(Json.mapper().copy().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE));
    }
}
