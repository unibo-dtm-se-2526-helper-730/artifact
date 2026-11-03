package it.helper730.reporting;

import it.helper730.domain.Msg;
import java.util.List;
import java.util.Objects;

/**
 * The values of one quadro of the declaration.
 *
 * @param quadro quadro letter: {@code T}, {@code M} or {@code W}
 * @param title  title of the section, translatable
 * @param items  values to write, in the order of the form
 */
public record DeclarationSection(String quadro, Msg title, List<DeclarationItem> items) {

    public DeclarationSection {
        Objects.requireNonNull(quadro, "quadro");
        Objects.requireNonNull(title, "title");
        items = List.copyOf(items);
    }
}
