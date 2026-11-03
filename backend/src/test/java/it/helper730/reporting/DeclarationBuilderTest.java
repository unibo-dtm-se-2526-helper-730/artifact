package it.helper730.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import it.helper730.GoldenData;
import it.helper730.domain.AssetClass;
import it.helper730.domain.Instrument;
import it.helper730.domain.Msg;
import it.helper730.domain.OperationType;
import it.helper730.domain.TaxYearResult;
import it.helper730.domain.Transaction;
import it.helper730.domain.ivafe.IvafeResult;
import it.helper730.engine.TaxCalculator;
import it.helper730.engine.ivafe.IvafeCalculator;
import it.helper730.engine.rules.RuleSetFactory;
import it.helper730.engine.zainetto.CompensationStrategy;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DeclarationBuilderTest {

    private final RuleSetFactory rules = new RuleSetFactory();
    private final TaxCalculator taxCalculator = new TaxCalculator(rules, CompensationStrategy.OLDEST_FIRST);
    private final IvafeCalculator ivafeCalculator = new IvafeCalculator(rules);
    private final DeclarationBuilder builder = new DeclarationBuilder();

    private static final Map<String, BigDecimal> GOLDEN_PRICES_2026 = Map.of(
            "VGWD", new BigDecimal("80"),
            "CEBS", new BigDecimal("9"),
            "EUNK", new BigDecimal("100"),
            "XEON", new BigDecimal("150"));

    private Declaration build(
            int year,
            Collection<Transaction> operations,
            Map<String, Instrument> instruments,
            Map<Integer, BigDecimal> carriedLosses,
            Map<String, BigDecimal> prices) {
        TaxYearResult tax = taxCalculator.calculate(year, operations, instruments, carriedLosses);
        IvafeResult ivafe = ivafeCalculator.calculate(year, operations, prices, Map.of());
        return builder.build(tax, ivafe, instruments, "LT");
    }

    private static DeclarationItem item(Declaration declaration, String quadro, String key) {
        return declaration.section(quadro).orElseThrow().items().stream()
                .filter(i -> i.key().equals(key))
                .findFirst()
                .orElseThrow();
    }

    private static Transaction dividend(int row, String ticker, String net) {
        return new Transaction(
                String.format("%064x", row),
                Instant.parse("2026-03-01T10:00:00Z"),
                OperationType.DIVIDEND,
                ticker,
                null,
                null,
                new BigDecimal(net),
                "EUR",
                row);
    }

    @Test
    void goldenDeclarationFor2026() {
        Declaration declaration =
                build(2026, GoldenData.operations(), GoldenData.instruments(), Map.of(), GOLDEN_PRICES_2026);

        assertThat(declaration.model()).isEqualTo("730/2027");
        assertThat(declaration.sections())
                .extracting(DeclarationSection::quadro)
                .containsExactly("T", "M", "W");
        // 5J50: proceeds and cost of the sale
        assertThat(item(declaration, "T", "t11_proceeds").amount()).isEqualByComparingTo("390.10");
        assertThat(item(declaration, "T", "t11_cost").amount()).isEqualByComparingTo("407.00");
        // VGWD dividends from Ireland, XEON gain from Luxembourg, both fund income at
        // 26%
        assertThat(item(declaration, "M", "m31_B_040_26").amount()).isEqualByComparingTo("5.11");
        assertThat(item(declaration, "M", "m31_B_092_26").amount()).isEqualByComparingTo("1.96");
        assertThat(declaration.section("W").orElseThrow().items())
                .isNotEmpty()
                .allSatisfy(i -> assertThat(i.rigo()).startsWith("W"));
        assertThat(declaration.warnings()).isEmpty();
    }

    @Test
    void lossesOfPreviousYearsGoInT102InTheColumnOfTheirYear() {
        Declaration declaration = build(
                2026,
                GoldenData.operations(),
                GoldenData.instruments(),
                Map.of(2024, new BigDecimal("30")),
                GOLDEN_PRICES_2026);

        DeclarationItem loss = item(declaration, "T", "t102_2024");
        // columns 1-4 are 2022-2025
        assertThat(loss.rigo()).isEqualTo("T102 col. 3");
        assertThat(loss.amount()).isEqualByComparingTo("30");
    }

    @Test
    void etfIncomeIsSplitBetween26And12Point5AndForeignDividendsAreGross() {
        Map<String, Instrument> instruments = Map.of(
                "AGGH",
                new Instrument("AGGH", "IE00B3RBWM25", null, AssetClass.UCITS_ETF, new BigDecimal("0.40"), null),
                "AAPL",
                new Instrument("AAPL", "US0378331005", null, AssetClass.STOCK, null, new BigDecimal("0.15")));
        List<Transaction> operations = List.of(dividend(1, "AGGH", "100"), dividend(2, "AAPL", "8.50"));

        Declaration declaration = build(2026, operations, instruments, Map.of(), Map.of());

        assertThat(item(declaration, "M", "m31_B_040_26").amount()).isEqualByComparingTo("60.00");
        assertThat(item(declaration, "M", "m31_B_040_12.5").amount()).isEqualByComparingTo("40.00");
        DeclarationItem apple = item(declaration, "M", "m31_H_069_26");
        assertThat(apple.amount()).isEqualByComparingTo("10.00");
        assertThat(apple.secondary()).isEqualByComparingTo("1.50");
    }

    @Test
    void missingIsinAndMissingPriceAreReported() {
        Map<String, Instrument> instruments = GoldenData.instruments();
        instruments.put("VGWD", new Instrument("VGWD", null, null, AssetClass.UCITS_ETF, null, null));

        Declaration declaration = build(2026, GoldenData.operations(), instruments, Map.of(), Map.of());

        assertThat(declaration.warnings())
                .extracting(Msg::code)
                .contains("declaration.unknown_state_of_origin", "declaration.missing_year_end_price");
    }

    @Test
    void excludedInstrumentsAreReported() {
        Map<String, Instrument> instruments =
                Map.of("SPY", new Instrument("SPY", "US78462F1030", null, AssetClass.NON_UCITS_ETF, null, null));
        List<Transaction> operations = List.of(dividend(1, "SPY", "10"));

        Declaration declaration = build(2026, operations, instruments, Map.of(), Map.of());

        assertThat(declaration.section("M")).isEmpty();
        assertThat(declaration.warnings()).extracting(Msg::code).containsExactly("declaration.excluded_instrument");
    }

    @Test
    void yearWithoutOperationsHasNoSections() {
        Declaration declaration = build(2026, List.of(), Map.of(), Map.of(), Map.of());

        assertThat(declaration.sections()).isEmpty();
        assertThat(declaration.warnings()).isEmpty();
    }
}
