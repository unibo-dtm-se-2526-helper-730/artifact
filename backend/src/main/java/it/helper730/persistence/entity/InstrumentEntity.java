package it.helper730.persistence.entity;

import it.helper730.domain.AssetClass;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * InstrumentEntity.
 *
 * Instrument registry row. {@code assetClass} is null until the user classifies
 * the ticker.
 */
@Entity
@Table(name = "instruments")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class InstrumentEntity {

    @Id
    private String ticker;

    private String isin;
    private String name;

    @Enumerated(EnumType.STRING)
    private AssetClass assetClass;

    private BigDecimal governmentBondShare;
    private BigDecimal foreignWithholdingRate;
}
