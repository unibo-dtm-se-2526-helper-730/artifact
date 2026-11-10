package it.helper730.ingestion.traderepublic;

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
import java.util.Locale;
import java.util.Objects;
import lombok.Getter;

/**
 * TradeRepublicParser.
 *
 * Reads the transaction export of Trade Republic: 23 columns, numbers with a
 * decimal point, UTC timestamps. The type column holds the operation type of
 * the app ({@code BUY}, {@code SELL}, {@code DIVIDEND}, {@code COUPON},
 * {@code CASH_TOP_UP}, {@code CASH_WITHDRAWAL}). Amount, fee and tax are
 * signed: the total of the operation is {@code |amount + fee + tax|}, so a
 * purchase includes its fee and a sale or a dividend is net of fee and tax.
 * Instruments are identified by ISIN.
 */
public class TradeRepublicParser implements BrokerParser {

    static final List<String> HEADER = List.of(
            "datetime",
            "date",
            "account_type",
            "category",
            "type",
            "asset_class",
            "name",
            "symbol",
            "shares",
            "price",
            "amount",
            "fee",
            "tax",
            "currency",
            "original_amount",
            "original_currency",
            "fx_rate",
            "description",
            "transaction_id",
            "counterparty_name",
            "counterparty_iban",
            "payment_reference",
            "mcc_code");

    private static final int DATETIME = 0;
    private static final int TYPE = 4;
    private static final int SYMBOL = 7;
    private static final int SHARES = 8;
    private static final int PRICE = 9;
    private static final int AMOUNT = 10;
    private static final int FEE = 11;
    private static final int TAX = 12;
    private static final int CURRENCY = 13;
    private static final int TRANSACTION_ID = 18;

    private final ValidationChain validation;

    public TradeRepublicParser(ValidationChain validation) {
        this.validation = Objects.requireNonNull(validation, "validation");
    }

    @Override
    public String broker() {
        return "TRADE_REPUBLIC";
    }

    /** Trade Republic Bank GmbH is base in Germany (DE). */
    @Override
    public String country() {
        return "DE";
    }

    @Override
    public boolean supports(CsvRow header) {
        return header.fields().stream().map(String::trim).toList().equals(HEADER);
    }

    @Override
    public ParseResult parse(List<CsvRow> rows) {
        List<Transaction> transacions = new ArrayList<>();
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
                    transacions.add(transaction);
                }
            } catch (RowException e) {
                issues.add(RowIssue.error(row.line(), e.msg()));
            } catch (DomainException e) {
                issues.add(RowIssue.error(row.line(), e.msg()));
            }
        }
        return new ParseResult(broker(), transacions, issues);
    }

    private Transaction toTransaction(CsvRow row) {
        int line = row.line();
        Instant executedAt = parseInstant(row.field(DATETIME), line);
        OperationType type = parseType(row.field(TYPE), line);
        String ticker = row.field(SYMBOL).isEmpty() ? null : row.field(SYMBOL);
        BigDecimal shares = number(row, SHARES, "shares");
        BigDecimal price = number(row, PRICE, "price");
        BigDecimal amount = number(row, AMOUNT, "amount");
        if (amount == null) {
            throw new RowException(Msg.of("error.missing_field", "amount", line));
        }
        BigDecimal fee = orZero(number(row, FEE, "fee"));
        BigDecimal tax = orZero(number(row, TAX, "fee"));
        BigDecimal total = amount.add(fee).add(tax).abs();
        BigDecimal quantity = shares == null ? null : shares.abs();

        String transactionId = row.field(TRANSACTION_ID);
        if (transactionId.isEmpty()) {
            throw new RowException(Msg.of("error.missing_field", "transaction_id", line));
        }

        String id = sha256(broker() + "|" + transactionId);
        return new Transaction(id, executedAt, type, ticker, quantity, price, total, row.field(CURRENCY), line);
    }

    static OperationType parseType(String raw, int line) {
        try {
            return OperationType.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new RowException(Msg.of("error.unknown_operation_type", raw, line));
        }
    }

    private static Instant parseInstant(String value, int line) {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            throw new RowException(Msg.of("error.invalid_field", "datetime", value, line));
        }
    }

    /** A number with a decimal point, or null when the cell is empty. */
    private static BigDecimal number(CsvRow row, int index, String column) {
        String value = row.field(index);
        if (value.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new RowException(Msg.of("error.invalid_field", column, value, row.line()));
        }
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * Computes the SHA-256 hash of the given string and returns it as a
     * hexadecimal string.
     *
     * The string is first encoded to bytes using {@link StandardCharsets#UTF_8},
     * then hashed with SHA-256. The result is a 64-character lowercase
     * hexadecimal string.
     *
     * @param value the string to hash; must not be {@code null}
     * @return the SHA-256 hash of {@code value} as a 64-character lowercase
     *         hexadecimal string
     * @throws NullPointerException  if {@code value} is {@code null}
     * @throws IllegalStateException if the SHA-256 algorithm is not available;
     *                               this should never happen, since every Java
     *                               platform is required
     *                               to support SHA-256
     */
    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by every Java platform", e);
        }
    }

    /**
     * RuntimeException.
     *
     * A row that cannot become a Transaction throws a RuntimeException
     */
    @Getter
    private static final class RowException extends RuntimeException {
        private final transient Msg msg;

        RowException(Msg msg) {
            super(msg.code(), null, false, false);
            this.msg = msg;
        }
    }
}
