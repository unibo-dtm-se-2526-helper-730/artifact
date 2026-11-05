package it.helper730.service;

import it.helper730.domain.Instrument;
import it.helper730.domain.Transaction;
import it.helper730.ingestion.BrokerParserRegistry;
import it.helper730.ingestion.ParseResult;
import it.helper730.ingestion.RowIssue;
import it.helper730.ingestion.Severity;
import it.helper730.persistence.EntityMapper;
import it.helper730.persistence.entity.ImportBatchEntity;
import it.helper730.persistence.repository.ImportBatchRepository;
import it.helper730.persistence.repository.InstrumentRepository;
import it.helper730.persistence.repository.TransactionRepository;
import java.io.InputStream;
import java.time.Clock;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ImportService.
 *
 * Imports a broker CSV: parse, validate, drop duplicates, register new
 * instruments, save.
 */
@Service
@RequiredArgsConstructor
public class ImportService {

    private final BrokerParserRegistry parsers;
    private final TransactionRepository transactions;
    private final InstrumentRepository instruments;
    private final ImportBatchRepository batches;
    private final Clock clock;

    @Transactional
    public ImportReport importCsv(String filename, InputStream content) {
        ParseResult parsed = parsers.parse(content);
        List<RowIssue> issues = new ArrayList<>(parsed.issues());

        // 1. Validation
        List<Transaction> valid = parsed.transactions();

        // 2. Duplicates: already in the database, or repeated within the same file.
        Set<String> alreadyStored = transactions
                .findAllById(valid.stream().map(Transaction::id).toList())
                .stream()
                .map(t -> t.getId())
                .collect(Collectors.toSet());
        Map<String, Transaction> fresh = new LinkedHashMap<>();
        int duplicates = 0;
        for (Transaction t : valid) {
            if (alreadyStored.contains(t.id()) || fresh.putIfAbsent(t.id(), t) != null) {
                duplicates++;
            }
        }

        // 3. New tickers become unclassified instruments (required by the foreign key).
        Set<String> tickers = fresh.values().stream()
                .map(Transaction::ticker)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
        instruments.findAllById(tickers).forEach(known -> tickers.remove(known.ticker()));
        instruments.saveAll(tickers.stream()
                .map(Instrument::unclassified)
                .map(EntityMapper::toEntity)
                .toList());

        // 4. Batch and transactions.
        int errorRows = (int) issues.stream()
                .filter(i -> i.severity() == Severity.ERROR)
                .map(RowIssue::fileRow)
                .distinct()
                .count();
        ImportBatchEntity batch = batches.save(
                new ImportBatchEntity(filename, parsed.broker(), clock.instant(), fresh.size(), duplicates, errorRows));
        transactions.saveAll(fresh.values().stream()
                .map(t -> EntityMapper.toEntity(t, batch))
                .toList());

        issues.sort(Comparator.comparingInt(RowIssue::fileRow));
        return new ImportReport(batch.id(), fresh.size(), duplicates, errorRows, List.copyOf(issues));
    }

    @Transactional(readOnly = true)
    public List<ImportBatchEntity> history() {
        return batches.findAllByOrderByImportedAtDesc();
    }
}
