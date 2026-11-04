package it.helper730.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ImportBatchEntity.
 * 
 * One uploaded CSV file and its outcome counters.
 */
@Entity
@Table(name = "import_batches")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImportBatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String filename;
    private String broker;
    private Instant importedAt;
    private int imported;
    private int duplicates;
    private int errors;

    public ImportBatchEntity(
            String filename, String broker, Instant importedAt, int imported, int duplicates, int errors) {
        this.filename = filename;
        this.broker = broker;
        this.importedAt = importedAt;
        this.imported = imported;
        this.duplicates = duplicates;
        this.errors = errors;
    }
}