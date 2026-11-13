package com.trainreservation.backend.repository;

import com.trainreservation.backend.entity.Train;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TrainRepository extends JpaRepository<Train, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select train from Train train where train.id = :trainId")
    Optional<Train> findByIdForRouteUpdate(@Param("trainId") Long trainId);

    Optional<Train> findByTrainNumber(String trainNumber);

    boolean existsByTrainNumberIgnoreCase(String trainNumber);

    boolean existsByTrainNumberIgnoreCaseAndIdNot(String trainNumber, Long id);

    Page<Train> findByActive(Boolean active, Pageable pageable);

    Page<Train> findByTrainNumberContainingIgnoreCaseOrTrainNameContainingIgnoreCase(
            String trainNumber, String trainName, Pageable pageable);
}
