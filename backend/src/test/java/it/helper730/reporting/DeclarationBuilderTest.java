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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
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
        // every holding comes from Revolut, as in the golden file
        Map<String, Set<String>> countries = operations.stream()
                .map(Transaction::ticker)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toMap(t -> t, t -> Set.of("LT")));
        return builder.build(tax, ivafe, instruments, countries);
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

    private static Transaction trade(
            int row, String instant, OperationType type, String quantity, String price, String total) {
        return new Transaction(
                String.format("%064x", row),
                Instant.parse(instant),
                type,
                "AAPL",
                new BigDecimal(quantity),
                new BigDecimal(price),
                new BigDecimal(total),
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
    void lossesToCarryForwardGoInT102InTheColumnOfTheirYear() {
        Declaration declaration = build(
                2026,
                GoldenData.operations(),
                GoldenData.instruments(),
                Map.of(2024, new BigDecimal("30")),
                GOLDEN_PRICES_2026);

        // columns 1-5 are 2022-2026
        DeclarationItem previous = item(declaration, "T", "t102_2024");
        assertThat(previous.rigo()).isEqualTo("T102 col. 3");
        assertThat(previous.amount()).isEqualByComparingTo("30");
        // the net loss of the year (5J50: 390.10 - 407.00) is carried forward too
        DeclarationItem current = item(declaration, "T", "t102_2026");
        assertThat(current.rigo()).isEqualTo("T102 col. 5");
        assertThat(current.amount()).isEqualByComparingTo("16.90");
    }

    @Test
    void t102ListsOnlyWhatIsLeftAfterTheYear() {
        Map<String, Instrument> instruments =
                Map.of("AAPL", new Instrument("AAPL", "US0378331005", null, AssetClass.STOCK, null, null));
        List<Transaction> operations = List.of(
                trade(1, "2026-02-01T10:00:00Z", OperationType.BUY, "1", "100", "101"),
                trade(2, "2026-06-01T10:00:00Z", OperationType.SELL, "1", "150", "149"));

        // gain 48: the 2022 loss (oldest, last usable in 2026) and 18 of the 2023 one
        // are used
        Declaration declaration = build(
                2026,
                operations,
                instruments,
                Map.of(2022, new BigDecimal("30"), 2023, new BigDecimal("40"), 2024, new BigDecimal("25")),
                Map.of());

        assertThat(declaration.section("T").orElseThrow().items())
                .extracting(DeclarationItem::key)
                .containsExactly("t11_proceeds", "t11_cost", "t102_2023", "t102_2024");
        assertThat(item(declaration, "T", "t102_2023").amount()).isEqualByComparingTo("22");
        assertThat(item(declaration, "T", "t102_2024").amount()).isEqualByComparingTo("25");
    }

    @Test
    void aLossThatExpiresInTheYearIsNotCarriedForward() {
        Declaration declaration = build(
                2026,
                GoldenData.operations(),
                GoldenData.instruments(),
                Map.of(2022, new BigDecimal("30")),
                GOLDEN_PRICES_2026);

        // the 2022 loss is usable until 31/12/2026, but 2026 has no gain to offset
        assertThat(declaration.section("T").orElseThrow().items())
                .extracting(DeclarationItem::key)
                .doesNotContain("t102_2022")
                .contains("t102_2026");
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
    void foreignTaxOnGovernmentBondCouponsGoesToThe12Point5Line() {
        Map<String, Instrument> instruments = Map.of(
                "OAT",
                new Instrument(
                        "OAT", "FR0010070060", null, AssetClass.GOVERNMENT_BOND_WL, null, new BigDecimal("0.15")));
        List<Transaction> operations = List.of(new Transaction(
                String.format("%064x", 1),
                Instant.parse("2026-03-01T10:00:00Z"),
                OperationType.COUPON,
                "OAT",
                null,
                null,
                new BigDecimal("85"),
                "EUR",
                1));

        Declaration declaration = build(2026, operations, instruments, Map.of(), Map.of());

        // net 85 with 15% withheld in France: gross 100, foreign tax 15, all at 12.5%
        DeclarationItem coupon = item(declaration, "M", "m31_A_029_12.5");
        assertThat(coupon.amount()).isEqualByComparingTo("100.00");
        assertThat(coupon.secondary()).isEqualByComparingTo("15.00");
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

    @Test
    void theStateInQuadroWComesFromTheBrokerOfEachHolding() {
        TaxYearResult tax = taxCalculator.calculate(2026, GoldenData.operations(), GoldenData.instruments(), Map.of());
        IvafeResult ivafe = ivafeCalculator.calculate(2026, GoldenData.operations(), GOLDEN_PRICES_2026, Map.of());
        Map<String, Set<String>> countries = new HashMap<>();
        GoldenData.instruments().keySet().forEach(t -> countries.put(t, Set.of("DE")));
        countries.put("VGWD", new TreeSet<>(Set.of("DE", "LT")));

        Declaration declaration = builder.build(tax, ivafe, GoldenData.instruments(), countries);

        // Germany for the holdings of one broker, no code for VGWD, held at two
        assertThat(declaration.section("W").orElseThrow().items())
                .extracting(i -> i.detail().params().get(2))
                .contains("094", "");
        assertThat(declaration.warnings()).containsOnlyOnce(Msg.of("declaration.country_not_unique", "VGWD", "DE, LT"));
    }

    @Test
    void aMissingPriceIsReportedOncePerTicker() {
        TaxYearResult tax = taxCalculator.calculate(2026, GoldenData.operations(), GoldenData.instruments(), Map.of());
        IvafeResult ivafe = ivafeCalculator.calculate(2026, GoldenData.operations(), Map.of(), Map.of());

        Declaration declaration = builder.build(tax, ivafe, GoldenData.instruments(), Map.of());

        // VGWD has three holding periods in 2026, but one warning
        assertThat(declaration.warnings())
                .filteredOn(w -> w.code().equals("declaration.missing_year_end_price"))
                .extracting(w -> w.params().getFirst())
                .containsExactly("CEBS", "EUNK", "VGWD", "XEON");
    }
}
