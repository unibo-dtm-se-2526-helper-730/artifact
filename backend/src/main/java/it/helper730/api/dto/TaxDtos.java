package it.helper730.api.dto;

import it.helper730.domain.AssetClass;
import it.helper730.domain.EventType;
import it.helper730.domain.IncomeCategory;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * TaxDtos.
 *
 * Response bodies of the tax, IVAFE, declaration and portfolio endpoints.
 * Amounts are strings, like every BigDecimal in the API.
 */
public final class TaxDtos {

    private TaxDtos() {}

    /**
     * MsgDto.
     *
     */
    public record MsgDto(String code, List<Object> params, String text) {}

    // --- /tax/years ---------------------------------------------------------

    /**
     * YearSummaryDto.
     *
     * Totals are null when the year cannot be computed yet; errorCode says why.
     */
    public record YearSummaryDto(
            int year, String totalTax, String zainettoTotal, Integer eventCount, String errorCode) {}

    // --- /tax/{year} --------------------------------------------------------

    /**
     * ConsumptionDto.
     *
     * Shares of one purchase lot consumed by a sale.
     */
    public record ConsumptionDto(
            String buyTransactionId,
            Instant acquiredAt,
            String quantity,
            String cost,
            String cashCost,
            String proceeds,
            String gain) {}

    /**
     * EventDto.
     *
     * One taxable event, with the lots it consumed (empty for dividends and
     * coupons).
     */
    public record EventDto(
            String transactionId,
            Instant executedAt,
            String ticker,
            EventType type,
            IncomeCategory category,
            String amount,
            String weight,
            String foreignTax,
            String taxableAmount,
            List<ConsumptionDto> consumptions) {}

    /**
     * LossDto.
     *
     * A loss in the Zainetto.
     */
    public record LossDto(int year, String amount, String remaining, int lastUsableYear) {}

    /**
     * CompensationDto.
     *
     * Part of a loss used in the year.
     */
    public record CompensationDto(int lossYear, String used) {}

    /**
     * ZainettoDto.
     *
     * Zainetto movements of the year; closingTotal is what is carried to the next
     * year.
     */
    public record ZainettoDto(
            List<LossDto> opening,
            List<LossDto> expired,
            String gains,
            String losses,
            List<CompensationDto> compensations,
            LossDto realizedLoss,
            List<LossDto> closing,
            String openingTotal,
            String usedTotal,
            String closingTotal) {}

    /**
     * TaxYearDto.
     *
     * Full result of a tax year.
     */
    public record TaxYearDto(
            int year,
            String economicResult,
            String miscellaneousTaxable,
            String capitalTaxable,
            String rate,
            String miscellaneousTax,
            String capitalTax,
            String totalTax,
            String paymentThreshold,
            boolean belowThreshold,
            String payableTax,
            ZainettoDto zainetto,
            List<EventDto> events) {}

    // --- /tax/{year}/ivafe --------------------------------------------------

    /**
     * IvafeRowDto.
     *
     * IVAFE of one lot. ivafe is null when the price at 31/12 is missing.
     */
    public record IvafeRowDto(
            String ticker,
            String quantity,
            LocalDate from,
            LocalDate to,
            boolean sold,
            String yearEndPrice,
            String initialValue,
            String finalValue,
            int holdingDays,
            String ivafe,
            List<MsgDto> notes) {}

    /**
     * IvafeDto.
     *
     * IVAFE of the year. total ignores the rows without a price: complete is false
     * and missingPrices lists the tickers whose price at 31/12 must be entered.
     */
    public record IvafeDto(
            int year,
            String rate,
            String total,
            String paymentThreshold,
            boolean belowThreshold,
            boolean complete,
            List<String> missingPrices,
            List<IvafeRowDto> rows) {}

    // --- /tax/{year}/declaration --------------------------------------------

    /**
     * DeclarationItemDto.
     *
     * One value to write in the declaration; detail and secondary may be null.
     */
    public record DeclarationItemDto(
            String key, MsgDto description, String amount, String rigo, MsgDto detail, String secondary) {}

    /**
     * DeclarationSectionDto.
     *
     * The values of one quadro (T, M or W).
     */
    public record DeclarationSectionDto(String quadro, MsgDto title, List<DeclarationItemDto> items) {}

    /**
     * DeclarationDto.
     *
     * Modello 730 summary.
     */
    public record DeclarationDto(int year, String model, List<DeclarationSectionDto> sections, List<MsgDto> warnings) {}

    // --- /portfolio ---------------------------------------------------------

    /**
     * PositionDto.
     *
     * An open position; weight is the share of the portfolio cost, between 0 and 1.
     */
    public record PositionDto(
            String ticker,
            String name,
            AssetClass assetClass,
            String quantity,
            String cost,
            String taxCost,
            String averageCost,
            String weight) {}

    /**
     * AssetClassWeightDto.
     *
     * Cost and weight of one asset class; assetClass is null for unclassified
     * instruments.
     */
    public record AssetClassWeightDto(AssetClass assetClass, String cost, String weight) {}

    /**
     * PortfolioDto.
     *
     * Holdings at cost.
     */
    public record PortfolioDto(List<PositionDto> positions, List<AssetClassWeightDto> byAssetClass, String totalCost) {}
}
