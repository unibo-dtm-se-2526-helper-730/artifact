package it.helper730.persistence.entity.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import it.helper730.persistence.entity.CarriedLossEntity;

/**
 * CarriedLossRepository.
 * 
 * Repository for the carried losses.
 */
public interface CarriedLossRepository extends JpaRepository<CarriedLossEntity, Long> {

    List<CarriedLossEntity> findAllByOrderByLossYearAsc();
}