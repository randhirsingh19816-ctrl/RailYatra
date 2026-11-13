package com.trainreservation.backend.repository;

import com.trainreservation.backend.entity.TrainRun;
import com.trainreservation.backend.enums.RunningStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TrainRunRepository extends JpaRepository<TrainRun, Long> {

    boolean existsByTrainId(Long trainId);

    Optional<TrainRun> findByTrainIdAndJourneyDate(Long trainId, LocalDate journeyDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select trainRun from TrainRun trainRun where trainRun.id = :trainRunId")
    Optional<TrainRun> findByIdForBooking(@Param("trainRunId") Long trainRunId);

    List<TrainRun> findByJourneyDateAndRunningStatusOrderByTrainIdAsc(LocalDate journeyDate,
                                                                       RunningStatus runningStatus);

    List<TrainRun> findByJourneyDateAndRunningStatusInOrderByTrainIdAsc(
            LocalDate journeyDate, Collection<RunningStatus> runningStatuses);
}
