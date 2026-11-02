package it.helper730.engine.zainetto;

import static org.assertj.core.api.Assertions.assertThat;

import it.helper730.domain.EventType;
import it.helper730.domain.IncomeCategory;
import it.helper730.domain.OperationType;
import it.helper730.domain.TaxableEvent;
import it.helper730.domain.Transaction;
import it.helper730.domain.zainetto.CapitalLoss;
import it.helper730.domain.zainetto.Compensation;
import it.helper730.domain.zainetto.ZainettoYear;
import it.helper730.engine.rules.RuleSetFactory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ZainettoTest {

    private final Zainetto zainetto = new Zainetto(new RuleSetFactory(), CompensationStrategy.OLDEST_FIRST);
    private int row = 1;

    /** A sale in mid-year with the given result, category and weight. */
    private TaxableEvent event(int year, IncomeCategory category, String amount, String weight) {
        int fileRow = row++;
        Transaction sale = new Transaction(
                String.format("%064x", fileRow),
                Instant.parse(year + "-06-15T10:00:00Z"),
                OperationType.SELL,
                "TEST",
                BigDecimal.ONE,
                BigDecimal.TEN,
                BigDecimal.TEN,
                "EUR",
                fileRow);
        BigDecimal value = new BigDecimal(amount);
        return new TaxableEvent(
                sale,
                value.signum() >= 0 ? EventType.CAPITAL_GAIN : EventType.CAPITAL_LOSS,
                category,
                value,
                new BigDecimal(weight),
                BigDecimal.ZERO,
                List.of());
    }

    private TaxableEvent miscellaneous(int year, String amount) {
        return event(year, IncomeCategory.MISCELLANEOUS, amount, "1");
    }

    private static ZainettoYear year(List<ZainettoYear> history, int year) {
        return history.stream().filter(y -> y.year() == year).findFirst().orElseThrow();
    }

    @Test
    void lossIsUsableFromTheNextYearUntilTheFourthAndThenExpires() {
        // the 5J50 case of the golden test
        List<ZainettoYear> history = zainetto.history(List.of(miscellaneous(2026, "-16.90")), Map.of(), 2031);

        ZainettoYear y2026 = year(history, 2026);
        assertThat(y2026.newLoss()).hasValueSatisfying(l -> {
            assertThat(l.amount()).isEqualByComparingTo("16.90");
            assertThat(l.lastUsableYear()).isEqualTo(2030);
        });
        assertThat(y2026.openingTotal()).isEqualByComparingTo("0");
        assertThat(y2026.closingTotal()).isEqualByComparingTo("16.90");

        assertThat(year(history, 2030).openingTotal()).isEqualByComparingTo("16.90");
        assertThat(year(history, 2031).openingTotal()).isEqualByComparingTo("0");
        assertThat(year(history, 2031).expiredTotal()).isEqualByComparingTo("16.90");
        assertThat(history).extracting(ZainettoYear::year).containsExactly(2026, 2027, 2028, 2029, 2030, 2031);
    }

    @Test
    void gainsAndLossesOfTheSameYearAreNetted() {
        List<ZainettoYear> history =
                zainetto.history(List.of(miscellaneous(2026, "100"), miscellaneous(2026, "-30")), Map.of(), 2026);

        ZainettoYear y2026 = year(history, 2026);
        assertThat(y2026.net()).isEqualByComparingTo("70");
        assertThat(y2026.taxableAmount()).isEqualByComparingTo("70");
        assertThat(y2026.newLoss()).isEmpty();
    }

    @Test
    void lossOffsetsMiscellaneousGainsOfALaterYear() {
        List<ZainettoYear> history =
                zainetto.history(List.of(miscellaneous(2025, "-50"), miscellaneous(2026, "30")), Map.of(), 2026);

        ZainettoYear y2026 = year(history, 2026);
        assertThat(y2026.compensations()).containsExactly(new Compensation(2025, new BigDecimal("30.00")));
        assertThat(y2026.taxableAmount()).isEqualByComparingTo("0");
        assertThat(y2026.closingTotal()).isEqualByComparingTo("20");
    }

    @Test
    void capitalIncomeIsNeverOffset() {
        List<ZainettoYear> history = zainetto.history(
                List.of(miscellaneous(2025, "-50"), event(2026, IncomeCategory.CAPITAL, "30", "1")), Map.of(), 2026);

        ZainettoYear y2026 = year(history, 2026);
        assertThat(y2026.compensations()).isEmpty();
        assertThat(y2026.closingTotal()).isEqualByComparingTo("50");
    }

    @Test
    void lossesThatExpireFirstAreUsedFirst() {
        List<ZainettoYear> history = zainetto.history(
                List.of(miscellaneous(2023, "-20"), miscellaneous(2024, "-20"), miscellaneous(2025, "30")),
                Map.of(),
                2025);

        assertThat(year(history, 2025).compensations())
                .containsExactly(
                        new Compensation(2023, new BigDecimal("20.00")),
                        new Compensation(2024, new BigDecimal("10.00")));
    }

    @Test
    void expiredLossIsNotUsed() {
        List<ZainettoYear> history =
                zainetto.history(List.of(miscellaneous(2020, "-50"), miscellaneous(2025, "30")), Map.of(), 2025);

        ZainettoYear y2025 = year(history, 2025);
        assertThat(y2025.expiredTotal()).isEqualByComparingTo("50");
        assertThat(y2025.compensations()).isEmpty();
        assertThat(y2025.taxableAmount()).isEqualByComparingTo("30");
    }

    @Test
    void lossesEnteredByTheUserAreUsedLikeTheOthers() {
        List<ZainettoYear> history =
                zainetto.history(List.of(miscellaneous(2026, "25")), Map.of(2024, new BigDecimal("40")), 2026);

        ZainettoYear y2026 = year(history, 2026);
        assertThat(y2026.compensations()).containsExactly(new Compensation(2024, new BigDecimal("25.00")));
        assertThat(y2026.closing())
                .singleElement()
                .extracting(CapitalLoss::remaining)
                .satisfies(r -> assertThat(r).isEqualByComparingTo("15"));
    }

    @Test
    void governmentBondLossEntersAt48Percent() {
        List<ZainettoYear> history =
                zainetto.history(List.of(event(2026, IncomeCategory.MISCELLANEOUS, "-100", "0.4808")), Map.of(), 2026);

        assertThat(year(history, 2026).closingTotal()).isEqualByComparingTo("48.08");
    }

    @Test
    void nothingToComputeGivesAnEmptyHistory() {
        assertThat(zainetto.history(List.of(), Map.of(), 2026)).isEmpty();
    }
}
