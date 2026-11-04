package it.helper730.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * CarriedLossEntity.
 * 
 * Loss realized in a year before the first import, entered manually.
 */
@Entity
@Table(name = "carried_losses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CarriedLossEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int lossYear;
    private BigDecimal amount;

    public CarriedLossEntity(int lossYear, BigDecimal amount) {
        this.lossYear = lossYear;
        this.amount = amount;
    }
}