package it.helper730.api;

import it.helper730.api.dto.ImportDtos.*;
import it.helper730.service.ImportReport;
import it.helper730.service.ImportService;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/imports")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ImportResponse upload(@RequestParam("file") MultipartFile file) throws IOException {
        try (InputStream in = file.getInputStream()) {
            return toResponse(importService.importCsv(file.getOriginalFilename(), in));
        }
    }

    @GetMapping
    public List<ImportBatchDto> history() {
        return importService.history().stream()
                .map(b -> new ImportBatchDto(
                        b.getId(),
                        b.getFilename(),
                        b.getBroker(),
                        b.getImportedAt(),
                        b.getImported(),
                        b.getDuplicates(),
                        b.getErrors()))
                .toList();
    }

    private static ImportResponse toResponse(ImportReport r) {
        List<RowIssueDto> issues = r.issues().stream()
                .map(i -> new RowIssueDto(
                        i.fileRow(),
                        i.severity().name(),
                        i.msg().code(),
                        i.msg().params()))
                .toList();
        return new ImportResponse(r.batchId(), r.imported(), r.duplicates(), r.errors(), issues);
    }
}
