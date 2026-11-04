package it.helper730.api.dto;

import java.time.Instant;
import java.util.List;

/**
 * ImportDtos.
 */
public final class ImportDtos {

    private ImportDtos() {}

    /**
     * RowIssueDto.
     */
    public record RowIssueDto(int row, String severity, String code, List<Object> params) {}

    /**
     * ImportResponse.
     */
    public record ImportResponse(long batchId, int imported, int duplicates, int errors, List<RowIssueDto> issues) {}

    /**
     * ImportBatchDto.
     */
    public record ImportBatchDto(
            long id, String filename, String broker, Instant importedAt, int imported, int duplicates, int errors) {}
}
