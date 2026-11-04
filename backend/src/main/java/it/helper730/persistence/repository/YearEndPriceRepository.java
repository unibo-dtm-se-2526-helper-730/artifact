package it.helper730.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.helper730.persistence.entity.YearEndPriceEntity;

/**
 * YearEndPriceRepository.
 * 
 * Repository for the year-end prices.
 */
public interface YearEndPriceRepository extends JpaRepository<YearEndPriceEntity, Long> {

    List<YearEndPriceEntity> findByTaxYearOrderByTickerAsc(int taxYear);

    @Modifying
    @Query("delete from YearEndPriceEntity p where p.taxYear = :year")
    int deleteByTaxYear(@Param("year") int year);
}