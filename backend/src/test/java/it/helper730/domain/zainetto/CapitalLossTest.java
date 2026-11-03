package it.helper730.domain.zainetto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import it.helper730.domain.exceptions.InvalidDataException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CapitalLossTest {

    @Test
    void realizedLossIsUsableForFourYears() {
        CapitalLoss loss = CapitalLoss.realized(2026, new BigDecimal("16.9"), 4);

        assertThat(loss.amount()).isEqualByComparingTo("16.90");
        assertThat(loss.remaining()).isEqualByComparingTo("16.90");
        assertThat(loss.lastUsableYear()).isEqualTo(2030);
    }

    @Test
    void usingPartOfALossLeavesTheOriginalUnchanged() {
        CapitalLoss loss = CapitalLoss.realized(2025, new BigDecimal("50"), 4);

        CapitalLoss used = loss.use(new BigDecimal("30"));

        assertThat(used.remaining()).isEqualByComparingTo("20");
        assertThat(used.amount()).isEqualByComparingTo("50");
        assertThat(loss.remaining()).isEqualByComparingTo("50");
    }

    @Test
    void rejectsNonPositiveAmount() {
        assertThatThrownBy(() -> CapitalLoss.realized(2025, BigDecimal.ZERO, 4))
                .isInstanceOfSatisfying(
                        InvalidDataException.class,
                        e -> assertThat(e.msg().code()).isEqualTo("error.loss_not_positive"));
    }

    @Test
    void cannotUseMoreThanWhatIsLeft() {
        CapitalLoss loss = CapitalLoss.realized(2025, new BigDecimal("10"), 4);

        assertThatThrownBy(() -> loss.use(new BigDecimal("10.01"))).isInstanceOf(IllegalArgumentException.class);
    }
}
