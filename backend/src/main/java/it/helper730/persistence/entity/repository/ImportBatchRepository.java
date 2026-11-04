package it.helper730.persistence.entity.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import it.helper730.persistence.entity.ImportBatchEntity;

/**
 * ImportBatchRepository
 * 
 * Repository for the import batch registry.
 */
public interface ImportBatchRepository extends JpaRepository<ImportBatchEntity, Long> {

    List<ImportBatchEntity> findAllByOrderByImportedAtDesc();
}