package it.helper730.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import it.helper730.domain.Msg;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

class MessagesTest {

    private final Messages messages = new Messages(source());

    /** Same settings as application.properties, without starting Spring. */
    private static ResourceBundleMessageSource source() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("i18n/messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        source.setAlwaysUseMessageFormat(true);
        return source;
    }

    @Test
    void translatesIntoItalianWithApostropheAndUngroupedYear() {
        String text = messages.text(Msg.of("error.unsupported_tax_year", 2014), Locale.ITALIAN);

        assertThat(text).isEqualTo("Anno d'imposta 2014 non supportato.");
    }

    @Test
    void translatesIntoEnglish() {
        String text = messages.text(Msg.of("error.unsupported_tax_year", 2014), Locale.ENGLISH);

        assertThat(text).isEqualTo("Tax year 2014 is not supported.");
    }

    @Test
    void unsupportedLanguageFallsBackToItalian() {
        String text = messages.text(Msg.of("error.empty_file"), Locale.GERMAN);

        assertThat(text).isEqualTo("Il file è vuoto.");
    }

    @Test
    void decimalSeparatorFollowsTheLanguage() {
        Msg msg = Msg.of("warning.implied_fee", new BigDecimal("1.00"), new BigDecimal("0.52"));

        assertThat(messages.text(msg, Locale.ITALIAN)).startsWith("Commissione implicita di 1,00 € (0,52%)");
        assertThat(messages.text(msg, Locale.ENGLISH)).startsWith("Implied fee of €1.00 (0.52%)");
    }

    @Test
    void unknownCodeIsReturnedAsItIs() {
        assertThat(messages.text(Msg.of("error.does_not_exist"), Locale.ITALIAN))
                .isEqualTo("error.does_not_exist");
    }

    @Test
    void bothLanguagesHaveTheSameKeys() throws IOException {
        assertThat(keys("i18n/messages_en.properties")).isEqualTo(keys("i18n/messages.properties"));
    }

    private static java.util.Set<String> keys(String resource) throws IOException {
        Properties properties = new Properties();
        try (Reader reader = new InputStreamReader(
                MessagesTest.class.getClassLoader().getResourceAsStream(resource), StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties.stringPropertyNames();
    }
}
