package com.trainreservation.backend.service;

import com.trainreservation.backend.dto.request.BookingPassengerRequest;
import com.trainreservation.backend.dto.request.CreateBookingRequest;
import com.trainreservation.backend.dto.response.BookingConfirmationResponse;
import com.trainreservation.backend.dto.response.TrainAvailabilityResponse;
import com.trainreservation.backend.dto.response.BookingHistoryPageResponse;
import com.trainreservation.backend.dto.response.CancellationResponse;
import com.trainreservation.backend.dto.response.TrainSearchResponse;
import com.trainreservation.backend.entity.Booking;
import com.trainreservation.backend.entity.Cancellation;
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
import com.trainreservation.backend.enums.CancellationStatus;
import com.trainreservation.backend.enums.Gender;
import com.trainreservation.backend.enums.PaymentStatus;
import com.trainreservation.backend.enums.RunningStatus;
import com.trainreservation.backend.enums.SeatType;
import com.trainreservation.backend.enums.TrainClassCode;
import com.trainreservation.backend.enums.TrainType;
import com.trainreservation.backend.enums.UserRole;
import com.trainreservation.backend.exception.BookingAccessDeniedException;
import com.trainreservation.backend.exception.BookingAlreadyCancelledException;
import com.trainreservation.backend.exception.BookingNotFoundException;
import com.trainreservation.backend.exception.CancellationNotPermittedException;
import com.trainreservation.backend.repository.BookingRepository;
import com.trainreservation.backend.repository.CancellationRepository;
import com.trainreservation.backend.repository.CoachRepository;
import com.trainreservation.backend.repository.PassengerRepository;
import com.trainreservation.backend.repository.PaymentRepository;
import com.trainreservation.backend.repository.SeatAllocationRepository;
import com.trainreservation.backend.repository.SeatRepository;
import com.trainreservation.backend.repository.StationRepository;
import com.trainreservation.backend.repository.TrainClassRepository;
import com.trainreservation.backend.repository.TrainRepository;
import com.trainreservation.backend.repository.TrainRunRepository;
import com.trainreservation.backend.repository.TrainStopRepository;
import com.trainreservation.backend.repository.UserRepository;
import com.trainreservation.backend.service.impl.BookingServiceImpl;
import com.trainreservation.backend.service.impl.TrainAvailabilityServiceImpl;
import com.trainreservation.backend.service.impl.TrainServiceImpl;
import com.trainreservation.backend.service.CancellationRefundPolicy;
import com.trainreservation.backend.service.TrainRouteLookupService;
import com.trainreservation.backend.exception.InsufficientSeatAvailabilityException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({BookingServiceImpl.class, TrainAvailabilityServiceImpl.class, TrainServiceImpl.class,
        TrainRouteLookupService.class, FareCalculationService.class, CancellationRefundPolicy.class})
