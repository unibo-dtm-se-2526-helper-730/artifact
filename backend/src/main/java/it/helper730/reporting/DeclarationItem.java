package it.helper730.reporting;

import it.helper730.domain.Msg;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * One value to write in the declaration.
 *
 * @param key         stable identifier, e.g. {@code t11_proceeds}
 * @param description what the value is, translatable
 * @param amount      amount to write, in cents
 * @param rigo        where to write it, e.g. {@code T11 col. 1}
 * @param detail      further information (codes of the other columns),
 *                    translatable; may be {@code null}
 * @param secondary   a related amount (e.g. initial value, foreign tax); may be
 *                    {@code null}
 */
public record DeclarationItem(
        String key, Msg description, BigDecimal amount, String rigo, Msg detail, BigDecimal secondary) {

    public DeclarationItem {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(rigo, "rigo");
    }
}
