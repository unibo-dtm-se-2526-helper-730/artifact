package it.helper730.api;

import static it.helper730.api.dto.Decimals.str;

import io.swagger.v3.oas.annotations.tags.Tag;
import it.helper730.api.dto.TaxDtos.DeclarationDto;
import it.helper730.api.dto.TaxDtos.IvafeDto;
import it.helper730.api.dto.TaxDtos.TaxYearDto;
import it.helper730.api.dto.TaxDtos.YearSummaryDto;
import it.helper730.domain.TaxYearResult;
import it.helper730.domain.exceptions.DomainException;
import it.helper730.service.TaxService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TaxController.
 *
 * Substitute tax, Zainetto, IVAFE and Modello 730 summary of a tax year.
 */
@RestController
@RequestMapping("/api/v1/tax")
@RequiredArgsConstructor
@Tag(name = "Tax", description = "Substitute tax, Zainetto, IVAFE and Modello 730 summary")
public class TaxController {

    private final TaxService taxService;
    private final TaxDtoMapper mapper;

    /**
     * One summary per year. A year that cannot be computed yet (e.g. unclassified
     * instruments) still appears, with null totals and the error code: the year
     * menu must work even before the user has classified everything.
     */
    @GetMapping("/years")
    public List<YearSummaryDto> years() {
        return taxService.years().stream().map(this::summary).toList();
    }

    @GetMapping("/{year}")
    public TaxYearDto tax(@PathVariable int year) {
        return mapper.toDto(taxService.tax(year));
    }

    @GetMapping("/{year}/ivafe")
    public IvafeDto ivafe(@PathVariable int year) {
        return mapper.toDto(taxService.ivafe(year));
    }

    @GetMapping("/{year}/declaration")
    public DeclarationDto declaration(@PathVariable int year) {
        return mapper.toDto(taxService.declaration(year));
    }

    private YearSummaryDto summary(int year) {
        try {
            TaxYearResult r = taxService.tax(year);
            return new YearSummaryDto(
                    year,
                    str(r.totalTax()),
                    str(r.zainetto().closingTotal()),
                    r.events().size(),
                    null);
        } catch (DomainException e) {
            return new YearSummaryDto(year, null, null, null, e.code());
        }
    }
}
