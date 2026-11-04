package it.helper730.persistence.repository;

import it.helper730.persistence.entity.InstrumentEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * InstrumentRepository.
 *
 * Repository for the instrument registry.
 */
public interface InstrumentRepository extends JpaRepository<InstrumentEntity, String> {

    List<InstrumentEntity> findAllByOrderByTickerAsc();

    /**
     * Deletes instruments no longer referenced by any transaction. Returns how many
     * were removed.
     */
    @Modifying
    @Query("""
            delete from InstrumentEntity i
            where i.ticker not in (select t.ticker from TransactionEntity t where t.ticker is not null)
            """)
    int deleteUnused();
}
