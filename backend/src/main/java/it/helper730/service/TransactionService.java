package it.helper730.service;

import it.helper730.domain.Transaction;
import it.helper730.persistence.EntityMapper;
import it.helper730.persistence.repository.ImportBatchRepository;
import it.helper730.persistence.repository.InstrumentRepository;
import it.helper730.persistence.repository.TransactionRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactions;
    private final ImportBatchRepository batches;
    private final InstrumentRepository instruments;

    /**
     * All transactions, or those of one tax year.
     */
    @Transactional(readOnly = true)
    public List<Transaction> list(Integer year) {
        var entities = year == null
                ? transactions.findAllByOrderByExecutedAtAscIdAsc()
                : transactions.findInPeriod(startOf(year), startOf(year + 1));
        return entities.stream().map(EntityMapper::toDomain).toList();
    }

    /**
     * Deletes every transaction and the import history; optionally also the
     * instruments left unused.
     * Transactions go first: without ON DELETE CASCADE the batches cannot be
     * removed while referenced.
     */
    @Transactional
    public DeleteResult deleteAll(boolean includeInstruments) {
        int tx = (int) transactions.count();
        int imports = (int) batches.count();
        transactions.deleteAllInBatch();
        batches.deleteAllInBatch();
        int removed = includeInstruments ? instruments.deleteUnused() : 0;
        return new DeleteResult(tx, imports, removed);
    }

    private static Instant startOf(int year) {
        return LocalDate.of(year, 1, 1).atStartOfDay(Transaction.TAX_ZONE).toInstant();
    }

    public record DeleteResult(int transactions, int imports, int instruments) {}
}
