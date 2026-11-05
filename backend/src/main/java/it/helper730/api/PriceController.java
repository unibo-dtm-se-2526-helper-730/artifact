package it.helper730.api;

import it.helper730.api.dto.Decimals;
import it.helper730.api.dto.PriceDtos.PricesDto;
import it.helper730.api.dto.PriceDtos.PricesRequest;
import it.helper730.service.PriceService;
import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/prices")
@RequiredArgsConstructor
public class PriceController {

    private final PriceService priceService;

    @GetMapping("/{year}")
    public PricesDto get(@PathVariable int year) {
        return toDto(year, priceService.get(year));
    }

    @PutMapping("/{year}")
    public PricesDto replace(@PathVariable int year, @RequestBody PricesRequest body) {
        return toDto(year, priceService.replace(year, body.prices()));
    }

    private static PricesDto toDto(int year, Map<String, BigDecimal> prices) {
        Map<String, String> out = new TreeMap<>();
        prices.forEach((ticker, price) -> out.put(ticker, Decimals.str(price)));
        return new PricesDto(year, out);
    }
}
