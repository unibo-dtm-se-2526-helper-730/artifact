package it.helper730.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.regex.Pattern;

import it.helper730.domain.exceptions.InvalidDataException;
import it.helper730.domain.exceptions.UnsupportedCurrencyException;

/**
 * One operation from a broker export, already parsed and normalized.
 *
 * @param id            SHA-256 (hex) of the normalized row: re-importing a file
 *                      creates no duplicates
 * @param executedAt    execution instant
 * @param type          operation type
 * @param ticker        instrument ticker; {@code null} for cash movements
 * @param quantity      number of shares, fractional; only for trades
 * @param pricePerShare price of one share; only for trades
 * @param totalAmount   amount actually paid or received: the authoritative
 *                      figure
 * @param currency      ISO 4217 code; only EUR is supported
 * @param fileRow       row number in the source file, for error messages and
 *                      the audit trail
 */
public record Transaction(
        String id,
        Instant executedAt,
        OperationType type,
        String ticker,
        BigDecimal quantity,
        BigDecimal pricePerShare,
        BigDecimal totalAmount,
        String currency,
        int fileRow) {

    /** Time zone that decides the tax year of an operation. */
    public static final ZoneId TAX_ZONE = ZoneId.of("Europe/Rome");

    public static final String SUPPORTED_CURRENCY = "EUR";

    private static final Pattern SHA256_HEX = Pattern.compile("[0-9a-f]{64}");

    public Transaction {
        Objects.requireNonNull(executedAt, "executedAt");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(totalAmount, "totalAmount");
        if (id == null || !SHA256_HEX.matcher(id).matches()) {
            throw new InvalidDataException(Msg.of("error.invalid_transaction_id", fileRow));
        }
        if (!SUPPORTED_CURRENCY.equals(currency)) {
            throw new UnsupportedCurrencyException(currency, fileRow);
        }
        if (type.isTaxRelevant() && (ticker == null || ticker.isBlank())) {
            throw new InvalidDataException(Msg.of("error.ticker_required_row", fileRow));
        }
        if (type.isTrade()) {
            requirePositive(quantity, "quantity", fileRow);
            requirePositive(pricePerShare, "price_per_share", fileRow);
        }
        if (type.isTaxRelevant()) {
            requirePositive(totalAmount, "total_amount", fileRow);
        }
    }

    /** Tax year of the operation: the calendar year of its local date in Italy. */
    public int taxYear() {
        return executedAt.atZone(TAX_ZONE).getYear();
    }

    /**
     * Fee implied by the difference between the total amount and quantity * price,
     * in cents.
     * For a purchase the fee raises what was paid, for a sale it lowers what was
     * received.
     * Returns zero for operations that are not trades.
     */
    public BigDecimal impliedFee() {
        if (!type.isTrade()) {
            return BigDecimal.ZERO.setScale(Money.SCALE);
        }
        BigDecimal gross = quantity.multiply(pricePerShare);
        BigDecimal fee = type == OperationType.BUY ? totalAmount.subtract(gross) : gross.subtract(totalAmount);
        return Money.round(fee);
    }

    private static void requirePositive(BigDecimal value, String field, int fileRow) {
        if (value == null || value.signum() <= 0) {
            throw new InvalidDataException(Msg.of("error.not_positive", field, fileRow));
        }
    }
}
