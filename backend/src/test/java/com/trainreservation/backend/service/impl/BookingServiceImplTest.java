package com.trainreservation.backend.service.impl;

import com.trainreservation.backend.dto.request.BookingPassengerRequest;
import com.trainreservation.backend.dto.request.CreateBookingRequest;
import com.trainreservation.backend.dto.response.BookingConfirmationResponse;
import com.trainreservation.backend.entity.Booking;
import com.trainreservation.backend.entity.Coach;
import com.trainreservation.backend.entity.Passenger;
import com.trainreservation.backend.entity.Payment;
import com.trainreservation.backend.entity.Seat;
import com.trainreservation.backend.entity.SeatAllocation;
import com.trainreservation.backend.entity.Station;
import com.trainreservation.backend.entity.Train;
import com.trainreservation.backend.entity.TrainClass;
import com.trainreservation.backend.entity.TrainRun;
import com.trainreservation.backend.entity.TrainStop;
import com.trainreservation.backend.entity.User;
import com.trainreservation.backend.enums.BookingStatus;
import com.trainreservation.backend.enums.Gender;
import com.trainreservation.backend.enums.PaymentStatus;
import com.trainreservation.backend.enums.RunningStatus;
import com.trainreservation.backend.enums.SeatType;
import com.trainreservation.backend.enums.TrainClassCode;
import com.trainreservation.backend.enums.TrainType;
import com.trainreservation.backend.enums.UserRole;
import com.trainreservation.backend.exception.BookingNotAuthorizedException;
import com.trainreservation.backend.exception.InsufficientSeatAvailabilityException;
import com.trainreservation.backend.exception.InvalidRequestException;
import com.trainreservation.backend.exception.InvalidRouteException;
import com.trainreservation.backend.exception.TrainRunNotFoundException;
import com.trainreservation.backend.exception.TrainNotOperatingException;
import com.trainreservation.backend.repository.BookingRepository;
import com.trainreservation.backend.repository.CancellationRepository;
import com.trainreservation.backend.repository.PassengerRepository;
import com.trainreservation.backend.repository.PaymentRepository;
import com.trainreservation.backend.repository.SeatAllocationRepository;
import com.trainreservation.backend.repository.SeatRepository;
import com.trainreservation.backend.repository.TrainClassRepository;
import com.trainreservation.backend.repository.TrainRunRepository;
import com.trainreservation.backend.repository.TrainStopRepository;
import com.trainreservation.backend.repository.UserRepository;
import com.trainreservation.backend.service.FareCalculationService;
import com.trainreservation.backend.service.CancellationRefundPolicy;
import com.trainreservation.backend.service.TrainRouteLookupService;
import com.trainreservation.backend.repository.StationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingServiceImplTest {

    private static final LocalDate JOURNEY_DATE = LocalDate.of(2026, 12, 1);
    private static final String USER_EMAIL = "alice@example.test";

    private BookingRepository bookingRepository;
    private CancellationRepository cancellationRepository;
    private UserRepository userRepository;
    private TrainRunRepository trainRunRepository;
    private TrainStopRepository trainStopRepository;
    private TrainClassRepository trainClassRepository;
    private SeatRepository seatRepository;
    private SeatAllocationRepository seatAllocationRepository;
    private PassengerRepository passengerRepository;
    private PaymentRepository paymentRepository;
    private StationRepository stationRepository;
    private BookingServiceImpl bookingService;
    private User user;
    private User admin;
    private TrainRun trainRun;
    private TrainClass trainClass;
    private Station source;
    private Station connection;
    private Station destination;
    private TrainStop sourceStop;
    private TrainStop destinationStop;
    private List<Seat> seats;
    private List<SeatAllocation> savedAllocations;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        cancellationRepository = mock(CancellationRepository.class);
        userRepository = mock(UserRepository.class);
        trainRunRepository = mock(TrainRunRepository.class);
        trainStopRepository = mock(TrainStopRepository.class);
        trainClassRepository = mock(TrainClassRepository.class);
        seatRepository = mock(SeatRepository.class);
        seatAllocationRepository = mock(SeatAllocationRepository.class);
        passengerRepository = mock(PassengerRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        stationRepository = mock(StationRepository.class);
        bookingService = new BookingServiceImpl(
                bookingRepository,
                cancellationRepository,
                userRepository,
                trainRunRepository,
                trainStopRepository,
                trainClassRepository,
                seatRepository,
                seatAllocationRepository,
                passengerRepository,
                paymentRepository,
                new TrainRouteLookupService(stationRepository),
                new FareCalculationService(new BigDecimal("1.25")),
                new CancellationRefundPolicy(new BigDecimal("0.80")));

        user = user(1L, UserRole.USER);
        admin = user(2L, UserRole.ADMIN);
        Train train = new Train();
        train.setId(10L);
        train.setTrainNumber("12001");
        train.setTrainName("Example Express");
        train.setTrainType(TrainType.EXPRESS);
        train.setActive(true);

        trainRun = new TrainRun();
        trainRun.setId(100L);
        trainRun.setTrain(train);
        trainRun.setJourneyDate(JOURNEY_DATE);
        trainRun.setRunningStatus(RunningStatus.SCHEDULED);

        trainClass = new TrainClass();
        trainClass.setId(50L);
        trainClass.setTrain(train);
        trainClass.setClassCode(TrainClassCode.SL);
        trainClass.setClassName("Sleeper");
        trainClass.setBaseFare(new BigDecimal("100.00"));

        source = station(1L, "NDLS");
        connection = station(2L, "CNB");
        destination = station(3L, "LKO");
        sourceStop = stop(source, 1, "0.00");
        TrainStop connectionStop = stop(connection, 2, "100.00");
        destinationStop = stop(destination, 3, "200.00");

        Coach coach = new Coach();
        coach.setId(20L);
        coach.setTrainRun(trainRun);
        coach.setTrainClass(trainClass);
        coach.setCoachNumber("S1");
        seats = List.of(seat(coach, 1L, "1"), seat(coach, 2L, "2"), seat(coach, 3L, "3"));

        when(userRepository.findByEmailIgnoreCase(USER_EMAIL)).thenReturn(Optional.of(user));
        when(trainRunRepository.findByIdForBooking(100L)).thenReturn(Optional.of(trainRun));
        when(trainStopRepository.findByTrainIdOrderByStopNumberAsc(10L))
                .thenReturn(List.of(sourceStop, connectionStop, destinationStop));
        when(trainClassRepository.findByTrainIdAndClassCode(10L, TrainClassCode.SL))
                .thenReturn(Optional.of(trainClass));
        when(stationRepository.findByStationCodeIgnoreCase("NDLS")).thenReturn(Optional.of(source));
        when(stationRepository.findByStationCodeIgnoreCase("CNB")).thenReturn(Optional.of(connection));
        when(stationRepository.findByStationCodeIgnoreCase("LKO")).thenReturn(Optional.of(destination));
        when(seatRepository.countInventorySeats(100L, 50L)).thenReturn(3L);
        when(seatAllocationRepository.countOccupiedSeatsForOverlappingSegments(
                100L, 50L, 1, 3, List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)))
                .thenReturn(0L);
        when(seatRepository.findAvailableSeatsForOverlappingSegments(
                eq(100L), eq(50L), eq(1), eq(3),
                eq(List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)),
                any(Pageable.class))).thenReturn(seats);
        when(bookingRepository.existsByPnrNumber(anyString())).thenReturn(false);
        when(bookingRepository.saveAndFlush(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(passengerRepository.saveAllAndFlush(any()))
                .thenAnswer(invocation -> {
                    List<Passenger> savedPassengers = invocation.getArgument(0);
                    return new ArrayList<>(savedPassengers);
                });
        when(seatAllocationRepository.saveAllAndFlush(any()))
                .thenAnswer(invocation -> {
                    Iterable<SeatAllocation> allocations = invocation.getArgument(0);
                    savedAllocations = new ArrayList<>();
                    allocations.forEach(savedAllocations::add);
                    return savedAllocations;
                });
        when(paymentRepository.saveAndFlush(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        authenticateAs(USER_EMAIL, "ROLE_USER");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsMultiPassengerBookingUsingAuthenticatedUserAndBackendFare() {
        BookingConfirmationResponse response = bookingService.createBooking(request("NDLS", "LKO", 2));

        assertEquals(BookingStatus.PENDING, response.bookingStatus());
        assertEquals(PaymentStatus.PENDING, response.paymentStatus());
        assertEquals(new BigDecimal("350.00"), response.farePerPassenger());
        assertEquals(new BigDecimal("700.00"), response.totalFare());
        assertEquals(2, response.passengers().size());
        assertEquals("S1", response.passengers().get(0).coachNumber());
        assertNotEquals(response.passengers().get(0).seatNumber(), response.passengers().get(1).seatNumber());
        assertTrue(response.pnrNumber().matches("[2-9A-HJ-NP-Z]{16}"));

        org.mockito.ArgumentCaptor<Booking> bookingCaptor = org.mockito.ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).saveAndFlush(bookingCaptor.capture());
        assertEquals(user, bookingCaptor.getValue().getUser());
        assertEquals(new BigDecimal("700.00"), bookingCaptor.getValue().getTotalFare());

        org.mockito.ArgumentCaptor<Payment> paymentCaptor = org.mockito.ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).saveAndFlush(paymentCaptor.capture());
        assertEquals(PaymentStatus.PENDING, paymentCaptor.getValue().getPaymentStatus());
        assertEquals(new BigDecimal("700.00"), paymentCaptor.getValue().getAmount());

        verify(seatAllocationRepository).saveAllAndFlush(any());
        assertEquals(4, savedAllocations.size());
    }

    @Test
    void generatesDifferentPnrValuesForSeparateBookings() {
        BookingConfirmationResponse first = bookingService.createBooking(request("NDLS", "LKO", 1));
        BookingConfirmationResponse second = bookingService.createBooking(request("NDLS", "LKO", 1));

        assertNotEquals(first.pnrNumber(), second.pnrNumber());
    }

    @Test
    void rejectsUnknownTrainRun() {
        when(trainRunRepository.findByIdForBooking(100L)).thenReturn(Optional.empty());

        assertThrows(TrainRunNotFoundException.class,
                () -> bookingService.createBooking(request("NDLS", "LKO", 1)));
        verify(bookingRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsPastJourneyDate() {
        CreateBookingRequest pastJourney = new CreateBookingRequest(
                100L, "NDLS", "LKO", LocalDate.now().minusDays(1), "SL",
                List.of(new BookingPassengerRequest("Passenger", 30, Gender.OTHER)));

        assertThrows(InvalidRequestException.class, () -> bookingService.createBooking(pastJourney));
        verify(trainRunRepository, never()).findByIdForBooking(any());
    }

    @Test
    void rejectsBookingAfterTheTrainDepartedFromTheRequestedSource() {
        trainRun.setJourneyDate(LocalDate.now());
        sourceStop.setDepartureTime(LocalTime.MIN);
        CreateBookingRequest today = new CreateBookingRequest(
                100L, "NDLS", "LKO", LocalDate.now(), "SL",
                List.of(new BookingPassengerRequest("Passenger", 30, Gender.OTHER)));

        assertThrows(TrainNotOperatingException.class, () -> bookingService.createBooking(today));
        verify(bookingRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsBookingWhenDestinationArrivalIsUnscheduled() {
        destinationStop.setArrivalTime(null);

        assertThrows(TrainNotOperatingException.class,
                () -> bookingService.createBooking(request("NDLS", "LKO", 1)));
        verify(bookingRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsDestinationBeforeSource() {
        assertThrows(InvalidRouteException.class,
                () -> bookingService.createBooking(request("LKO", "NDLS", 1)));
        verify(bookingRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsPassengerCountAboveAvailability() {
        when(seatRepository.countInventorySeats(100L, 50L)).thenReturn(1L);
        when(seatAllocationRepository.countOccupiedSeatsForOverlappingSegments(
                100L, 50L, 1, 3, List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)))
                .thenReturn(0L);

        assertThrows(InsufficientSeatAvailabilityException.class,
                () -> bookingService.createBooking(request("NDLS", "LKO", 2)));
        verify(bookingRepository, never()).saveAndFlush(any());
    }

    @Test
    void cannotCreateBookingUsingAdminIdentity() {
        when(userRepository.findByEmailIgnoreCase(USER_EMAIL)).thenReturn(Optional.of(admin));

        assertThrows(BookingNotAuthorizedException.class,
                () -> bookingService.createBooking(request("NDLS", "LKO", 1)));
        verify(trainRunRepository, never()).findByIdForBooking(any());
    }

    private CreateBookingRequest request(String sourceCode, String destinationCode, int count) {
        List<BookingPassengerRequest> passengers = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            passengers.add(new BookingPassengerRequest("Passenger " + index, 30, Gender.OTHER));
        }
        return new CreateBookingRequest(100L, sourceCode, destinationCode, JOURNEY_DATE, "SL", passengers);
    }

    private void authenticateAs(String email, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        email, "unused", List.of(new SimpleGrantedAuthority(role))));
    }

    private User user(Long id, UserRole role) {
        User entity = new User();
        entity.setId(id);
        entity.setEmail(USER_EMAIL);
        entity.setRole(role);
        return entity;
    }

    private Station station(Long id, String code) {
        Station station = new Station();
        station.setId(id);
        station.setStationCode(code);
        station.setStationName(code + " station");
        station.setCity("Test City");
        station.setState("Test State");
        return station;
    }

    private TrainStop stop(Station station, int number, String distance) {
        TrainStop stop = new TrainStop();
        stop.setStation(station);
        stop.setStopNumber(number);
        stop.setDayNumber(1);
        stop.setArrivalTime(LocalTime.of(10, number));
        stop.setDepartureTime(LocalTime.of(10, number + 1));
        stop.setDistanceFromSource(new BigDecimal(distance));
        return stop;
    }

    private Seat seat(Coach coach, Long id, String number) {
        Seat seat = new Seat();
        seat.setId(id);
        seat.setCoach(coach);
        seat.setSeatNumber(number);
        seat.setSeatType(SeatType.LOWER);
        return seat;
    }
}
