package it.helper730.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class InstrumentTest {

    @ParameterizedTest
    @ValueSource(strings = {"IE00BK5BQT80", "US0378331005", "LU0290358497"})
    void acceptsValidIsin(String isin) {
        assertThat(Instrument.isValidIsin(isin)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"IE00BK5BQT81", "ie00bk5bqt80", "IE00BK5BQT8", "1E00BK5BQT80"})
    void rejectsMalformedIsinOrWrongCheckDigit(String isin) {
        assertThat(Instrument.isValidIsin(isin)).isFalse();
    }

    @Test
    void constructorRejectsInvalidIsin() {
        assertThatThrownBy(() -> new Instrument("VWCE", "IE00BK5BQT81", null, AssetClass.UCITS_ETF, null, null))
                .isInstanceOfSatisfying(InvalidDataException.class, e -> {
                    assertThat(e.code()).isEqualTo("INVALID_DATA");
                    assertThat(e.msg().code()).isEqualTo("error.invalid_isin");
                });
    }

    @Test
    void unclassifiedInstrumentIsNotClassified() {
        Instrument instrument = Instrument.unclassified("5J50");

        assertThat(instrument.isClassified()).isFalse();
        assertThat(instrument.assetClass()).isNull();
    }

    @Test
    void ucitsEtfAcceptsGovernmentBondShare() {
        Instrument xeon = new Instrument("XEON", null, null, AssetClass.UCITS_ETF, new BigDecimal("0.40"), null);

        assertThat(xeon.governmentBondShare()).isEqualByComparingTo("0.4");
    }

    @Test
    void stockRejectsGovernmentBondShare() {
        assertThatThrownBy(() -> new Instrument("ANT", null, null, AssetClass.STOCK, new BigDecimal("0.40"), null))
                .isInstanceOf(InvalidDataException.class);
    }

    @Test
    void ucitsEtfRejectsForeignWithholding() {
        assertThatThrownBy(() -> new Instrument("VGWD", null, null, AssetClass.UCITS_ETF, null, new BigDecimal("0.15")))
                .isInstanceOf(InvalidDataException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.01", "1.01"})
    void rejectsRateOutsideZeroOne(String rate) {
        assertThatThrownBy(() -> new Instrument("XEON", null, null, AssetClass.UCITS_ETF, new BigDecimal(rate), null))
                .isInstanceOfSatisfying(
                        InvalidDataException.class,
                        e -> assertThat(e.msg().code()).isEqualTo("error.rate_out_of_range"));
    }

    @Test
    void rejectsBlankTicker() {
        assertThatThrownBy(() -> Instrument.unclassified(" ")).isInstanceOf(InvalidDataException.class);
    }
}
