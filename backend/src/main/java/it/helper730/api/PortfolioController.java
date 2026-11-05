package it.helper730.api;

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
public class PortfolioController {

    private final TaxService taxService;

    @GetMapping("/api/v1/portfolio")
    public PortfolioDto portfolio() {
        return TaxDtoMapper.toDto(taxService.portfolio());
    }
}
