package it.helper730.reporting;

import it.helper730.domain.AssetClass;
import it.helper730.domain.EventType;
import it.helper730.domain.IncomeCategory;
import it.helper730.domain.Instrument;
import it.helper730.domain.LotConsumption;
import it.helper730.domain.Money;
import it.helper730.domain.Msg;
import it.helper730.domain.TaxYearResult;
import it.helper730.domain.TaxableEvent;
import it.helper730.domain.ivafe.IvafeResult;
import it.helper730.domain.ivafe.IvafeRow;
import it.helper730.domain.zainetto.CapitalLoss;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Builds the Modello 730 values (quadri T, M and W) from the tax and IVAFE
 * results of a year.
 */
public final class DeclarationBuilder {

    private static final BigDecimal FULL_RATE = new BigDecimal("26");
    private static final BigDecimal REDUCED_RATE = new BigDecimal("12.5");

    /**
     * Builds the declaration.
     *
     * @param tax           tax result of the year
     * @param ivafe         IVAFE result of the same year
     * @param instruments   instruments by ticker, for codes and countries
     * @param brokerCountry ISO 3166 alpha-2 code of the country where the
     *                      securities are held
     */
    public Declaration build(
            TaxYearResult tax,
            IvafeResult ivafe,
            Map<String, Instrument> instruments,
            Map<String, Set<String>> countriesByTicker) {
        if (tax.year() != ivafe.year()) {
            throw new IllegalArgumentException("Tax and IVAFE results of different years");
        }
        Mappings.Layout layout = Mappings.forYear(tax.year());
        Warnings warnings = new Warnings();
        List<DeclarationSection> sections = new ArrayList<>();

        addIfNotEmpty(sections, "T", "declaration.quadro_t", quadroT(tax, layout));
        addIfNotEmpty(sections, "M", "declaration.quadro_m", quadroM(tax, layout, instruments, warnings));
        addIfNotEmpty(sections, "W", "declaration.quadro_w", quadroW(ivafe, layout, countriesByTicker, warnings));

        tax.excludedEvents().forEach(e -> warnings.addOnce("declaration.excluded_instrument", e.ticker()));
        return new Declaration(tax.year(), Mappings.modelName(tax.year()), sections, warnings.list());
    }

    /**
     * Quadro T: proceeds and cost of the sales giving miscellaneous income, losses
     * of previous years.
     */
    private static List<DeclarationItem> quadroT(TaxYearResult tax, Mappings.Layout layout) {
        List<DeclarationItem> items = new ArrayList<>();
        BigDecimal proceeds = BigDecimal.ZERO;
        BigDecimal cost = BigDecimal.ZERO;
        boolean anySale = false;
        for (TaxableEvent event : tax.events()) {
            if (event.category() != IncomeCategory.MISCELLANEOUS) {
                continue;
            }
            anySale = true;
            // amounts of government bonds are written at 48.08%, like their gains and
            // losses
            for (LotConsumption c : event.consumptions()) {
                proceeds = proceeds.add(c.proceeds().multiply(event.weight()));
                cost = cost.add(c.cost().multiply(event.weight()));
            }
        }
        if (anySale) {
            items.add(new DeclarationItem(
                    "t11_proceeds",
                    Msg.of("declaration.t11.proceeds"),
                    Money.round(proceeds),
                    layout.proceedsRigo(),
                    null,
                    null));
            items.add(new DeclarationItem(
                    "t11_cost", Msg.of("declaration.t11.cost"), Money.round(cost), layout.costRigo(), null, null));
        }

        int firstColumnYear = tax.year() - 4;
        for (CapitalLoss loss : tax.zainetto().opening()) {
            int column = Math.max(loss.year() - firstColumnYear + 1, 1);
            items.add(new DeclarationItem(
                    "t102_" + loss.year(),
                    Msg.of("declaration.t102.loss", loss.year()),
                    loss.remaining(),
                    layout.carriedLossesRigo() + " col. " + column,
                    Msg.of("declaration.t102.detail", loss.year(), loss.lastUsableYear()),
                    null));
        }
        return items;
    }

    /**
     * Quadro M: capital income grouped by income code, state of origin and rate.
     * Income from a UCITS ETF is split between 26% and 12.5% according to its share
     * of government bonds.
     */
    private static List<DeclarationItem> quadroM(
            TaxYearResult tax, Mappings.Layout layout, Map<String, Instrument> instruments, Warnings warnings) {
        Map<String, Line> lines = new LinkedHashMap<>();
        for (TaxableEvent event : tax.events()) {
            if (event.category() != IncomeCategory.CAPITAL) {
                continue;
            }
            Instrument instrument = Objects.requireNonNull(instruments.get(event.ticker()), event.ticker());
            String state = stateOf(instrument, warnings);
            String code = incomeCode(instrument.assetClass(), event.type());
            BigDecimal reducedShare = reducedRateShare(instrument);
            BigDecimal reduced = Money.round(event.amount().multiply(reducedShare));
            BigDecimal full = event.amount().subtract(reduced);
            if (full.signum() > 0) {
                line(lines, code, state, FULL_RATE).add(full, event.foreignTax());
            }
            if (reduced.signum() > 0) {
                line(lines, code, state, REDUCED_RATE).add(reduced, BigDecimal.ZERO);
            }
        }
        List<DeclarationItem> items = new ArrayList<>();
        for (Line l : lines.values()) {
            items.add(new DeclarationItem(
                    "m31_" + l.code + "_" + l.state + "_"
                            + l.rate.stripTrailingZeros().toPlainString(),
                    Msg.of("declaration.m31." + l.code),
                    Money.round(l.amount),
                    layout.capitalIncomeRigo(),
                    Msg.of(
                            "declaration.m31.detail",
                            l.code,
                            l.state,
                            l.rate.stripTrailingZeros().toPlainString()),
                    l.foreignTax.signum() > 0 ? Money.round(l.foreignTax) : null));
        }
        return items;
    }

