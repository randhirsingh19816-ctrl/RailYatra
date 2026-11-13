package com.trainreservation.backend.repository;

import com.trainreservation.backend.entity.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PassengerRepository extends JpaRepository<Passenger, Long> {

    List<Passenger> findByBookingId(Long bookingId);

    long countByBookingId(Long bookingId);
}
