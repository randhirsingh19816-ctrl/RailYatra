package com.trainreservation.backend.repository;

import com.trainreservation.backend.entity.Coach;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CoachRepository extends JpaRepository<Coach, Long> {

    List<Coach> findByTrainRunIdAndTrainClassId(Long trainRunId, Long trainClassId);
}
