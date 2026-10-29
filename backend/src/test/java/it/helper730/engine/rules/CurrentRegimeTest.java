package it.helper730.engine.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import it.helper730.domain.AssetClass;
import it.helper730.domain.IncomeCategory;
import it.helper730.domain.Instrument;
import it.helper730.domain.InvalidDataException;
import it.helper730.domain.OperationType;
import it.helper730.domain.UnsupportedTaxYearException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class CurrentRegimeTest {

    private final TaxRuleSet rules = new CurrentRegime();

    private static Instrument instrument(AssetClass assetClass) {
        return new Instrument("TEST", null, null, assetClass, null, null);
    }

    @ParameterizedTest
    @EnumSource(
            value = AssetClass.class,
            names = {"STOCK", "BOND", "GOVERNMENT_BOND_NON_WL", "ETC_ETN"})
    void gainsAndLossesAreFullyTaxableMiscellaneousIncome(AssetClass assetClass) {
        for (boolean gain : new boolean[] {true, false}) {
            Treatment treatment = rules.disposalTreatment(instrument(assetClass), gain);

            assertThat(treatment.category()).isEqualTo(IncomeCategory.MISCELLANEOUS);
            assertThat(treatment.weight()).isEqualByComparingTo("1");
        }
    }

    @Test
    void whiteListGovernmentBondHasEffectiveRateOf12Point5() {
        Treatment treatment = rules.disposalTreatment(instrument(AssetClass.GOVERNMENT_BOND_WL), true);

        BigDecimal effectiveRate = treatment.weight().multiply(rules.rate());

        assertThat(effectiveRate).isEqualByComparingTo("0.125008");
    }

    @Test
    void ucitsEtfGainIsCapitalIncomeAndLossIsMiscellaneous() {
        Instrument etf = instrument(AssetClass.UCITS_ETF);

        assertThat(rules.disposalTreatment(etf, true).category()).isEqualTo(IncomeCategory.CAPITAL);
        assertThat(rules.disposalTreatment(etf, false).category()).isEqualTo(IncomeCategory.MISCELLANEOUS);
    }

    @Test
    void ucitsEtfWeightReflectsGovernmentBondShare() {
        Instrument etf = new Instrument("AGGH", null, null, AssetClass.UCITS_ETF, new BigDecimal("0.40"), null);

        assertThat(rules.incomeTreatment(etf, OperationType.DIVIDEND).weight()).isEqualByComparingTo("0.79232");
        assertThat(rules.disposalTreatment(etf, true).weight()).isEqualByComparingTo("0.79232");
    }

    @Test
    void nonUcitsEtfIsExcluded() {
        Treatment treatment = rules.disposalTreatment(instrument(AssetClass.NON_UCITS_ETF), true);

        assertThat(treatment.category()).isEqualTo(IncomeCategory.EXCLUDED);
        assertThat(treatment.weight()).isEqualByComparingTo("0");
    }

    @Test
    void incomeFromEtcIsRejected() {
        assertThatThrownBy(() -> rules.incomeTreatment(instrument(AssetClass.ETC_ETN), OperationType.DIVIDEND))
                .isInstanceOfSatisfying(
                        InvalidDataException.class,
                        e -> assertThat(e.msg().code()).isEqualTo("error.income_not_expected"));
    }

    @Test
    void factoryRejectsYearsBeforeTheCurrentRegime() {
        RuleSetFactory factory = new RuleSetFactory();

        assertThat(factory.forYear(2026)).isInstanceOf(CurrentRegime.class);
        assertThatThrownBy(() -> factory.forYear(2014))
                .isInstanceOfSatisfying(
                        UnsupportedTaxYearException.class,
                        e -> assertThat(e.code()).isEqualTo("UNSUPPORTED_TAX_YEAR"));
    }
}
