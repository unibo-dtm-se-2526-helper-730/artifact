package it.helper730.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import it.helper730.api.dto.TaxDtos.PortfolioDto;
import it.helper730.service.TaxService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PortfolioController.
 *
 * Current holdings at cost, by instrument and by asset class.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Portfolio", description = "Current holdings at cost")
public class PortfolioController {

    private final TaxService taxService;
    private final TaxDtoMapper mapper;

    @GetMapping("/api/v1/portfolio")
    public PortfolioDto portfolio() {
        return mapper.toDto(taxService.portfolio());
    }
}
