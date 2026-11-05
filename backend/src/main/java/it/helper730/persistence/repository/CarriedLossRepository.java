package it.helper730.persistence.repository;

import it.helper730.persistence.entity.CarriedLossEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * CarriedLossRepository.
 *
 * Repository for the carried losses.
 */
public interface CarriedLossRepository extends JpaRepository<CarriedLossEntity, Long> {

    List<CarriedLossEntity> findAllByOrderByLossYearAsc();
}
