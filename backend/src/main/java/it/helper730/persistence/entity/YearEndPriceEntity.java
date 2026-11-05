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
 * YearEndPriceEntity.
 *
 * Price of a ticker at 31/12 of a tax year, entered by the user for IVAFE.
 */
@Entity
@Table(name = "year_end_prices")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class YearEndPriceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int taxYear;
    private String ticker;
    private BigDecimal price;

    public YearEndPriceEntity(int taxYear, String ticker, BigDecimal price) {
        this.taxYear = taxYear;
        this.ticker = ticker;
        this.price = price;
    }
}
