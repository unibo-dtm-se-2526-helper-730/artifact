package it.helper730.api;

import static it.helper730.api.dto.Decimals.str;

import it.helper730.api.dto.TaxDtos.AssetClassWeightDto;
import it.helper730.api.dto.TaxDtos.CompensationDto;
import it.helper730.api.dto.TaxDtos.ConsumptionDto;
import it.helper730.api.dto.TaxDtos.DeclarationDto;
import it.helper730.api.dto.TaxDtos.DeclarationItemDto;
import it.helper730.api.dto.TaxDtos.DeclarationSectionDto;
import it.helper730.api.dto.TaxDtos.EventDto;
import it.helper730.api.dto.TaxDtos.IvafeDto;
import it.helper730.api.dto.TaxDtos.IvafeRowDto;
import it.helper730.api.dto.TaxDtos.LossDto;
import it.helper730.api.dto.TaxDtos.MsgDto;
import it.helper730.api.dto.TaxDtos.PortfolioDto;
import it.helper730.api.dto.TaxDtos.PositionDto;
import it.helper730.api.dto.TaxDtos.TaxYearDto;
import it.helper730.api.dto.TaxDtos.ZainettoDto;
import it.helper730.domain.LotConsumption;
import it.helper730.domain.TaxYearResult;
import it.helper730.domain.TaxableEvent;
import it.helper730.domain.Transaction;
import it.helper730.domain.ivafe.IvafeResult;
import it.helper730.domain.ivafe.IvafeRow;
import it.helper730.domain.portfolio.AssetClassWeight;
import it.helper730.domain.portfolio.Portfolio;
import it.helper730.domain.portfolio.Position;
import it.helper730.domain.zainetto.CapitalLoss;
import it.helper730.domain.zainetto.ZainettoYear;
import it.helper730.reporting.Declaration;
import it.helper730.reporting.DeclarationItem;
import it.helper730.reporting.DeclarationSection;
import java.util.List;

/**
 * TaxDtoMapper.
 *
 * Converts engine and reporting results into API responses. Totals come from
 * the domain records (closingTotal, total, belowThreshold...): the API never
 * recomputes them.
 */
final class TaxDtoMapper {

    private TaxDtoMapper() {}

    // --- tax ----------------------------------------------------------------

    static TaxYearDto toDto(TaxYearResult r) {
        return new TaxYearDto(
                r.year(),
                str(r.economicResult()),
                str(r.miscellaneousTaxable()),
                str(r.capitalTaxable()),
                str(r.rate()),
                str(r.miscellaneousTax()),
                str(r.capitalTax()),
                str(r.totalTax()),
                str(r.paymentThreshold()),
                r.belowThreshold(),
                str(r.payableTax()),
                toDto(r.zainetto()),
                r.events().stream().map(TaxDtoMapper::toDto).toList());
    }

    private static ZainettoDto toDto(ZainettoYear z) {
        return new ZainettoDto(
                losses(z.opening()),
                losses(z.expired()),
                str(z.gains()),
                str(z.losses()),
                z.compensations().stream()
                        .map(c -> new CompensationDto(c.lossYear(), str(c.used())))
                        .toList(),
                z.realizedLoss() == null ? null : toDto(z.realizedLoss()),
                losses(z.closing()),
                str(z.openingTotal()),
                str(z.used()),
                str(z.closingTotal()));
    }

    private static List<LossDto> losses(List<CapitalLoss> losses) {
        return losses.stream().map(TaxDtoMapper::toDto).toList();
    }

    private static LossDto toDto(CapitalLoss l) {
        return new LossDto(l.year(), str(l.amount()), str(l.remaining()), l.lastUsableYear());
    }

    private static EventDto toDto(TaxableEvent e) {
        Transaction t = e.source();
        return new EventDto(
                t.id(),
                t.executedAt(),
                e.ticker(),
                e.type(),
                e.category(),
                str(e.amount()),
                str(e.weight()),
                str(e.foreignTax()),
                str(e.taxableAmount()),
                e.consumptions().stream().map(TaxDtoMapper::toDto).toList());
    }

    private static ConsumptionDto toDto(LotConsumption c) {
        return new ConsumptionDto(
                c.buyTransactionId(),
                c.acquiredAt(),
                str(c.quantity()),
                str(c.cost()),
                str(c.cashCost()),
                str(c.proceeds()),
                str(c.gain()));
    }

    // --- IVAFE --------------------------------------------------------------

    static IvafeDto toDto(IvafeResult r) {
        return new IvafeDto(
                r.year(),
                str(r.rate()),
                str(r.total()),
                str(r.paymentThreshold()),
                r.belowThreshold(),
                r.isComplete(),
                r.missingPrices(),
                r.rows().stream().map(TaxDtoMapper::toDto).toList());
    }

    private static IvafeRowDto toDto(IvafeRow r) {
        return new IvafeRowDto(
                r.ticker(),
                str(r.quantity()),
                r.from(),
                r.to(),
                r.sold(),
                str(r.yearEndPrice()),
                str(r.initialValue()),
                str(r.finalValue()),
                r.holdingDays(),
                str(r.ivafe()),
                r.notes().stream().map(MsgDto::of).toList());
    }

    // --- declaration --------------------------------------------------------

    static DeclarationDto toDto(Declaration d) {
        return new DeclarationDto(
                d.year(),
                d.model(),
                d.sections().stream().map(TaxDtoMapper::toDto).toList(),
                d.warnings().stream().map(MsgDto::of).toList());
    }

    private static DeclarationSectionDto toDto(DeclarationSection s) {
        return new DeclarationSectionDto(
                s.quadro(),
                MsgDto.of(s.title()),
                s.items().stream().map(TaxDtoMapper::toDto).toList());
    }

    private static DeclarationItemDto toDto(DeclarationItem i) {
        return new DeclarationItemDto(
                i.key(),
                MsgDto.of(i.description()),
                str(i.amount()),
                i.rigo(),
                MsgDto.of(i.detail()),
                str(i.secondary()));
    }

    // --- portfolio ----------------------------------------------------------

    static PortfolioDto toDto(Portfolio p) {
        return new PortfolioDto(
                p.positions().stream().map(TaxDtoMapper::toDto).toList(),
                p.byAssetClass().stream().map(TaxDtoMapper::toDto).toList(),
                str(p.totalCost()));
    }

    private static PositionDto toDto(Position x) {
        return new PositionDto(
                x.ticker(),
                x.name(),
                x.assetClass(),
                str(x.quantity()),
                str(x.cost()),
                str(x.taxCost()),
                str(x.averageCost()),
                str(x.weight()));
    }

    private static AssetClassWeightDto toDto(AssetClassWeight w) {
        return new AssetClassWeightDto(w.assetClass(), str(w.cost()), str(w.weight()));
    }
}
