package it.helper730.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import it.helper730.api.dto.Decimals;
import it.helper730.api.dto.TransactionDtos.DeleteResponse;
import it.helper730.api.dto.TransactionDtos.TransactionDto;
import it.helper730.domain.Transaction;
import it.helper730.service.TransactionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Stored operations, by tax year")
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public List<TransactionDto> list(@RequestParam(required = false) Integer year) {
        return transactionService.list(year).stream()
                .map(TransactionController::toDto)
                .toList();
    }

    @DeleteMapping
    public DeleteResponse deleteAll(@RequestParam(defaultValue = "false") boolean instruments) {
        var r = transactionService.deleteAll(instruments);
        return new DeleteResponse(r.transactions(), r.imports(), r.instruments());
    }

    private static TransactionDto toDto(Transaction t) {
        return new TransactionDto(
                t.id(),
                t.executedAt(),
                t.taxYear(),
                t.type(),
                t.ticker(),
                Decimals.str(t.quantity()),
                Decimals.str(t.pricePerShare()),
                Decimals.str(t.totalAmount()),
                t.currency(),
                t.fileRow());
    }
}
