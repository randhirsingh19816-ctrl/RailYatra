package com.trainreservation.backend.repository;

import com.trainreservation.backend.entity.TrainStop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrainStopRepository extends JpaRepository<TrainStop, Long> {

    List<TrainStop> findByTrainIdOrderByStopNumberAsc(Long trainId);

    boolean existsByTrainId(Long trainId);

    Optional<TrainStop> findByIdAndTrainId(Long id, Long trainId);

    Optional<TrainStop> findByTrainIdAndStationId(Long trainId, Long stationId);

    Optional<TrainStop> findByTrainIdAndStopNumber(Long trainId, Integer stopNumber);
}
