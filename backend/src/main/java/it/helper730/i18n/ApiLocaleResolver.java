package it.helper730.i18n;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.LocaleResolver;

/**
 * ApiLocaleResolver.
 *
 * Chooses the language of each request: the {@code lang} query parameter, then
 * the {@code Accept-Language} header, then Italian.
 */
public class ApiLocaleResolver implements LocaleResolver {

    static final List<Locale> SUPPORTED = List.of(Locale.ITALIAN, Locale.ENGLISH);
    static final Locale DEFAULT = Locale.ITALIAN;

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        String lang = langParameter(request.getQueryString());
        if (lang != null) {
            String language = Locale.forLanguageTag(lang).getLanguage();
            for (Locale supported : SUPPORTED) {
                if (supported.getLanguage().equals(language)) {
                    return supported;
                }
            }
        }
        String header = request.getHeader(HttpHeaders.ACCEPT_LANGUAGE);
        if (header != null && !header.isBlank()) {
            try {
                Locale match = Locale.lookup(Locale.LanguageRange.parse(header), SUPPORTED);
                if (match != null) {
                    return match;
                }
            } catch (IllegalArgumentException malformedHeader) {
                // a malformed header must not break the request: use the default language
            }
        }
        return DEFAULT;
    }

    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        throw new UnsupportedOperationException("The language is chosen per request with ?lang= or Accept-Language");
    }

    /**
     * Reads {@code lang} from the query string only. {@code request.getParameter}
     * would also parse the body of a multipart upload, before Spring applies its
     * size limits.
     */
    static String langParameter(String query) {
        if (query == null) {
            return null;
        }
        for (String pair : query.split("&")) {
            if (pair.startsWith("lang=")) {
                return pair.substring("lang=".length());
            }
        }
        return null;
    }
}
