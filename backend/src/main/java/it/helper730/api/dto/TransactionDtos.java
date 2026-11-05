package it.helper730.api.dto;

import it.helper730.domain.OperationType;
import java.time.Instant;

/**
 * TransactionDtos.
 */
public final class TransactionDtos {

    private TransactionDtos() {}

    /**
     * TransactionDto.
     *
     * Response body for GET /transactions/{id}.
     */
    public record TransactionDto(
            String id,
            Instant executedAt,
            int taxYear,
            OperationType type,
            String ticker,
            String quantity,
            String pricePerShare,
            String totalAmount,
            String currency,
            int fileRow) {}

    /**
     * DeleteResponse.
     *
     * Response body for DELETE /transactions.
     */
    public record DeleteResponse(int transactions, int imports, int instruments) {}
}
