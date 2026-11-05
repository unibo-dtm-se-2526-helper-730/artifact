package it.helper730.api;

import it.helper730.api.dto.Decimals;
import it.helper730.api.dto.InstrumentDtos.InstrumentDto;
import it.helper730.api.dto.InstrumentDtos.InstrumentRequest;
import it.helper730.domain.Instrument;
import it.helper730.service.InstrumentService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/instruments")
@RequiredArgsConstructor
public class InstrumentController {

    private final InstrumentService instrumentService;

    @GetMapping()
    public List<InstrumentDto> list() {
        return instrumentService.list().stream()
                .map(InstrumentController::toDto)
                .toList();
    }

    @PutMapping("/{ticker}")
    public InstrumentDto save(@PathVariable String ticker, @RequestBody InstrumentRequest body) {
        Instrument instrument = new Instrument(
                ticker,
                body.isin(),
                body.name(),
                body.assetClass(),
                body.governmentBondShare(),
                body.foreignWithholdingRate());
        return toDto(instrumentService.save(instrument));
    }

    @DeleteMapping
    public Map<String, Integer> deleteUnused() {
        return Map.of("deleted", instrumentService.deleteUnused());
    }

    private static InstrumentDto toDto(Instrument i) {
        return new InstrumentDto(
                i.ticker(),
                i.isin(),
                i.name(),
                i.assetClass(),
                Decimals.str(i.governmentBondShare()),
                Decimals.str(i.foreignWithholdingRate()),
                i.isClassified());
    }
}
