package com.trainreservation.backend.service;

import com.trainreservation.backend.dto.request.AddTrainRouteStopRequest;
import com.trainreservation.backend.dto.request.TrainRouteRequest;
import com.trainreservation.backend.dto.request.TrainRouteStopRequest;
import com.trainreservation.backend.dto.response.TrainRouteResponse;
import com.trainreservation.backend.entity.Booking;
import com.trainreservation.backend.entity.Station;
import com.trainreservation.backend.entity.Train;
import com.trainreservation.backend.entity.TrainRun;
import com.trainreservation.backend.entity.TrainStop;
import com.trainreservation.backend.entity.User;
import com.trainreservation.backend.enums.BookingStatus;
import com.trainreservation.backend.enums.PaymentStatus;
import com.trainreservation.backend.enums.RunningStatus;
import com.trainreservation.backend.enums.TrainType;
import com.trainreservation.backend.enums.UserRole;
import com.trainreservation.backend.exception.InvalidRouteException;
import com.trainreservation.backend.exception.TrainRouteConflictException;
import com.trainreservation.backend.repository.BookingRepository;
import com.trainreservation.backend.repository.StationRepository;
import com.trainreservation.backend.repository.TrainRepository;
import com.trainreservation.backend.repository.TrainRunRepository;
import com.trainreservation.backend.repository.TrainStopRepository;
import com.trainreservation.backend.repository.UserRepository;
import com.trainreservation.backend.service.impl.TrainRouteManagementServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TrainRouteManagementServiceImpl.class)
class TrainRouteManagementPostgresTest {

    @Autowired
    private TrainRouteManagementServiceImpl routeService;

    @Autowired
    private TrainRepository trainRepository;

    @Autowired
    private TrainStopRepository trainStopRepository;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private TrainRunRepository trainRunRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    void postgresCreatesRetrievesAndReplacesOrderedRoute() {
        Fixture fixture = fixture();
        TrainRouteResponse created = routeService.createRoute(fixture.train().getId(),
                route(fixture, "0", "100"));

        assertEquals(List.of(fixture.origin().getId(), fixture.destination().getId()),
                created.stops().stream().map(stop -> stop.stationId()).toList());
        assertEquals(1, created.stops().get(0).stopNumber());
        assertEquals(2, created.stops().get(1).stopNumber());
        assertEquals(2, routeService.getRoute(fixture.train().getId()).stops().size());

        TrainRouteResponse updated = routeService.updateRoute(fixture.train().getId(),
                route(fixture, "0", "120"));
        assertEquals(new BigDecimal("120"), updated.stops().get(1).distanceFromSource());
        assertEquals(2, trainStopRepository.findByTrainIdOrderByStopNumberAsc(
                fixture.train().getId()).size());
    }

    @Test
    void invalidPostgresRouteReplacementLeavesExistingRouteUnchanged() {
        Fixture fixture = fixture();
        routeService.createRoute(fixture.train().getId(), route(fixture, "0", "100"));

        TrainRouteRequest invalidReplacement = new TrainRouteRequest(List.of(
                stop(fixture.origin(), "1", null, time("08:00"), 1, "0"),
                stop(fixture.destination(), "2", time("10:00"), time("10:05"), 1, "100")));
        assertThrows(InvalidRouteException.class,
                () -> routeService.updateRoute(fixture.train().getId(), invalidReplacement));

        assertEquals(new BigDecimal("100"), routeService.getRoute(fixture.train().getId())
                .stops().get(1).distanceFromSource());
        assertEquals(2, trainStopRepository.findByTrainIdOrderByStopNumberAsc(
                fixture.train().getId()).size());
    }

