package it.helper730.persistence.repository;

import it.helper730.persistence.entity.ImportBatchEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * ImportBatchRepository.
 *
 * Repository for the import batch registry.
 */
public interface ImportBatchRepository extends JpaRepository<ImportBatchEntity, Long> {

    List<ImportBatchEntity> findAllByOrderByImportedAtDesc();
}
