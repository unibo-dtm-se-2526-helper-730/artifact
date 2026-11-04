package it.helper730.persistence;

import it.helper730.domain.Instrument;
import it.helper730.domain.Transaction;
import it.helper730.persistence.entity.ImportBatchEntity;
import it.helper730.persistence.entity.InstrumentEntity;
import it.helper730.persistence.entity.TransactionEntity;

/**
 * EntityMapper.
 *
 * Converts between domain records and JPA entities. Domain validation runs
 * again on the way back.
 */
public final class EntityMapper {

    private EntityMapper() {}

    public static TransactionEntity toEntity(Transaction t, ImportBatchEntity batch) {
        return new TransactionEntity(
                t.id(),
                t.executedAt(),
                t.type(),
                t.ticker(),
                t.quantity(),
                t.pricePerShare(),
                t.totalAmount(),
                t.currency(),
                t.fileRow(),
                batch);
    }

    public static Transaction toDomain(TransactionEntity e) {
        return new Transaction(
                e.getId(),
                e.getExecutedAt(),
                e.getOperationType(),
                e.getTicker(),
                e.getQuantity(),
                e.getPricePerShare(),
                e.getTotalAmount(),
                e.getCurrency(),
                e.getFileRow());
    }

    public static InstrumentEntity toEntity(Instrument i) {
        return new InstrumentEntity(
                i.ticker(), i.isin(), i.name(), i.assetClass(), i.governmentBondShare(), i.foreignWithholdingRate());
    }

    public static Instrument toDomain(InstrumentEntity e) {
        return new Instrument(
                e.getTicker(),
                e.getIsin(),
                e.getName(),
                e.getAssetClass(),
                e.getGovernmentBondShare(),
                e.getForeignWithholdingRate());
    }
}