    private static List<DeclarationItem> quadroW(
            IvafeResult ivafe, Mappings.Layout layout, Map<String, Set<String>> countriesByTicker, Warnings warnings) {
        List<DeclarationItem> items = new ArrayList<>();
        int rigo = 1;
        for (IvafeRow row : ivafe.rows()) {
            if (!row.isComputed()) {
                // one warning per ticker: a ticker can have several holding periods
                warnings.addOnce("declaration.missing_year_end_price", row.ticker());
                continue;
            }
            String state = stateCode(row.ticker(), countriesByTicker, warnings);
            items.add(new DeclarationItem(
                    "w" + rigo,
                    Msg.of("declaration.w.holding", row.ticker(), row.from(), row.to()),
                    row.finalValue(),
                    layout.foreignAssetsPrefix() + rigo,
                    Msg.of(
                            "declaration.w.detail",
                            Mappings.W_OWNERSHIP,
                            Mappings.W_SECURITIES,
                            state,
                            Mappings.W_MARKET_VALUE,
                            row.holdingDays(),
                            row.ivafe()),
                    row.initialValue()));
            rigo++;
        }
        return items;
    }

    /**
     * State code of the country where a ticker is held, from the broker it was
     * bought at. Empty, with a warning, when it cannot be told: unknown country,
     * or the same ticker held at brokers of different countries.
     */
    private static String stateCode(String ticker, Map<String, Set<String>> countriesByTicker, Warnings warnings) {
        Set<String> countries = countriesByTicker.getOrDefault(ticker, Set.of());
        if (countries.size() != 1) {
            warnings.addOnce("declaration.country_not_unique", ticker, String.join(", ", countries));
            return "";
        }
        String country = countries.iterator().next();
        String state = Mappings.stateCode(country).orElse("");
        if (state.isEmpty()) {
            warnings.addOnce("declaration.unknown_state", country);
        }
        return state;
    }

    private static String incomeCode(AssetClass assetClass, EventType type) {
        return switch (assetClass) {
            case UCITS_ETF -> Mappings.CODE_FUND_INCOME;
            case STOCK -> type == EventType.DIVIDEND ? Mappings.CODE_FOREIGN_DIVIDENDS : Mappings.CODE_BOND_INTEREST;
            case BOND, GOVERNMENT_BOND_WL, GOVERNMENT_BOND_NON_WL, ETC_ETN, NON_UCITS_ETF ->
                Mappings.CODE_BOND_INTEREST;
        };
    }

    /** Share of the income taxed at 12.5%. */
    private static BigDecimal reducedRateShare(Instrument instrument) {
        return switch (instrument.assetClass()) {
            case GOVERNMENT_BOND_WL -> BigDecimal.ONE;
            case UCITS_ETF ->
                instrument.governmentBondShare() == null ? BigDecimal.ZERO : instrument.governmentBondShare();
            default -> BigDecimal.ZERO;
        };
    }

    /** State of origin of the income, from the first two letters of the ISIN. */
    private static String stateOf(Instrument instrument, Warnings warnings) {
        String iso = instrument.isin() == null ? null : instrument.isin().substring(0, 2);
        return Mappings.stateCode(iso).orElseGet(() -> {
            warnings.addOnce("declaration.unknown_state_of_origin", instrument.ticker());
            return "";
        });
    }

    private static Line line(Map<String, Line> lines, String code, String state, BigDecimal rate) {
        return lines.computeIfAbsent(code + "|" + state + "|" + rate, k -> new Line(code, state, rate));
    }

    private static void addIfNotEmpty(
            List<DeclarationSection> sections, String quadro, String title, List<DeclarationItem> items) {
        if (!items.isEmpty()) {
            sections.add(new DeclarationSection(quadro, Msg.of(title), items));
        }
    }

    /**
     * Warnings of a declaration, each one added once: a ticker can have several
     * holding periods and several events. Duplicates are told by code and
     * parameters rather than by {@link Msg#equals}, which may not compare the
     * parameters by value.
     */
    private static final class Warnings {
        private final List<Msg> list = new ArrayList<>();
        private final Set<String> seen = new HashSet<>();

        void addOnce(String code, Object... params) {
            if (seen.add(code + Arrays.deepToString(params))) {
                list.add(Msg.of(code, params));
            }
        }

        List<Msg> list() {
            return List.copyOf(list);
        }
    }

    /** Running total of one rigo M31: same code, state and rate. */
    private static final class Line {
        private final String code;
        private final String state;
        private final BigDecimal rate;
        private BigDecimal amount = BigDecimal.ZERO;
        private BigDecimal foreignTax = BigDecimal.ZERO;

        Line(String code, String state, BigDecimal rate) {
            this.code = code;
            this.state = state;
            this.rate = rate;
        }

        void add(BigDecimal value, BigDecimal tax) {
            amount = amount.add(value);
            foreignTax = foreignTax.add(tax);
        }
    }
}
