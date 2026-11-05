package it.helper730.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import it.helper730.api.dto.CarriedLossDtos.CarriedLossDto;
import it.helper730.api.dto.CarriedLossDtos.CarriedLossRequest;
import it.helper730.api.dto.Decimals;
import it.helper730.service.CarriedLossService;
import it.helper730.service.CarriedLossService.CarriedLoss;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/carried-losses")
@RequiredArgsConstructor
@Tag(name = "Carried losses", description = "Losses from years before the first import")
public class CarriedLossController {

    private final CarriedLossService carriedLossService;

    @GetMapping
    public List<CarriedLossDto> list() {
        return toDto(carriedLossService.list());
    }

    @PutMapping
    public List<CarriedLossDto> replace(@RequestBody List<CarriedLossRequest> body) {
        List<CarriedLoss> losses = body.stream()
                .map(r -> new CarriedLoss(r.lossYear(), r.amount()))
                .toList();
        return toDto(carriedLossService.replace(losses));
    }

    private static List<CarriedLossDto> toDto(List<CarriedLoss> losses) {
        return losses.stream()
                .map(l -> new CarriedLossDto(l.lossYear(), Decimals.str(l.amount())))
                .toList();
    }
}
