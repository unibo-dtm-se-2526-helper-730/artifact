package it.helper730.service;

import it.helper730.domain.Msg;
import it.helper730.domain.exceptions.InvalidDataException;
import it.helper730.persistence.entity.CarriedLossEntity;
import it.helper730.persistence.repository.CarriedLossRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Year;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Losses realized before the first import: the opening balance of the Zainetto.
 */
@Service
@RequiredArgsConstructor
public class CarriedLossService {

    private final CarriedLossRepository carriedLosses;
    private final Clock clock;

    /**
     * CarriedLoss.
     * A loss as entered by the user: year of realization and positive amount.
     */
    public record CarriedLoss(int lossYear, BigDecimal amount) {}

    @Transactional(readOnly = true)
    public List<CarriedLoss> list() {
        return carriedLosses.findAllByOrderByLossYearAsc().stream()
                .map(e -> new CarriedLoss(e.lossYear(), e.amount()))
                .toList();
    }

    /**
     * Replaces the whole list. Years must be unique and not in the future; amounts
     * positive.
     */
    @Transactional
    public List<CarriedLoss> replace(List<CarriedLoss> losses) {
        int currentYear = Year.now(clock).getValue();
        Set<Integer> seen = new HashSet<>();
        for (CarriedLoss loss : losses) {
            if (loss.amount() == null || loss.amount().signum() <= 0) {
                throw new InvalidDataException(Msg.of("error.carried_loss_not_positive", loss.lossYear()));
            }
            if (loss.lossYear() > currentYear) {
                throw new InvalidDataException(Msg.of("error.carried_loss_future_year", loss.lossYear()));
            }
            if (!seen.add(loss.lossYear())) {
                throw new InvalidDataException(Msg.of("error.carried_loss_duplicate_year", loss.lossYear()));
            }
        }

        carriedLosses.deleteAllInBatch();
        carriedLosses.saveAll(losses.stream()
                .map(l -> new CarriedLossEntity(l.lossYear(), l.amount()))
                .toList());
        return list();
    }
}