@TestPropertySource(properties = "app.fare.per-kilometre-rate=1.25")
class BookingCreationPostgresTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private TrainAvailabilityService availabilityService;

    @Autowired
    private TrainService trainService;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private TrainRepository trainRepository;

    @Autowired
    private TrainClassRepository trainClassRepository;

    @Autowired
    private TrainRunRepository trainRunRepository;

    @Autowired
    private TrainStopRepository trainStopRepository;

    @Autowired
    private CoachRepository coachRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PassengerRepository passengerRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockitoSpyBean
    private CancellationRepository cancellationRepository;

    @MockitoSpyBean
    private SeatAllocationRepository seatAllocationRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void persistsBookingPassengersSeatsAndPendingPaymentInPostgres() {
        Fixture fixture = createFixture();
        authenticateAs(fixture.user().getEmail());

        BookingConfirmationResponse first = bookingService.createBooking(
                request(fixture, List.of(
                        new BookingPassengerRequest("Passenger One", 32, Gender.FEMALE),
                        new BookingPassengerRequest("Passenger Two", 28, Gender.MALE))));
        BookingConfirmationResponse second = bookingService.createBooking(
                request(fixture, List.of(new BookingPassengerRequest("Passenger Three", 40, Gender.OTHER))));

        assertEquals(BookingStatus.PENDING, first.bookingStatus());
        assertEquals(PaymentStatus.PENDING, first.paymentStatus());
        assertEquals(new BigDecimal("350.00"), first.farePerPassenger());
        assertEquals(new BigDecimal("700.00"), first.totalFare());
        assertEquals(2, first.passengers().size());
        assertNotEquals(first.passengers().get(0).seatNumber(), first.passengers().get(1).seatNumber());
        assertTrue(first.pnrNumber().matches("[2-9A-HJ-NP-Z]{16}"));
        assertNotEquals(first.pnrNumber(), second.pnrNumber());
        assertEquals(PaymentStatus.PENDING, second.paymentStatus());

        Booking persistedBooking = bookingRepository.findByPnrNumber(first.pnrNumber()).orElseThrow();
        assertEquals(fixture.user().getId(), persistedBooking.getUser().getId());
        assertEquals(new BigDecimal("700.00"), persistedBooking.getTotalFare());
        assertEquals(2, passengerRepository.findByBookingId(persistedBooking.getId()).size());

        List<Passenger> passengers = passengerRepository.findByBookingId(persistedBooking.getId());
        assertTrue(passengers.stream().allMatch(passenger -> passenger.getSeat() != null));
        assertEquals(4, seatAllocationRepository.findByTrainRunIdAndBookingId(
                fixture.trainRun().getId(), persistedBooking.getId()).size());

        Payment payment = paymentRepository.findByBookingId(persistedBooking.getId()).orElseThrow();
        assertEquals(PaymentStatus.PENDING, payment.getPaymentStatus());
        assertEquals(new BigDecimal("700.00"), payment.getAmount());
        assertNotNull(persistedBooking.getBookingDate());
    }

    @Test
    void rollsBackBookingAndPassengersWhenSeatAllocationFails() {
        Fixture fixture = createFixture();
        authenticateAs(fixture.user().getEmail());
        long bookingsBefore = bookingRepository.count();
        long passengersBefore = passengerRepository.count();
        long paymentsBefore = paymentRepository.count();
        long allocationsBefore = seatAllocationRepository.count();

        TestTransaction.flagForCommit();
        TestTransaction.end();
        doThrow(new DataIntegrityViolationException("Simulated seat allocation constraint conflict"))
                .when(seatAllocationRepository).saveAllAndFlush(any());

        try {
            assertThrows(DataIntegrityViolationException.class,
                    () -> bookingService.createBooking(request(fixture,
                            List.of(new BookingPassengerRequest("Rollback Passenger", 30, Gender.OTHER)))));

            assertEquals(bookingsBefore, bookingRepository.count());
            assertEquals(passengersBefore, passengerRepository.count());
            assertEquals(paymentsBefore, paymentRepository.count());
            assertEquals(allocationsBefore, seatAllocationRepository.count());
        } finally {
            cleanupFixture(fixture);
        }
    }

    @Test
    void preventsOverlappingAllocationButAllowsAdjacentSegmentReuse() {
        Fixture fixture = createFixture(1);
        User secondUser = createAdditionalUser();
        try {
            authenticateAs(fixture.user().getEmail());
            BookingConfirmationResponse firstBooking = bookingService.createBooking(request(
                    fixture, fixture.source(), fixture.connection(),
                    List.of(new BookingPassengerRequest("First Passenger", 30, Gender.OTHER))));

            TrainAvailabilityResponse overlapping = availabilityService.getAvailability(
                    fixture.train().getId(), fixture.source().getStationCode(),
                    fixture.destination().getStationCode(), fixture.trainRun().getJourneyDate(), "SL");
            TrainAvailabilityResponse adjacent = availabilityService.getAvailability(
                    fixture.train().getId(), fixture.connection().getStationCode(),
                    fixture.destination().getStationCode(), fixture.trainRun().getJourneyDate(), "SL");
            assertEquals(0L, overlapping.availableSeats());
            assertEquals(1L, adjacent.availableSeats());

            authenticateAs(secondUser.getEmail());
            assertThrows(InsufficientSeatAvailabilityException.class,
                    () -> bookingService.createBooking(request(
                            fixture, fixture.source(), fixture.destination(),
                            List.of(new BookingPassengerRequest("Overlapping Passenger", 30, Gender.OTHER)))));
            assertTrue(bookingRepository.findByUserIdOrderByBookingDateDesc(secondUser.getId()).isEmpty());

            BookingConfirmationResponse adjacentBooking = bookingService.createBooking(request(
                    fixture, fixture.connection(), fixture.destination(),
                    List.of(new BookingPassengerRequest("Adjacent Passenger", 30, Gender.OTHER))));
            assertEquals(firstBooking.passengers().get(0).seatNumber(),
                    adjacentBooking.passengers().get(0).seatNumber());

            Booking first = bookingRepository.findByPnrNumber(firstBooking.pnrNumber()).orElseThrow();
            Booking second = bookingRepository.findByPnrNumber(adjacentBooking.pnrNumber()).orElseThrow();
            assertEquals(List.of(1), segmentNumbers(fixture, first));
            assertEquals(List.of(2), segmentNumbers(fixture, second));
        } finally {
            cleanupFixture(fixture, secondUser);
        }
    }

    @Test
    void inactiveBookingAllocationsDoNotBlockSeatReuse() {
        Fixture fixture = createFixture(1);
        User secondUser = createAdditionalUser();
        Booking cancelledBooking = new Booking();
        cancelledBooking.setPnrNumber("CANCEL" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 10).toUpperCase());
        cancelledBooking.setUser(fixture.user());
        cancelledBooking.setTrainRun(fixture.trainRun());
        cancelledBooking.setSourceStation(fixture.source());
        cancelledBooking.setDestinationStation(fixture.connection());
        cancelledBooking.setSourceStopNumber(1);
        cancelledBooking.setDestinationStopNumber(2);
        cancelledBooking.setBookingDate(java.time.LocalDateTime.now());
        cancelledBooking.setTotalFare(new BigDecimal("100.00"));
        cancelledBooking.setBookingStatus(BookingStatus.CANCELLED);
        cancelledBooking.setPaymentStatus(PaymentStatus.PENDING);
        bookingRepository.saveAndFlush(cancelledBooking);

        Passenger cancelledPassenger = new Passenger();
        cancelledPassenger.setBooking(cancelledBooking);
        cancelledPassenger.setFullName("Cancelled Passenger");
        cancelledPassenger.setAge(30);
        cancelledPassenger.setGender(Gender.OTHER);
        cancelledPassenger.setSeat(fixture.seats().get(0));
        passengerRepository.saveAndFlush(cancelledPassenger);

        SeatAllocation inactiveAllocation = new SeatAllocation();
        inactiveAllocation.setTrainRun(fixture.trainRun());
        inactiveAllocation.setSeat(fixture.seats().get(0));
        inactiveAllocation.setBooking(cancelledBooking);
        inactiveAllocation.setPassenger(cancelledPassenger);
        inactiveAllocation.setSegmentNumber(1);
        seatAllocationRepository.saveAndFlush(inactiveAllocation);

        authenticateAs(secondUser.getEmail());
        BookingConfirmationResponse newBooking = bookingService.createBooking(request(
                fixture, fixture.source(), fixture.connection(),
                List.of(new BookingPassengerRequest("New Passenger", 30, Gender.OTHER))));

        assertEquals(fixture.seats().get(0).getSeatNumber(), newBooking.passengers().get(0).seatNumber());
        assertTrue(seatAllocationRepository.findByTrainRunIdAndBookingId(
                fixture.trainRun().getId(), cancelledBooking.getId()).isEmpty());
    }

    @Test
    void concurrentRequestsForOneSeatOnlyAllowOneOverlappingBooking() throws Exception {
        Fixture fixture = createFixture(1);
        User secondUser = createAdditionalUser();
        TestTransaction.flagForCommit();
        TestTransaction.end();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<BookingConfirmationResponse> first = executor.submit(
                    concurrentRequest(fixture.user(), fixture, ready, start));
            Future<BookingConfirmationResponse> second = executor.submit(
                    concurrentRequest(secondUser, fixture, ready, start));
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            int successfulBookings = 0;
            int rejectedBookings = 0;
            for (Future<BookingConfirmationResponse> result : List.of(first, second)) {
                try {
                    result.get(30, TimeUnit.SECONDS);
                    successfulBookings++;
                } catch (ExecutionException exception) {
                    if (exception.getCause() instanceof InsufficientSeatAvailabilityException) {
                        rejectedBookings++;
                    } else {
                        throw new AssertionError("Unexpected concurrent booking failure", exception.getCause());
                    }
                }
            }
            assertEquals(1, successfulBookings);
            assertEquals(1, rejectedBookings);
            List<Booking> firstUserBookings =
                    bookingRepository.findByUserIdOrderByBookingDateDesc(fixture.user().getId());
            List<Booking> secondUserBookings =
                    bookingRepository.findByUserIdOrderByBookingDateDesc(secondUser.getId());
            List<Booking> persistedBookings = new ArrayList<>(firstUserBookings);
            persistedBookings.addAll(secondUserBookings);
            assertEquals(1, persistedBookings.size());
            assertEquals(2, seatAllocationRepository.findByTrainRunIdAndBookingId(
                    fixture.trainRun().getId(), persistedBookings.get(0).getId()).size());
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
            cleanupFixture(fixture, secondUser);
        }
    }

    @Test
    void insufficientSeatsDoNotCreatePartialBookingRecords() {
        Fixture fixture = createFixture(1);
        authenticateAs(fixture.user().getEmail());
        long bookingsBefore = bookingRepository.count();
        long passengersBefore = passengerRepository.count();
        long paymentsBefore = paymentRepository.count();
        long allocationsBefore = seatAllocationRepository.count();

        assertThrows(InsufficientSeatAvailabilityException.class,
                () -> bookingService.createBooking(request(
                        fixture, fixture.source(), fixture.destination(),
                        List.of(
                                new BookingPassengerRequest("Passenger One", 30, Gender.OTHER),
                                new BookingPassengerRequest("Passenger Two", 25, Gender.FEMALE)))));

        assertEquals(bookingsBefore, bookingRepository.count());
        assertEquals(passengersBefore, passengerRepository.count());
        assertEquals(paymentsBefore, paymentRepository.count());
        assertEquals(allocationsBefore, seatAllocationRepository.count());
    }

    @Test
    void historyAndPnrDetailsAreLimitedToTheAuthenticatedOwner() {
        Fixture fixture = createFixture();
        User secondUser = createAdditionalUser();

        authenticateAs(fixture.user().getEmail());
        BookingConfirmationResponse ownBooking = bookingService.createBooking(request(
                fixture, List.of(new BookingPassengerRequest("Owner Passenger", 30, Gender.OTHER))));
        authenticateAs(secondUser.getEmail());
        BookingConfirmationResponse otherBooking = bookingService.createBooking(request(
                fixture, List.of(new BookingPassengerRequest("Other Passenger", 31, Gender.FEMALE))));

        authenticateAs(fixture.user().getEmail());
        BookingHistoryPageResponse history = bookingService.getMyBookings(0, 20);
        assertEquals(1, history.totalElements());
        assertEquals(ownBooking.pnrNumber(), history.bookings().get(0).pnrNumber());
        BookingConfirmationResponse details = bookingService.getBookingByPnr(ownBooking.pnrNumber());
        assertEquals(ownBooking.trainNumber(), details.trainNumber());
        assertEquals("Owner Passenger", details.passengers().get(0).fullName());
        assertThrows(BookingAccessDeniedException.class,
                () -> bookingService.getBookingByPnr(otherBooking.pnrNumber()));
        assertThrows(BookingNotFoundException.class,
                () -> bookingService.getBookingByPnr("2222222222222222"));
    }

    @Test
    void adminCanLookUpAnotherUsersBookingByPnr() {
        Fixture fixture = createFixture();
        authenticateAs(fixture.user().getEmail());
        BookingConfirmationResponse booking = bookingService.createBooking(request(
                fixture, List.of(new BookingPassengerRequest("Admin Lookup", 30, Gender.OTHER))));
        User admin = createAdditionalUser(UserRole.ADMIN);

        authenticateAs(admin.getEmail());
        assertEquals(booking.pnrNumber(), bookingService.getBookingByPnr(booking.pnrNumber()).pnrNumber());
    }

    @Test
    void userCannotCancelAnotherUsersBooking() {
        Fixture fixture = createFixture();
        User secondUser = createAdditionalUser();
        authenticateAs(fixture.user().getEmail());
        BookingConfirmationResponse booking = bookingService.createBooking(request(
                fixture, List.of(new BookingPassengerRequest("Owner Passenger", 30, Gender.OTHER))));

        authenticateAs(secondUser.getEmail());
        assertThrows(BookingAccessDeniedException.class,
                () -> bookingService.cancelBooking(booking.pnrNumber()));
    }

    @Test
    void cancellationReleasesOnlyItsSeatsAndLeavesRefundPending() {
        Fixture fixture = createFixture(2);
        User secondUser = createAdditionalUser();
        authenticateAs(fixture.user().getEmail());
        BookingConfirmationResponse cancelledBooking = bookingService.createBooking(request(
                fixture, List.of(new BookingPassengerRequest("Cancel Me", 30, Gender.OTHER))));
        authenticateAs(secondUser.getEmail());
        BookingConfirmationResponse otherBooking = bookingService.createBooking(request(
                fixture, List.of(new BookingPassengerRequest("Keep Me", 31, Gender.FEMALE))));

        authenticateAs(fixture.user().getEmail());
        CancellationResponse cancellation = bookingService.cancelBooking(cancelledBooking.pnrNumber());
        Booking persistedCancelled = bookingRepository.findByPnrNumber(cancelledBooking.pnrNumber()).orElseThrow();
        Booking persistedOther = bookingRepository.findByPnrNumber(otherBooking.pnrNumber()).orElseThrow();

        assertEquals(BookingStatus.CANCELLED, cancellation.bookingStatus());
        assertEquals(new BigDecimal("280.00"), cancellation.refundAmount());
        assertEquals(CancellationStatus.REQUESTED, cancellation.refundStatus());
        assertEquals(BookingStatus.CANCELLED, persistedCancelled.getBookingStatus());
        assertEquals(BookingStatus.PENDING, persistedOther.getBookingStatus());
        assertTrue(seatAllocationRepository.findByTrainRunIdAndBookingId(
                fixture.trainRun().getId(), persistedCancelled.getId()).isEmpty());
        assertEquals(2, seatAllocationRepository.findByTrainRunIdAndBookingId(
                fixture.trainRun().getId(), persistedOther.getId()).size());
        assertEquals(PaymentStatus.PENDING,
                paymentRepository.findByBookingId(persistedCancelled.getId()).orElseThrow().getPaymentStatus());
        assertEquals(CancellationStatus.REQUESTED,
                cancellationRepository.findByBookingId(persistedCancelled.getId()).orElseThrow()
                        .getCancellationStatus());

        authenticateAs(fixture.user().getEmail());
        BookingConfirmationResponse reusedSeatBooking = bookingService.createBooking(request(
                fixture, List.of(new BookingPassengerRequest("Reuse Seat", 25, Gender.MALE))));
        assertEquals(cancelledBooking.passengers().get(0).seatNumber(),
                reusedSeatBooking.passengers().get(0).seatNumber());
        assertThrows(BookingAlreadyCancelledException.class,
                () -> bookingService.cancelBooking(cancelledBooking.pnrNumber()));
    }

    @Test
    void cancellationFailureRollsBackStatusAndSeatRelease() {
        Fixture fixture = createFixture(1);
        authenticateAs(fixture.user().getEmail());
        BookingConfirmationResponse booking = bookingService.createBooking(request(
                fixture, List.of(new BookingPassengerRequest("Rollback Cancel", 30, Gender.OTHER))));
        long bookingId = bookingRepository.findByPnrNumber(booking.pnrNumber()).orElseThrow().getId();
        TestTransaction.flagForCommit();
        TestTransaction.end();
        doThrow(new DataIntegrityViolationException("Simulated cancellation persistence failure"))
                .when(cancellationRepository).saveAndFlush(any(Cancellation.class));

        try {
            assertThrows(DataIntegrityViolationException.class,
                    () -> bookingService.cancelBooking(booking.pnrNumber()));

            Booking unchanged = bookingRepository.findByPnrNumber(booking.pnrNumber()).orElseThrow();
            assertEquals(BookingStatus.PENDING, unchanged.getBookingStatus());
            assertEquals(2, seatAllocationRepository.findByTrainRunIdAndBookingId(
                    fixture.trainRun().getId(), bookingId).size());
            assertTrue(cancellationRepository.findByBookingId(bookingId).isEmpty());
        } finally {
            cleanupFixture(fixture);
        }
    }

    @Test
    void cancellationAfterScheduledDepartureIsRejected() {
        Fixture fixture = createFixture();
        authenticateAs(fixture.user().getEmail());
        BookingConfirmationResponse booking = bookingService.createBooking(request(
                fixture, List.of(new BookingPassengerRequest("Late Cancel", 30, Gender.OTHER))));
        fixture.trainRun().setJourneyDate(LocalDate.now());
        trainRunRepository.saveAndFlush(fixture.trainRun());
        TrainStop sourceStop = fixture.stops().get(0);
        sourceStop.setDepartureTime(LocalTime.MIN);
        trainStopRepository.saveAndFlush(sourceStop);

        assertThrows(CancellationNotPermittedException.class,
                () -> bookingService.cancelBooking(booking.pnrNumber()));
        Booking persisted = bookingRepository.findByPnrNumber(booking.pnrNumber()).orElseThrow();
        assertEquals(BookingStatus.PENDING, persisted.getBookingStatus());
        assertEquals(2, seatAllocationRepository.findByTrainRunIdAndBookingId(
                fixture.trainRun().getId(), persisted.getId()).size());
    }

    @Test
    void completesSearchBookingHistoryCancellationAndSeatReuseLifecycle() {
        Fixture fixture = createFixture(2);
        List<TrainSearchResponse> searchResults =
                trainService.searchTrains(fixture.source().getStationCode(),
                        fixture.destination().getStationCode(),
                        fixture.trainRun().getJourneyDate(), "SL");
        assertEquals(1, searchResults.size());
        assertEquals(fixture.train().getId(), searchResults.get(0).trainId());
        assertEquals(3, trainService.getTrainDetails(fixture.train().getId()).routeStops().size());

        TrainAvailabilityResponse beforeBooking = availabilityService.getAvailability(
                fixture.train().getId(), fixture.source().getStationCode(),
                fixture.destination().getStationCode(), fixture.trainRun().getJourneyDate(), "SL");
        assertEquals(2L, beforeBooking.totalSeats());
        assertEquals(2L, beforeBooking.availableSeats());

        authenticateAs(fixture.user().getEmail());
        BookingConfirmationResponse booking = bookingService.createBooking(request(
                fixture, List.of(new BookingPassengerRequest("Lifecycle Passenger", 30, Gender.OTHER))));
        assertEquals(1L, availabilityService.getAvailability(
                fixture.train().getId(), fixture.source().getStationCode(),
                fixture.destination().getStationCode(), fixture.trainRun().getJourneyDate(), "SL")
                .availableSeats());
        assertEquals(booking.pnrNumber(), bookingService.getMyBookings(0, 10)
                .bookings().get(0).pnrNumber());
        assertEquals("Lifecycle Passenger",
                bookingService.getBookingByPnr(booking.pnrNumber()).passengers().get(0).fullName());

        CancellationResponse cancellation = bookingService.cancelBooking(booking.pnrNumber());
        assertEquals(CancellationStatus.REQUESTED, cancellation.refundStatus());
        assertEquals(PaymentStatus.PENDING,
                paymentRepository.findByBookingId(
                        bookingRepository.findByPnrNumber(booking.pnrNumber()).orElseThrow().getId())
                        .orElseThrow().getPaymentStatus());
        assertEquals(2L, availabilityService.getAvailability(
                fixture.train().getId(), fixture.source().getStationCode(),
                fixture.destination().getStationCode(), fixture.trainRun().getJourneyDate(), "SL")
                .availableSeats());

        BookingConfirmationResponse replacement = bookingService.createBooking(request(
                fixture, List.of(new BookingPassengerRequest("Replacement Passenger", 25, Gender.FEMALE))));
        assertEquals(booking.passengers().get(0).seatNumber(),
                replacement.passengers().get(0).seatNumber());
    }

    @Test
    void concurrentCancellationRequestsCreateOnlyOneCancellation() throws Exception {
        Fixture fixture = createFixture(1);
        authenticateAs(fixture.user().getEmail());
        BookingConfirmationResponse booking = bookingService.createBooking(request(
                fixture, List.of(new BookingPassengerRequest("Concurrent Cancel", 30, Gender.OTHER))));
        long bookingId = bookingRepository.findByPnrNumber(booking.pnrNumber()).orElseThrow().getId();
        TestTransaction.flagForCommit();
        TestTransaction.end();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<CancellationResponse> first = executor.submit(
                    concurrentCancellation(fixture.user(), booking.pnrNumber(), ready, start));
            Future<CancellationResponse> second = executor.submit(
                    concurrentCancellation(fixture.user(), booking.pnrNumber(), ready, start));
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            int successfulCancellations = 0;
            int rejectedCancellations = 0;
            for (Future<CancellationResponse> result : List.of(first, second)) {
                try {
                    result.get(30, TimeUnit.SECONDS);
                    successfulCancellations++;
                } catch (ExecutionException exception) {
                    if (exception.getCause() instanceof BookingAlreadyCancelledException) {
                        rejectedCancellations++;
                    } else {
                        throw new AssertionError("Unexpected concurrent cancellation failure",
                                exception.getCause());
                    }
                }
            }
            assertEquals(1, successfulCancellations);
            assertEquals(1, rejectedCancellations);
            assertEquals(1, cancellationRepository.findByBookingId(bookingId).stream().count());
            assertTrue(seatAllocationRepository.findByTrainRunIdAndBookingId(
                    fixture.trainRun().getId(), bookingId).isEmpty());
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
            cleanupFixture(fixture);
        }
    }

    private Fixture createFixture() {
        return createFixture(4);
    }

    private Fixture createFixture(int seatCount) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        Station source = station("X" + suffix + "A");
        Station connection = station("X" + suffix + "B");
        Station destination = station("X" + suffix + "C");
        stationRepository.saveAllAndFlush(List.of(source, connection, destination));

        Train train = new Train();
        train.setTrainNumber("T" + suffix);
        train.setTrainName("Booking Integration Train");
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
        trainRun.setJourneyDate(LocalDate.now().plusDays(30));
        trainRun.setRunningStatus(RunningStatus.SCHEDULED);
        trainRunRepository.saveAndFlush(trainRun);

        List<TrainStop> stops = List.of(
                stop(train, source, 1, "0.00"),
                stop(train, connection, 2, "100.00"),
                stop(train, destination, 3, "200.00"));
        trainStopRepository.saveAllAndFlush(stops);

        Coach coach = new Coach();
        coach.setTrainRun(trainRun);
        coach.setTrainClass(trainClass);
        coach.setCoachNumber("S1");
        coachRepository.saveAndFlush(coach);

        List<Seat> seats = new ArrayList<>();
        for (int number = 1; number <= seatCount; number++) {
            Seat seat = new Seat();
            seat.setCoach(coach);
            seat.setSeatNumber(Integer.toString(number));
            seat.setSeatType(SeatType.LOWER);
            seats.add(seat);
        }
        seatRepository.saveAllAndFlush(seats);

        User user = new User();
        user.setFullName("Booking Integration User");
        user.setEmail("booking-" + suffix.toLowerCase() + "@example.test");
        user.setPhoneNumber("5551234567");
        user.setPassword(new BCryptPasswordEncoder().encode("integration-password"));
        user.setRole(UserRole.USER);
        userRepository.saveAndFlush(user);

        return new Fixture(train, trainClass, trainRun, coach, seats, stops,
                user, source, connection, destination);
    }

    private CreateBookingRequest request(Fixture fixture, List<BookingPassengerRequest> passengers) {
        return request(fixture, fixture.source(), fixture.destination(), passengers);
    }

    private CreateBookingRequest request(Fixture fixture, Station source, Station destination,
                                         List<BookingPassengerRequest> passengers) {
        return new CreateBookingRequest(fixture.trainRun().getId(),
                source.getStationCode(), destination.getStationCode(),
                fixture.trainRun().getJourneyDate(), fixture.trainClass().getClassCode().getCode(), passengers);
    }

    private User createAdditionalUser() {
        return createAdditionalUser(UserRole.USER);
    }

    private User createAdditionalUser(UserRole role) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        User user = new User();
        user.setFullName("Concurrent Booking User");
        user.setEmail("concurrent-" + suffix + "@example.test");
        user.setPhoneNumber("5551234568");
        user.setPassword(new BCryptPasswordEncoder().encode("integration-password"));
        user.setRole(role);
        return userRepository.saveAndFlush(user);
    }

    private Callable<CancellationResponse> concurrentCancellation(
            User user, String pnrNumber, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            authenticateAs(user.getEmail());
            ready.countDown();
            try {
                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting to start concurrent cancellation");
                }
                return bookingService.cancelBooking(pnrNumber);
            } finally {
                SecurityContextHolder.clearContext();
            }
        };
    }

    private Callable<BookingConfirmationResponse> concurrentRequest(
            User user, Fixture fixture, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            authenticateAs(user.getEmail());
            ready.countDown();
            try {
                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting to start concurrent booking");
                }
                return bookingService.createBooking(request(
                        fixture, fixture.source(), fixture.destination(),
                        List.of(new BookingPassengerRequest("Concurrent Passenger", 30, Gender.OTHER))));
            } finally {
                SecurityContextHolder.clearContext();
            }
        };
    }

    private List<Integer> segmentNumbers(Fixture fixture, Booking booking) {
        return seatAllocationRepository.findByTrainRunIdAndBookingId(
                        fixture.trainRun().getId(), booking.getId()).stream()
                .map(SeatAllocation::getSegmentNumber)
                .sorted()
                .toList();
    }

    private void cleanupFixture(Fixture fixture, User... additionalUsers) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            List<User> users = new ArrayList<>();
            users.add(fixture.user());
            users.addAll(List.of(additionalUsers));

            List<Booking> bookings = users.stream()
                    .flatMap(user -> bookingRepository.findByUserIdOrderByBookingDateDesc(user.getId()).stream())
                    .filter(booking -> booking.getTrainRun().getId().equals(fixture.trainRun().getId()))
                    .toList();
            List<Passenger> passengers = bookings.stream()
                    .flatMap(booking -> passengerRepository.findByBookingId(booking.getId()).stream())
                    .toList();
            List<Payment> payments = bookings.stream()
                    .map(booking -> paymentRepository.findByBookingId(booking.getId()).orElse(null))
                    .filter(java.util.Objects::nonNull)
                    .toList();
            List<Cancellation> cancellations = bookings.stream()
                    .map(booking -> cancellationRepository.findByBookingId(booking.getId()).orElse(null))
                    .filter(java.util.Objects::nonNull)
                    .toList();
            List<SeatAllocation> allocations = bookings.stream()
                    .flatMap(booking -> seatAllocationRepository
                            .findByTrainRunIdAndBookingId(fixture.trainRun().getId(), booking.getId()).stream())
                    .toList();
            seatAllocationRepository.deleteAll(allocations);
            cancellationRepository.deleteAll(cancellations);
            paymentRepository.deleteAll(payments);
            passengerRepository.deleteAll(passengers);
            bookingRepository.deleteAll(bookings);
            seatRepository.deleteAll(fixture.seats());
            coachRepository.delete(fixture.coach());
            trainStopRepository.deleteAll(fixture.stops());
            trainRunRepository.delete(fixture.trainRun());
            trainClassRepository.delete(fixture.trainClass());
            trainRepository.delete(fixture.train());
            stationRepository.deleteAll(List.of(
                    fixture.source(), fixture.connection(), fixture.destination()));
            userRepository.deleteAll(users);
        });
    }

    private void authenticateAs(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        email, "unused", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    private Station station(String code) {
        Station station = new Station();
        station.setStationCode(code);
        station.setStationName("Booking Test " + code);
        station.setCity("Test City");
        station.setState("Test State");
        return station;
    }

    private TrainStop stop(Train train, Station station, int number, String distance) {
        TrainStop stop = new TrainStop();
        stop.setTrain(train);
        stop.setStation(station);
        stop.setStopNumber(number);
        stop.setDayNumber(1);
        stop.setArrivalTime(LocalTime.of(10, number));
        stop.setDepartureTime(LocalTime.of(10, number + 1));
        stop.setDistanceFromSource(new BigDecimal(distance));
        return stop;
    }

    private record Fixture(Train train, TrainClass trainClass, TrainRun trainRun, Coach coach,
                           List<Seat> seats, List<TrainStop> stops, User user, Station source,
                           Station connection, Station destination) {
    }
}
