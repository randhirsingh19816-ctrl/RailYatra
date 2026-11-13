package com.trainreservation.backend.repository;

import com.trainreservation.backend.entity.Booking;
import com.trainreservation.backend.entity.Coach;
import com.trainreservation.backend.entity.Passenger;
import com.trainreservation.backend.entity.Seat;
import com.trainreservation.backend.entity.SeatAllocation;
import com.trainreservation.backend.entity.Station;
import com.trainreservation.backend.entity.Train;
import com.trainreservation.backend.entity.TrainClass;
import com.trainreservation.backend.entity.TrainRun;
import com.trainreservation.backend.entity.User;
import com.trainreservation.backend.enums.BookingStatus;
import com.trainreservation.backend.enums.Gender;
import com.trainreservation.backend.enums.PaymentStatus;
import com.trainreservation.backend.enums.RunningStatus;
import com.trainreservation.backend.enums.SeatType;
import com.trainreservation.backend.enums.TrainClassCode;
import com.trainreservation.backend.enums.TrainType;
import com.trainreservation.backend.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SeatAllocationRepositoryPostgresTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private TrainRepository trainRepository;

    @Autowired
    private TrainClassRepository trainClassRepository;

    @Autowired
    private TrainRunRepository trainRunRepository;

    @Autowired
    private CoachRepository coachRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PassengerRepository passengerRepository;

    @Autowired
    private SeatAllocationRepository seatAllocationRepository;

    @Test
    void countsOnlyDistinctSeatsOnOverlappingSegmentsWithActiveBookings() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        Station source = station("S" + suffix + "A");
        Station destination = station("S" + suffix + "B");
        stationRepository.saveAllAndFlush(List.of(source, destination));

        Train train = new Train();
        train.setTrainNumber("T" + suffix);
        train.setTrainName("Allocation Test Train");
        train.setTrainType(TrainType.EXPRESS);
        train.setActive(true);
        trainRepository.saveAndFlush(train);

        TrainClass trainClass = new TrainClass();
        trainClass.setTrain(train);
        trainClass.setClassCode(TrainClassCode.SL);
        trainClass.setClassName("Sleeper");
        trainClass.setBaseFare(new BigDecimal("100.00"));
        trainClassRepository.saveAndFlush(trainClass);

        TrainRun trainRun = new TrainRun();
        trainRun.setTrain(train);
        trainRun.setJourneyDate(LocalDate.of(2026, 12, 1));
        trainRun.setRunningStatus(RunningStatus.SCHEDULED);
        trainRunRepository.saveAndFlush(trainRun);

        Coach coach = new Coach();
        coach.setTrainRun(trainRun);
        coach.setTrainClass(trainClass);
        coach.setCoachNumber("S1");
        coachRepository.saveAndFlush(coach);

        Seat confirmedSeat = seat(coach, "1");
        Seat cancelledBookingSeat = seat(coach, "2");
        seatRepository.saveAllAndFlush(List.of(confirmedSeat, cancelledBookingSeat));

        User user = new User();
        user.setFullName("Allocation Test User");
        user.setEmail("allocation-" + suffix.toLowerCase() + "@example.test");
        user.setPhoneNumber("5551234567");
        user.setPassword(new BCryptPasswordEncoder().encode("integration-password"));
        user.setRole(UserRole.USER);
        userRepository.saveAndFlush(user);

        Booking activeBooking = booking("P" + suffix + "A", user, trainRun, source, destination,
                BookingStatus.CONFIRMED);
        Booking cancelledBooking = booking("P" + suffix + "B", user, trainRun, source, destination,
                BookingStatus.CANCELLED);
        bookingRepository.saveAllAndFlush(List.of(activeBooking, cancelledBooking));

        Passenger activePassenger = passenger(activeBooking);
        Passenger cancelledPassenger = passenger(cancelledBooking);
        passengerRepository.saveAllAndFlush(List.of(activePassenger, cancelledPassenger));

        seatAllocationRepository.saveAllAndFlush(List.of(
                allocation(trainRun, confirmedSeat, activeBooking, activePassenger, 1),
                allocation(trainRun, confirmedSeat, activeBooking, activePassenger, 2),
                allocation(trainRun, cancelledBookingSeat, cancelledBooking, cancelledPassenger, 1)));

        assertEquals(2L, seatRepository.countInventorySeats(trainRun.getId(), trainClass.getId()));
        assertEquals(1L, seatAllocationRepository.countOccupiedSeatsForOverlappingSegments(
                trainRun.getId(), trainClass.getId(), 1, 3,
                List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)));
        assertEquals(0L, seatAllocationRepository.countOccupiedSeatsForOverlappingSegments(
                trainRun.getId(), trainClass.getId(), 3, 4,
                List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)));
    }

    private Station station(String code) {
        Station station = new Station();
        station.setStationCode(code);
        station.setStationName("Station " + code);
        station.setCity("Test City");
        station.setState("Test State");
        return station;
    }

    private Seat seat(Coach coach, String number) {
        Seat seat = new Seat();
        seat.setCoach(coach);
        seat.setSeatNumber(number);
        seat.setSeatType(SeatType.LOWER);
        return seat;
    }

    private Booking booking(String pnr, User user, TrainRun trainRun, Station source,
                            Station destination, BookingStatus status) {
        Booking booking = new Booking();
        booking.setPnrNumber(pnr);
        booking.setUser(user);
        booking.setTrainRun(trainRun);
        booking.setSourceStation(source);
        booking.setDestinationStation(destination);
        booking.setSourceStopNumber(1);
        booking.setDestinationStopNumber(3);
        booking.setBookingDate(LocalDateTime.now());
        booking.setTotalFare(new BigDecimal("100.00"));
        booking.setBookingStatus(status);
        booking.setPaymentStatus(PaymentStatus.PAID);
        return booking;
    }

    private Passenger passenger(Booking booking) {
        Passenger passenger = new Passenger();
        passenger.setBooking(booking);
        passenger.setFullName("Allocation Test Passenger");
        passenger.setAge(30);
        passenger.setGender(Gender.OTHER);
        return passenger;
    }

    private SeatAllocation allocation(TrainRun trainRun, Seat seat, Booking booking,
                                      Passenger passenger, int segmentNumber) {
        SeatAllocation allocation = new SeatAllocation();
        allocation.setTrainRun(trainRun);
        allocation.setSeat(seat);
        allocation.setBooking(booking);
        allocation.setPassenger(passenger);
        allocation.setSegmentNumber(segmentNumber);
        return allocation;
    }
}
