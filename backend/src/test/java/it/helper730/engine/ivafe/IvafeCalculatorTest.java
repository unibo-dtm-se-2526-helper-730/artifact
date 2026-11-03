package it.helper730.engine.ivafe;

import static org.assertj.core.api.Assertions.assertThat;

import it.helper730.domain.Msg;
import it.helper730.domain.OperationType;
import it.helper730.domain.Transaction;
import it.helper730.domain.ivafe.IvafeResult;
import it.helper730.domain.ivafe.IvafeRow;
import it.helper730.engine.rules.RuleSetFactory;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class IvafeCalculatorTest {

    private final IvafeCalculator calculator = new IvafeCalculator(new RuleSetFactory());
    private int row = 1;

    private Transaction trade(
            OperationType type, String ticker, String date, String quantity, String price, String total) {
        int fileRow = row++;
        return new Transaction(
                String.format("%064x", fileRow),
                Instant.parse(date + "T10:00:00Z"),
                type,
                ticker,
                new BigDecimal(quantity),
                new BigDecimal(price),
                new BigDecimal(total),
                "EUR",
                fileRow);
    }

    private static Map<String, BigDecimal> price(String ticker, String value) {
        return Map.of(ticker, new BigDecimal(value));
    }

    /** 10 ENI bought in 2025, 4 of them sold on 31 March 2026. */
    private List<Transaction> partialSaleIn2026() {
        return List.of(
                trade(OperationType.BUY, "ENI", "2025-05-10", "10", "40", "400"),
                trade(OperationType.SELL, "ENI", "2026-03-31", "4", "50", "200"));
    }

    @Test
    void goldenVgwdHeldFor80DaysIn2025() {
        Transaction purchase = new Transaction(
                String.format("%064x", 3),
                Instant.parse("2025-10-13T06:00:44.187Z"),
                OperationType.BUY,
                "VGWD",
                new BigDecimal("2.83427915"),
                new BigDecimal("66.80"),
                new BigDecimal("189.33"),
                "EUR",
                3);

        IvafeResult result = calculator.calculate(2025, List.of(purchase), price("VGWD", "70"), Map.of());

        assertThat(result.rows()).singleElement().satisfies(r -> {
            assertThat(r.from()).isEqualTo(LocalDate.of(2025, 10, 13));
            assertThat(r.to()).isEqualTo(LocalDate.of(2025, 12, 31));
            assertThat(r.holdingDays()).isEqualTo(80);
            assertThat(r.initialValue()).isEqualByComparingTo("189.33");
            assertThat(r.finalValue()).isEqualByComparingTo("198.40");
            assertThat(r.ivafe()).isEqualByComparingTo("0.09");
        });
        assertThat(result.total()).isEqualByComparingTo("0.09");
        assertThat(result.isComplete()).isTrue();
        assertThat(result.belowThreshold()).isTrue();
    }

    @Test
    void partialSaleSplitsTheLotIntoTwoHoldingPeriods() {
        IvafeResult result = calculator.calculate(2026, partialSaleIn2026(), price("ENI", "45"), price("ENI", "42"));

        assertThat(result.rows()).hasSize(2);
        IvafeRow sold = result.rows().get(0);
        // 1 January – 31 March: 90 days, valued at the sale price 4 × 50
        assertThat(sold.sold()).isTrue();
        assertThat(sold.holdingDays()).isEqualTo(90);
        assertThat(sold.initialValue()).isEqualByComparingTo("168.00");
        assertThat(sold.finalValue()).isEqualByComparingTo("200.00");
        assertThat(sold.ivafe()).isEqualByComparingTo("0.10");

        IvafeRow held = result.rows().get(1);
        // the other 6 shares all year, valued at the year-end price 6 × 45
        assertThat(held.sold()).isFalse();
        assertThat(held.holdingDays()).isEqualTo(365);
        assertThat(held.initialValue()).isEqualByComparingTo("252.00");
        assertThat(held.finalValue()).isEqualByComparingTo("270.00");
        assertThat(held.ivafe()).isEqualByComparingTo("0.54");

        assertThat(result.total()).isEqualByComparingTo("0.64");
    }

    @Test
    void missingYearEndPriceLeavesTheRowUncomputedAndIsReported() {
        IvafeResult result = calculator.calculate(2026, partialSaleIn2026(), Map.of(), Map.of());

        IvafeRow held = result.rows().get(1);
        assertThat(held.isComputed()).isFalse();
        assertThat(held.notes()).extracting(Msg::code).contains("ivafe.missing_price", "ivafe.initial_value_from_cost");
        assertThat(result.missingPrices()).containsExactly("ENI");
        assertThat(result.isComplete()).isFalse();
        // the sold part does not need the year-end price
        assertThat(result.total()).isEqualByComparingTo("0.10");
    }

    @Test
    void fullYearIsExactlyTheYearlyRateEvenInALeapYear() {
        List<Transaction> operations = List.of(trade(OperationType.BUY, "ENI", "2027-05-10", "10", "100", "1000"));

        IvafeResult result = calculator.calculate(2028, operations, price("ENI", "100"), price("ENI", "100"));

        assertThat(result.rows()).singleElement().satisfies(r -> {
            assertThat(r.holdingDays()).isEqualTo(366);
            assertThat(r.ivafe()).isEqualByComparingTo("2.00");
        });
    }

    @Test
    void positionsClosedBeforeTheYearAndOperationsAfterItAreIgnored() {
        List<Transaction> operations = List.of(
                trade(OperationType.BUY, "ENI", "2024-05-10", "10", "40", "400"),
                trade(OperationType.SELL, "ENI", "2024-09-10", "10", "45", "450"),
                trade(OperationType.BUY, "VWCE", "2027-01-10", "1", "100", "100"));

        IvafeResult result = calculator.calculate(2026, operations, Map.of(), Map.of());

        assertThat(result.rows()).isEmpty();
        assertThat(result.total()).isEqualByComparingTo("0");
    }
}
