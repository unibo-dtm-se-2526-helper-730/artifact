package it.helper730.persistence.repository;

import it.helper730.persistence.entity.TransactionEntity;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * TransactionRepository.
 *
 * Repository for the transactions.
 */
public interface TransactionRepository extends JpaRepository<TransactionEntity, String> {

    List<TransactionEntity> findAllByOrderByExecutedAtAscIdAsc();

    /**
     * Transactions in [from, to). The caller computes the bounds of a tax year in
     * Europe/Rome.
     */
    @Query("""
            select t from TransactionEntity t
            where t.executedAt >= :from and t.executedAt < :to
            order by t.executedAt, t.id
            """)
    List<TransactionEntity> findInPeriod(@Param("from") Instant from, @Param("to") Instant to);

    @Query("select distinct t.ticker from TransactionEntity t where t.ticker is not null")
    Set<String> findDistinctTickers();

    /** Pairs of ticker and broker it was imported from. */
    @Query("""
            select distinct t.ticker as ticker, b.broker as broker
            from TransactionEntity t join t.batch b
            where t.ticker is not null
            """)
    List<TickerBroker> findTickerBrokers();

    /**
     * TickerBroker.
     *
     * A ticker and the broker of one of its imports.
     */
    interface TickerBroker {
        String getTicker();

        String getBroker();
    }
}
