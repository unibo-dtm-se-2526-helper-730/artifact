package it.helper730.i18n;

import it.helper730.domain.Msg;
import java.math.BigDecimal;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * Messages.
 *
 * Turns a domain {@link Msg} into text in the language of the current request.
 */
@Component
@RequiredArgsConstructor
public class Messages {

    private final MessageSource source;

    /** Text in the language of the current request. */
    public String text(Msg msg) {
        return text(msg, LocaleContextHolder.getLocale());
    }

    /**
     * Text in the given language. An unknown code is returned as it is, never as an
     * error.
     */
    public String text(Msg msg, Locale locale) {
        Object[] args = msg.params().stream().map(p -> format(p, locale)).toArray();
        return source.getMessage(msg.code(), args, msg.code(), locale);
    }

    /**
     * Parameters are formatted here, so MessageFormat only receives strings. Left
     * to MessageFormat, the year 2026 would become "2.026" in Italian.
     */
    static String format(Object param, Locale locale) {
        if (param == null) {
            return "";
        }
        if (param instanceof BigDecimal d) {
            char separator = DecimalFormatSymbols.getInstance(locale).getDecimalSeparator();
            return d.toPlainString().replace('.', separator);
        }
        if (param instanceof LocalDate date) {
            return date.format(
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale));
        }
        return String.valueOf(param);
    }
}
