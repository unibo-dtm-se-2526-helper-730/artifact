package it.helper730.ingestion.revolut;

import it.helper730.domain.Msg;
import it.helper730.domain.OperationType;
import it.helper730.domain.Transaction;
import it.helper730.domain.exceptions.DomainException;
import it.helper730.ingestion.BrokerParser;
import it.helper730.ingestion.CsvRow;
import it.helper730.ingestion.ParseResult;
import it.helper730.ingestion.RowIssue;
import it.helper730.ingestion.Severity;
import it.helper730.ingestion.validation.ValidationChain;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * RevolutParser.
 *
 * Reads the trading export of Revolut:
 * {@code Date,Ticker,Type,Quantity,Price per share,Total Amount,Currency}.
 * Quantities use a decimal comma ({@code "2,83427915"}), amounts a decimal
 * point and a currency prefix ({@code EUR 66.80}), timestamps are UTC with
 * milli or microseconds. Blank rows are skipped.
 */
public final class RevolutParser implements BrokerParser {

    static final List<String> HEADER = List.of("Date", "Ticker", "Type", "Quantity", "Price per share", "Total Amount",
            "Currency");

    /**
     * Operation types meaning a corporate action, which changes holdings without a
     * trade.
     */
    private static final List<String> CORPORATE_ACTIONS = List.of("STOCK SPLIT", "REVERSE SPLIT", "MERGER", "SPINOFF",
            "SPIN-OFF", "STOCK DIVIDEND");

    private final ValidationChain validation;

    public RevolutParser(ValidationChain validation) {
        this.validation = Objects.requireNonNull(validation, "validation");
    }

    @Override
    public String broker() {
        return "REVOLUT";
    }

    @Override
    public boolean supports(CsvRow header) {
        return header.fields().stream().map(String::trim).toList().equals(HEADER);
    }

    @Override
    public ParseResult parse(List<CsvRow> rows) {
        List<Transaction> transactions = new ArrayList<>();
        List<RowIssue> issues = new ArrayList<>();
        for (CsvRow row : rows) {
            if (row.isBlank()) {
                continue;
            }
            try {
                Transaction transaction = toTransaction(row);
                List<RowIssue> found = validation.validate(transaction);
                issues.addAll(found);
                if (found.stream().noneMatch(i -> i.severity() == Severity.ERROR)) {
                    transactions.add(transaction);
                }
            } catch (RowException e) {
                issues.add(RowIssue.error(row.line(), e.msg));
            } catch (DomainException e) {
                issues.add(RowIssue.error(row.line(), e.msg()));
            }
        }
        return new ParseResult(broker(), transactions, issues);
    }

    private Transaction toTransaction(CsvRow row) {
        Instant executedAt = parseInstant(row.field(0), row.line());
        String ticker = row.field(1).isEmpty() ? null : row.field(1);
        String rawType = row.field(2);
        OperationType type = parseType(rawType, row.line());
        String currency = row.field(6);
        BigDecimal quantity = parseQuantity(row.field(3), row.line()).orElse(null);
        BigDecimal price = parseAmount(row.field(4), currency, row.line()).orElse(null);
        BigDecimal total = parseAmount(row.field(5), currency, row.line())
                .orElseThrow(() -> new RowException(Msg.of("error.missing_field", "Total Amount", row.line())));
        String id = sha256(String.join(
                "|",
                executedAt.toString(),
                Objects.toString(ticker, ""),
                rawType,
                plain(quantity),
                plain(price),
                plain(total),
                currency));

        return new Transaction(id, executedAt, type, ticker, quantity, price, total, currency, row.line());
    }

    /**
     * Parses the operation type, which is case-insensitive and may have a suffix
     * (e.g. {@code BUY - MARKET}, {@code BUY - LIMIT} ... are all purchases
     *
     * @param raw  the raw value of the Type column
     * @param line the line number in the CSV file
     * @return the parsed operation type
     */
    static OperationType parseType(String raw, int line) {
        String type = raw.toUpperCase();
        if (type.startsWith("BUY")) {
            return OperationType.BUY;
        }
        if (type.startsWith("SELL")) {
            return OperationType.SELL;
        }
        switch (type) {
            case "DIVIDEND" -> {
                return OperationType.DIVIDEND;
            }
            case "COUPON" -> {
                return OperationType.COUPON;
            }
            case "CASH TOP-UP" -> {
                return OperationType.CASH_TOP_UP;
            }
            case "CASH WITHDRAWAL" -> {
                return OperationType.CASH_WITHDRAWAL;
            }
            case "CUSTODY FEE" -> {
                return OperationType.CUSTODY_FEE;
            }
            default -> {
                if (CORPORATE_ACTIONS.stream().anyMatch(type::startsWith)) {
                    throw new RowException(Msg.of("error.corporate_action", raw, line));
                }
                throw new RowException(Msg.of("error.unknown_operation_type", raw, line));
            }
        }
    }

    /**
     * Parse a timestamp into an Instant. The timestamp is in UTC and may have milli
     * or microseconds.
     *
     * @param value the raw timestamp value
     * @param line  the line number in the CSV file
     * @return the parsed Instant
     */
    private static Instant parseInstant(String value, int line) {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            throw new RowException(Msg.of("error.invalid_field", "Date", value, line));
        }
    }

    /** Quantities use a decimal comma: {@code 2,83427915}. */
    static Optional<BigDecimal> parseQuantity(String value, int line) {
        if (value.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(number(value.replace(",", "."), "Quantity", line));
    }

    /**
     * Amounts have an optional currency prefix, a decimal point and possibly a
     * comma as thousands separator: {@code EUR 1,234.56}. The prefix must match the
     * Currency column.
     */
    static Optional<BigDecimal> parseAmount(String value, String currency, int line) {
        if (value.isEmpty()) {
            return Optional.empty();
        }
        String number = value;
        int space = value.indexOf(' ');
        if (space > 0) {
            String prefix = value.substring(0, space);
            if (!prefix.equals(currency)) {
                throw new RowException(Msg.of("error.currency_mismatch", prefix, currency, line));
            }
            number = value.substring(space + 1);
        }
        return Optional.of(number(number.replace(",", ""), "Amount", line));
    }

    /**
     * Parse a number, throwing a RowException if it is invalid.
     *
     * @param value the raw value to parse
     * @param field the name of the field being parsed
     * @param line  the line number in the CSV file
     * @return the parsed BigDecimal
     *
     * @throws RowException if the value is not a valid number
     */
    private static BigDecimal number(String value, String field, int line) {
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            throw new RowException(Msg.of("error.invalid_field", field, value, line));
        }
    }

    /**
     * Convert a BigDecimal to a plain string, removing trailing zeros.
     *
     * @param value the BigDecimal to convert
     * @return the plain string representation
     */
    private static String plain(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    /**
     * Calculate the SHA-256 hash of a string.
     * The identifier of a row is the SHA-256 of its normalized values, so the same
     * row always has the same id.
     */
    private static String sha256(String normalized) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(normalized.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by every Java platform", e);
        }
    }

    /**
     * RowException.
     *
     * A RowException is thrown when a row has an issue that prevents it from being
     * parsed into a Transaction.
     */
    private static final class RowException extends RuntimeException {
        private final transient Msg msg;

        RowException(Msg msg) {
            super(msg.code(), null, false, false);
            this.msg = msg;
        }
    }
}
