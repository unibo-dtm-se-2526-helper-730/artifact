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
import it.helper730.domain.Msg;
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
import it.helper730.i18n.Messages;
import it.helper730.reporting.Declaration;
import it.helper730.reporting.DeclarationItem;
import it.helper730.reporting.DeclarationSection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * TaxDtoMapper.
 *
 * Converts engine and reporting results into API responses. Totals come from
 * the domain records (closingTotal, total, belowThreshold...): the API never
 * recomputes them. Messages are translated into the language of the request.
 */
@Component
@RequiredArgsConstructor
class TaxDtoMapper {

    private final Messages messages;

    // --- tax ----------------------------------------------------------------

    TaxYearDto toDto(TaxYearResult r) {
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
                r.events().stream().map(this::toDto).toList());
    }

    private ZainettoDto toDto(ZainettoYear z) {
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

    private List<LossDto> losses(List<CapitalLoss> losses) {
        return losses.stream().map(this::toDto).toList();
    }

    private LossDto toDto(CapitalLoss l) {
        return new LossDto(l.year(), str(l.amount()), str(l.remaining()), l.lastUsableYear());
    }

    private EventDto toDto(TaxableEvent e) {
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
                e.consumptions().stream().map(this::toDto).toList());
    }

    private ConsumptionDto toDto(LotConsumption c) {
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

    IvafeDto toDto(IvafeResult r) {
        return new IvafeDto(
                r.year(),
                str(r.rate()),
                str(r.total()),
                str(r.paymentThreshold()),
                r.belowThreshold(),
                r.isComplete(),
                r.missingPrices(),
                r.rows().stream().map(this::toDto).toList());
    }

    private IvafeRowDto toDto(IvafeRow r) {
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
                r.notes().stream().map(this::msg).toList());
    }

    // --- declaration --------------------------------------------------------

    DeclarationDto toDto(Declaration d) {
        return new DeclarationDto(
                d.year(),
                d.model(),
                d.sections().stream().map(this::toDto).toList(),
                d.warnings().stream().map(this::msg).toList());
    }

    private DeclarationSectionDto toDto(DeclarationSection s) {
        return new DeclarationSectionDto(
                s.quadro(), msg(s.title()), s.items().stream().map(this::toDto).toList());
    }

    private DeclarationItemDto toDto(DeclarationItem i) {
        return new DeclarationItemDto(
                i.key(), msg(i.description()), str(i.amount()), i.rigo(), msg(i.detail()), str(i.secondary()));
    }

    // --- portfolio ----------------------------------------------------------

    PortfolioDto toDto(Portfolio p) {
        return new PortfolioDto(
                p.positions().stream().map(this::toDto).toList(),
                p.byAssetClass().stream().map(this::toDto).toList(),
                str(p.totalCost()));
    }

    private PositionDto toDto(Position x) {
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

    private AssetClassWeightDto toDto(AssetClassWeight w) {
        return new AssetClassWeightDto(w.assetClass(), str(w.cost()), str(w.weight()));
    }

    // --- messages -----------------------------------------------------------

    /**
     * Code and parameters for the client logic, text for the user; null stays null.
     */
    private MsgDto msg(Msg m) {
        return m == null ? null : new MsgDto(m.code(), m.params(), messages.text(m));
    }
}