    @Test
    void postgresAppendsUpdatesAndRemovesStopsWithoutLeavingInvalidBoundaries() {
        Fixture fixture = fixture();
        routeService.createRoute(fixture.train().getId(), route(fixture, "0", "100"));
        Station appendedStation = station("M" + suffix());
        stationRepository.saveAndFlush(appendedStation);

        TrainRouteResponse appended = routeService.addStop(fixture.train().getId(),
                new AddTrainRouteStopRequest(stop(appendedStation, "3", time("12:00"), null, 1, "200"),
                        time("10:05")));
        assertEquals(time("10:05"), appended.stops().get(1).departureTime());
        assertEquals(3, appended.stops().size());

        Long appendedStopId = trainStopRepository.findByTrainIdOrderByStopNumberAsc(fixture.train().getId())
                .get(2).getId();
        TrainRouteResponse updated = routeService.updateStop(fixture.train().getId(), appendedStopId,
                stop(appendedStation, "3", time("12:00"), null, 1, "205"));
        assertEquals(new BigDecimal("205"), updated.stops().get(2).distanceFromSource());

        Long lastStopId = trainStopRepository.findByTrainIdOrderByStopNumberAsc(fixture.train().getId())
                .get(2).getId();
        TrainRouteResponse removed = routeService.removeStop(fixture.train().getId(), lastStopId);
        assertEquals(2, removed.stops().size());
        assertEquals(2, removed.stops().get(1).stopNumber());
        assertEquals(fixture.destination().getId(), removed.stops().get(1).stationId());
        assertNull(removed.stops().get(1).departureTime());
    }

    @Test
    void routeMutationIsRejectedAfterSchedulingAndHistoricalBookingRemains() {
        Fixture fixture = fixture();
        TrainRouteResponse route = routeService.createRoute(fixture.train().getId(),
                route(fixture, "0", "100"));

        TrainRun trainRun = new TrainRun();
        trainRun.setTrain(fixture.train());
        trainRun.setJourneyDate(LocalDate.now().plusDays(20));
        trainRun.setRunningStatus(RunningStatus.SCHEDULED);
        trainRun = trainRunRepository.saveAndFlush(trainRun);

        User user = new User();
        user.setFullName("Route History Test User");
        user.setEmail("route-history-" + suffix().toLowerCase() + "@example.test");
        user.setPhoneNumber("5551234567");
        user.setPassword(new BCryptPasswordEncoder().encode("route-history-password"));
        user.setRole(UserRole.USER);
        user = userRepository.saveAndFlush(user);

        Booking booking = new Booking();
        booking.setPnrNumber("P" + suffix());
        booking.setUser(user);
        booking.setTrainRun(trainRun);
        booking.setSourceStation(fixture.origin());
        booking.setDestinationStation(fixture.destination());
        booking.setSourceStopNumber(1);
        booking.setDestinationStopNumber(2);
        booking.setBookingDate(LocalDateTime.now());
        booking.setTotalFare(new BigDecimal("100.00"));
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.PAID);
        booking = bookingRepository.saveAndFlush(booking);

        TrainRouteRequest replacement = new TrainRouteRequest(List.of(
                stop(fixture.origin(), "1", null, time("07:00"), 1, "0"),
                stop(fixture.destination(), "2", time("09:00"), null, 1, "100")));
        assertThrows(TrainRouteConflictException.class,
                () -> routeService.updateRoute(fixture.train().getId(), replacement));
        assertThrows(TrainRouteConflictException.class,
                () -> routeService.removeStop(fixture.train().getId(), route.stops().get(1).id()));

