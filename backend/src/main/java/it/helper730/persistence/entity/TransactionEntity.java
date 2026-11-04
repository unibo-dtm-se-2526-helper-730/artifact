package it.helper730.persistence.entity;

import it.helper730.domain.OperationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

/**
 * TransactionEntity.
 * 
 * A broker transaction. The id is assigned by the application (SHA-256 of the
 * normalized row), so the entity implements {@link Persistable} to tell Spring
 * Data when it is new (and then do an insert, update otherwise).
 */
@Entity
@Table(name = "transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransactionEntity implements Persistable<String> {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 64)
    private String id;

    private Instant executedAt;

    @Enumerated(EnumType.STRING)
    private OperationType operationType;

    private String ticker;
    private BigDecimal quantity;
    private BigDecimal pricePerShare;
    private BigDecimal totalAmount;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 3)
    private String currency;
    private int fileRow;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id")
    private ImportBatchEntity batch;

    @Transient
    @Getter(AccessLevel.NONE)
    private boolean fresh = true;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return fresh;
    }

    @PostLoad
    @PostPersist
    void markPersisted() {
        fresh = false;
    }

    public TransactionEntity(
            Instant executedAt,
            OperationType operationType,
            String ticker,
            BigDecimal quantity,
            BigDecimal pricePerShare,
            BigDecimal totalAmount,
            String currency,
            int fileRow) {
        this.executedAt = executedAt;
        this.operationType = operationType;
        this.ticker = ticker;
        this.quantity = quantity;
        this.pricePerShare = pricePerShare;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.fileRow = fileRow;
    }
}