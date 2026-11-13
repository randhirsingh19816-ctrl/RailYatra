package com.trainreservation.backend.repository;

import com.trainreservation.backend.entity.Seat;
import com.trainreservation.backend.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByCoachIdOrderBySeatNumberAsc(Long coachId);

    @Query("""
            select count(distinct seat.id)
            from Seat seat
            join seat.coach coach
            where coach.trainRun.id = :trainRunId
              and coach.trainClass.id = :trainClassId
            """)
    long countInventorySeats(@Param("trainRunId") Long trainRunId,
                             @Param("trainClassId") Long trainClassId);

    @Query("""
            select seat
            from Seat seat
            join seat.coach coach
            where coach.trainRun.id = :trainRunId
              and coach.trainClass.id = :trainClassId
              and not exists (
                  select allocation.id
                  from SeatAllocation allocation
                  join allocation.booking booking
                  where allocation.trainRun.id = :trainRunId
                    and allocation.seat.id = seat.id
                    and allocation.segmentNumber >= :requestedStart
                    and allocation.segmentNumber < :requestedEnd
                    and booking.bookingStatus in :activeBookingStatuses
              )
            order by coach.coachNumber, seat.seatNumber
            """)
    List<Seat> findAvailableSeatsForOverlappingSegments(
            @Param("trainRunId") Long trainRunId,
            @Param("trainClassId") Long trainClassId,
            @Param("requestedStart") Integer requestedStart,
            @Param("requestedEnd") Integer requestedEnd,
            @Param("activeBookingStatuses") Collection<BookingStatus> activeBookingStatuses,
            Pageable pageable);
}