        assertTrue(bookingRepository.findByPnrNumber(booking.getPnrNumber()).isPresent());
        assertEquals(List.of(fixture.origin().getId(), fixture.destination().getId()),
                routeService.getRoute(fixture.train().getId()).stops().stream()
                        .map(stop -> stop.stationId()).toList());
    }

    @Test
    void postgresUniqueConstraintsRejectDuplicateSequenceAndStationAssignments() {
        Fixture fixture = fixture();
        TrainStop first = persistedStop(fixture.train(), fixture.origin(), 1, "0");
        trainStopRepository.saveAndFlush(first);
        assertThrows(DataIntegrityViolationException.class,
                () -> trainStopRepository.saveAndFlush(
                        persistedStop(fixture.train(), fixture.destination(), 1, "100")));
    }

    @Test
    void postgresUniqueStationConstraintRejectsRepeatedStationOnDifferentSequence() {
        Fixture fixture = fixture();
        trainStopRepository.saveAndFlush(persistedStop(fixture.train(), fixture.origin(), 1, "0"));
        assertThrows(DataIntegrityViolationException.class,
                () -> trainStopRepository.saveAndFlush(
                        persistedStop(fixture.train(), fixture.origin(), 2, "100")));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentAppendRequestsCannotCreateDuplicateRouteSequences() throws Exception {
        Fixture fixture = fixture();
        Station middle = station("C" + suffix());
        stationRepository.saveAndFlush(middle);
        routeService.createRoute(fixture.train().getId(), route(fixture, "0", "100"));

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(() -> tryAppend(
                    fixture, middle, ready, start));
            Future<Boolean> second = executor.submit(() -> tryAppend(
                    fixture, middle, ready, start));
            ready.await();
            start.countDown();

            int successfulAdds = (first.get() ? 1 : 0) + (second.get() ? 1 : 0);
            assertEquals(1, successfulAdds);
            List<TrainStop> persisted = trainStopRepository
                    .findByTrainIdOrderByStopNumberAsc(fixture.train().getId());
            assertEquals(3, persisted.size());
            assertEquals(List.of(1, 2, 3), persisted.stream().map(TrainStop::getStopNumber).toList());
        } finally {
            executor.shutdownNow();
            trainStopRepository.deleteAll(trainStopRepository.findByTrainIdOrderByStopNumberAsc(
                    fixture.train().getId()));
            trainStopRepository.flush();
            trainRepository.delete(fixture.train());
            stationRepository.deleteAll(List.of(fixture.origin(), fixture.destination(), middle));
        }
    }

    private boolean tryAppend(Fixture fixture, Station middle,
                              CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            routeService.addStop(fixture.train().getId(),
                    new AddTrainRouteStopRequest(stop(middle, "3", time("12:00"), null, 1, "200"),
                            time("10:05")));
            return true;
        } catch (TrainRouteConflictException | DataIntegrityViolationException exception) {
            return false;
        }
    }

    private Fixture fixture() {
        String suffix = suffix();
        Train train = new Train();
        train.setTrainNumber("T" + suffix);
        train.setTrainName("Route Test " + suffix);
        train.setTrainType(TrainType.EXPRESS);
        train.setActive(true);
        train = trainRepository.saveAndFlush(train);
        Station origin = station("A" + suffix);
        Station destination = station("B" + suffix);
        stationRepository.saveAllAndFlush(List.of(origin, destination));
        return new Fixture(train, origin, destination);
    }

    private Station station(String code) {
        Station station = new Station();
        station.setStationCode(code);
        station.setStationName("Route Station " + code);
        station.setCity("Test City");
        station.setState("Test State");
        station.setActive(true);
        return station;
    }

    private TrainRouteRequest route(Fixture fixture, String originDistance, String destinationDistance) {
        return new TrainRouteRequest(List.of(
                stop(fixture.origin(), "1", null, time("08:00"), 1, originDistance),
                stop(fixture.destination(), "2", time("10:00"), null, 1, destinationDistance)));
    }

    private TrainRouteStopRequest stop(Station station, String stopNumber,
                                       LocalTime arrival, LocalTime departure, int day, String distance) {
        return new TrainRouteStopRequest(station.getStationCode(), Integer.valueOf(stopNumber),
                arrival, departure, day, new BigDecimal(distance));
    }

    private TrainStop persistedStop(Train train, Station station, int number, String distance) {
        TrainStop stop = new TrainStop();
        stop.setTrain(train);
        stop.setStation(station);
        stop.setStopNumber(number);
        stop.setArrivalTime(number == 1 ? null : time("10:00"));
        stop.setDepartureTime(number == 1 ? time("08:00") : null);
        stop.setDayNumber(1);
        stop.setDistanceFromSource(new BigDecimal(distance));
        return stop;
    }

    private String suffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private LocalTime time(String value) {
        return LocalTime.parse(value);
    }

    private record Fixture(Train train, Station origin, Station destination) {
    }
}
