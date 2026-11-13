package com.trainreservation.backend.repository;

import com.trainreservation.backend.entity.TrainClass;
import com.trainreservation.backend.enums.TrainClassCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrainClassRepository extends JpaRepository<TrainClass, Long> {

    List<TrainClass> findByTrainId(Long trainId);

    Optional<TrainClass> findByTrainIdAndClassCode(Long trainId, TrainClassCode classCode);
}
