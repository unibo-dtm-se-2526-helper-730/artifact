package it.helper730.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

public class TransactionTest {

    private static final String ID = "a".repeat(64);

    private static Transaction trade(OperationType type, String at, String qty, String price, String total) {
        return new Transaction(
                ID,
                Instant.parse(at),
                type,
                "EUNK",
                new BigDecimal(qty),
                new BigDecimal(price),
                new BigDecimal(total),
                Transaction.SUPPORTED_CURRENCY,
                8);
    }

    @Test
    void taxYearUsesItalianLocalDate() {
        // 23:30 UTC on 31 December is already 00:30 on 1 January in Rome
        Transaction t = trade(OperationType.BUY, "2025-12-31T23:30:00Z", "1", "10", "10");

        assertThat(t.taxYear()).isEqualTo(2026);
    }

    @Test
    void impliedFeeOnBuyIsWhatWasPaidAboveQuantityTimesPrice() {
        Transaction t = trade(OperationType.BUY, "2026-03-02T10:00:00Z", "2", "95.65", "192.30");

        assertThat(t.impliedFee()).isEqualByComparingTo("1.00");
    }

    @Test
    void impliedFeeOnSellIsWhatWasReceivedBelowQuantityTimesPrice() {
        Transaction t = trade(OperationType.SELL, "2026-03-02T10:00:00Z", "1", "100", "99");

        assertThat(t.impliedFee()).isEqualByComparingTo("1.00");
    }

    @Test
    void roundingDifferenceIsNotAFee() {
        // example row: 2.83427915 * 66.80 = 189.3298..
        Transaction t = trade(OperationType.BUY, "2025-10-13T06:00:44.187Z", "2.83427915", "66.80", "189.33");

        assertThat(t.impliedFee()).isEqualByComparingTo("0");
    }

    @Test
    void rejectsCurrencyOtherThanEur() {
        assertThatThrownBy(() -> new Transaction(
                        ID,
                        Instant.now(),
                        OperationType.DIVIDEND,
                        "VGWD",
                        null,
                        null,
                        new BigDecimal("1.20"),
                        "USD",
                        5))
                .isInstanceOfSatisfying(UnsupportedCurrencyException.class, e -> {
                    assertThat(e.code()).isEqualTo("UNSUPPORTED_CURRENCY");
                    assertThat(e.msg().params()).containsExactly("USD", 5);
                });
    }

    @Test
    void tradeRequiresPositiveQuantity() {
        assertThatThrownBy(() -> trade(OperationType.SELL, "2026-01-05T10:00:00Z", "0", "10", "10"))
                .isInstanceOfSatisfying(
                        InvalidDataException.class,
                        e -> assertThat(e.msg().params()).containsExactly("quantity", 8));
    }

    @Test
    void dividendRequiresTicker() {
        assertThatThrownBy(() -> new Transaction(
                        ID, Instant.now(), OperationType.DIVIDEND, null, null, null, new BigDecimal("1.20"), "EUR", 5))
                .isInstanceOf(InvalidDataException.class);
    }

    @Test
    void cashTopUpNeedsNoTicker() {
        Transaction t = new Transaction(
                ID, Instant.now(), OperationType.CASH_TOP_UP, null, null, null, new BigDecimal("500"), "EUR", 2);

        assertThat(t.impliedFee()).isEqualByComparingTo("0");
    }

    @Test
    void rejectsMalformedId() {
        assertThatThrownBy(() -> new Transaction(
                        "abc", Instant.now(), OperationType.CASH_TOP_UP, null, null, null, BigDecimal.TEN, "EUR", 2))
                .isInstanceOf(InvalidDataException.class);
    }
}
