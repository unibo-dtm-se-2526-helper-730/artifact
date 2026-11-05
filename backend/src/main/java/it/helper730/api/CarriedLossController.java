package it.helper730.api;

import it.helper730.api.dto.CarriedLossDto;
import it.helper730.api.dto.Decimals;
import it.helper730.service.CarriedLossService;
import it.helper730.service.CarriedLossService.CarriedLoss;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/carried-losses")
@RequiredArgsConstructor
public class CarriedLossController {

    private final CarriedLossService carriedLossService;

    @GetMapping
    public List<CarriedLossDto> list() {
        return toDto(carriedLossService.list());
    }

    @PutMapping
    public List<CarriedLossDto> replace(@RequestBody List<CarriedLossDto> body) {
        List<CarriedLoss> losses = body.stream()
                .map(d -> new CarriedLoss(d.lossYear(), d.amount() == null ? null : new BigDecimal(d.amount())))
                .toList();
        return toDto(carriedLossService.replace(losses));
    }

    private static List<CarriedLossDto> toDto(List<CarriedLoss> losses) {
        return losses.stream()
                .map(l -> new CarriedLossDto(l.lossYear(), Decimals.str(l.amount())))
                .toList();
    }
}
