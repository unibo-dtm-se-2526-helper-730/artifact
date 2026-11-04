package it.helper730.service;

import it.helper730.domain.Instrument;
import it.helper730.persistence.EntityMapper;
import it.helper730.persistence.repository.InstrumentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InstrumentService {

    private final InstrumentRepository instruments;

    @Transactional(readOnly = true)
    public List<Instrument> list() {
        return instruments.findAllByOrderByTickerAsc().stream()
                .map(EntityMapper::toDomain)
                .toList();
    }

    /**
     * Creates or replaces the classification of an instrument. The domain
     * constructor validates it (ISIN, rates, fields allowed for the asset class)
     * before anything is saved.
     */
    @Transactional
    public Instrument save(Instrument instrument) {
        instruments.save(EntityMapper.toEntity(instrument));
        return instrument;
    }

    /**
     * Removes instruments no longer referenced by transactions or year-end prices.
     */
    @Transactional
    public int deleteUnused() {
        return instruments.deleteUnused();
    }
}
