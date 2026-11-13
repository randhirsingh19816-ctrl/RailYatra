package com.trainreservation.backend.repository;

import com.trainreservation.backend.entity.SeatAllocation;
import com.trainreservation.backend.enums.BookingStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SeatAllocationRepository extends JpaRepository<SeatAllocation, Long> {

    List<SeatAllocation> findByTrainRunIdAndBookingId(Long trainRunId, Long bookingId);

    boolean existsByTrainRunIdAndSeatIdAndSegmentNumber(Long trainRunId, Long seatId, Integer segmentNumber);

    @Modifying
    @Query("""
            delete from SeatAllocation allocation
            where allocation.trainRun.id = :trainRunId
              and allocation.seat.coach.trainClass.id = :trainClassId
              and allocation.segmentNumber >= :requestedStart
              and allocation.segmentNumber < :requestedEnd
              and allocation.booking.id in (
                  select booking.id
                  from Booking booking
                  where booking.bookingStatus not in :activeBookingStatuses
              )
            """)
    int deleteInactiveAllocationsForOverlappingSegments(
            @Param("trainRunId") Long trainRunId,
            @Param("trainClassId") Long trainClassId,
            @Param("requestedStart") Integer requestedStart,
            @Param("requestedEnd") Integer requestedEnd,
            @Param("activeBookingStatuses") Collection<BookingStatus> activeBookingStatuses);

    @Modifying
    @Query("""
            delete from SeatAllocation allocation
            where allocation.trainRun.id = :trainRunId
              and allocation.booking.id = :bookingId
            """)
    int deleteByTrainRunIdAndBookingId(
            @Param("trainRunId") Long trainRunId,
            @Param("bookingId") Long bookingId);

    @Query("""
            select count(distinct allocation.seat.id)
            from SeatAllocation allocation
            join allocation.seat seat
            join seat.coach coach
            join allocation.booking booking
            where allocation.trainRun.id = :trainRunId
              and booking.trainRun.id = :trainRunId
              and coach.trainRun.id = :trainRunId
              and coach.trainClass.id = :trainClassId
              and allocation.segmentNumber >= :requestedStart
              and allocation.segmentNumber < :requestedEnd
              and booking.bookingStatus in :activeBookingStatuses
            """)
    long countOccupiedSeatsForOverlappingSegments(
            @Param("trainRunId") Long trainRunId,
            @Param("trainClassId") Long trainClassId,
            @Param("requestedStart") Integer requestedStart,
            @Param("requestedEnd") Integer requestedEnd,
            @Param("activeBookingStatuses") Collection<BookingStatus> activeBookingStatuses);
}
