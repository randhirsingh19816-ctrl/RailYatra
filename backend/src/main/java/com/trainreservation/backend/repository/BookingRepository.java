package com.trainreservation.backend.repository;

import com.trainreservation.backend.entity.Booking;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByPnrNumber(String pnrNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select booking from Booking booking where booking.pnrNumber = :pnrNumber")
    Optional<Booking> findByPnrNumberForUpdate(@Param("pnrNumber") String pnrNumber);

    @Query("select booking.trainRun.id from Booking booking where booking.pnrNumber = :pnrNumber")
    Optional<Long> findTrainRunIdByPnrNumber(@Param("pnrNumber") String pnrNumber);

    boolean existsByPnrNumber(String pnrNumber);

    List<Booking> findByUserIdOrderByBookingDateDesc(Long userId);

    Page<Booking> findByUserIdOrderByBookingDateDesc(Long userId, Pageable pageable);
}
