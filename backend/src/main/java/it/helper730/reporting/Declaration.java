package it.helper730.reporting;

import it.helper730.domain.Msg;
import java.util.List;
import java.util.Optional;

/**
 * What to write in the Modello 730 for one tax year.
 *
 * @param year     tax year
 * @param model    form to fill in, e.g. {@code 730/2027} for tax year 2026
 * @param sections non-empty quadri, in the order T, M, W
 * @param warnings things the user must know or fix before filing
 */
public record Declaration(int year, String model, List<DeclarationSection> sections, List<Msg> warnings) {

    public Declaration {
        sections = List.copyOf(sections);
        warnings = List.copyOf(warnings);
    }

    /** The section of a quadro, if it has values. */
    public Optional<DeclarationSection> section(String quadro) {
        return sections.stream().filter(s -> s.quadro().equals(quadro)).findFirst();
    }
}
