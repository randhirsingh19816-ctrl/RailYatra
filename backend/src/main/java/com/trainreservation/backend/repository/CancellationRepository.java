package com.trainreservation.backend.repository;

import com.trainreservation.backend.entity.Cancellation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CancellationRepository extends JpaRepository<Cancellation, Long> {

    Optional<Cancellation> findByBookingId(Long bookingId);

    boolean existsByBookingId(Long bookingId);
}
