package com.trainreservation.backend.service;

import com.trainreservation.backend.dto.request.TrainRequest;
import com.trainreservation.backend.dto.response.TrainPageResponse;
import com.trainreservation.backend.dto.response.TrainResponse;
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
import com.trainreservation.backend.repository.BookingRepository;
import com.trainreservation.backend.repository.StationRepository;
import com.trainreservation.backend.repository.TrainRepository;
import com.trainreservation.backend.repository.TrainRunRepository;
import com.trainreservation.backend.repository.TrainStopRepository;
import com.trainreservation.backend.repository.UserRepository;
import com.trainreservation.backend.service.impl.TrainManagementServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TrainManagementServiceImpl.class)
class TrainManagementPostgresTest {

    @Autowired
    private TrainManagementServiceImpl trainService;

    @Autowired
    private TrainRepository trainRepository;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private TrainRunRepository trainRunRepository;

    @Autowired
    private TrainStopRepository trainStopRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    void postgresUniqueConstraintRejectsDuplicateTrainNumber() {
        String number = trainNumber();
        trainRepository.saveAndFlush(train(number, "Unique Test Train", true));

        assertThrows(DataIntegrityViolationException.class,
                () -> trainRepository.saveAndFlush(train(number, "Duplicate Test Train", true)));
    }

    @Test
    void updateAndDeactivationPreserveExistingRouteRunAndBookingRelationships() {
        String suffix = suffix();
        Train train = trainRepository.saveAndFlush(train("T" + suffix, "Relationship Test Train", true));
        Station source = stationRepository.saveAndFlush(station("A" + suffix));
        Station destination = stationRepository.saveAndFlush(station("B" + suffix));

        TrainStop sourceStop = stop(train, source, 1, "0.00");
        TrainStop destinationStop = stop(train, destination, 2, "100.00");
        trainStopRepository.saveAllAndFlush(List.of(sourceStop, destinationStop));

        TrainRun trainRun = new TrainRun();
        trainRun.setTrain(train);
        trainRun.setJourneyDate(LocalDate.now().plusDays(30));
        trainRun.setRunningStatus(RunningStatus.SCHEDULED);
        trainRunRepository.saveAndFlush(trainRun);

        User user = new User();
        user.setFullName("Train Management Test User");
        user.setEmail("train-management-" + suffix.toLowerCase() + "@example.test");
        user.setPhoneNumber("5551234567");
        user.setPassword(new BCryptPasswordEncoder().encode("integration-password"));
        user.setRole(UserRole.USER);
        userRepository.saveAndFlush(user);

        Booking booking = new Booking();
        booking.setPnrNumber("B" + suffix);
        booking.setUser(user);
        booking.setTrainRun(trainRun);
        booking.setSourceStation(source);
        booking.setDestinationStation(destination);
        booking.setSourceStopNumber(1);
        booking.setDestinationStopNumber(2);
        booking.setBookingDate(LocalDateTime.now());
        booking.setTotalFare(new BigDecimal("200.00"));
        booking.setBookingStatus(BookingStatus.PENDING);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        bookingRepository.saveAndFlush(booking);

        TrainResponse updated = trainService.updateTrain(train.getId(),
                new TrainRequest("u" + suffix, "Renamed Test Train", TrainType.SUPERFAST));
        TrainResponse deactivated = trainService.deactivateTrain(train.getId());

        assertEquals(train.getId(), updated.id());
        assertEquals("U" + suffix, updated.trainNumber());
        assertFalse(deactivated.active());
        assertTrue(trainRepository.existsById(train.getId()));
        assertTrue(trainRunRepository.findById(trainRun.getId()).isPresent());
        assertEquals(booking.getId(),
                bookingRepository.findByPnrNumber(booking.getPnrNumber()).orElseThrow().getId());
        List<TrainStop> persistedStops = trainStopRepository.findByTrainIdOrderByStopNumberAsc(train.getId());
        assertEquals(List.of(source.getId(), destination.getId()),
                persistedStops.stream().map(stop -> stop.getStation().getId()).toList());
    }

    @Test
    void postgresSearchAndActiveFilterReturnMatchingTrains() {
        String suffix = suffix();
        Train byNumber = train("N" + suffix, "Overnight Express", true);
        Train byName = train("Q" + suffix, "Needle " + suffix + " Rajdhani", false);
        trainRepository.saveAllAndFlush(List.of(byNumber, byName));

        assertEquals(List.of(byNumber.getId()), trainService.searchTrains(
                        "N" + suffix, 0, 10, "trainNumber", "asc").trains().stream()
                .map(TrainResponse::id).toList());
        assertEquals(List.of(byName.getId()), trainService.searchTrains(
                        "needle " + suffix, 0, 10, "trainName", "asc").trains().stream()
                .map(TrainResponse::id).toList());
        assertTrue(trainService.searchTrains("NoMatch" + suffix, 0, 10, "trainName", "asc")
                .trains().isEmpty());

        TrainPageResponse inactiveTrains = trainService.getTrains(0, 100, "trainName", "asc", false);
        assertTrue(inactiveTrains.trains().stream().anyMatch(result -> result.id().equals(byName.getId())));
        TrainPageResponse activeTrains = trainService.getTrains(0, 100, "trainName", "asc", true);
        assertTrue(activeTrains.trains().stream().anyMatch(result -> result.id().equals(byNumber.getId())));
        assertFalse(activeTrains.trains().stream().anyMatch(result -> result.id().equals(byName.getId())));
    }

    @Test
    void postgresTrainListingReturnsRequestedPageAndSort() {
        String suffix = suffix();
        trainRepository.saveAllAndFlush(List.of(
                train("A" + suffix, "A Test Train " + suffix, true),
                train("Z" + suffix, "Z Test Train " + suffix, true)));

        TrainPageResponse firstPage = trainService.searchTrains(
                "Test Train " + suffix, 0, 1, "trainName", "asc");
        TrainPageResponse secondPage = trainService.searchTrains(
                "Test Train " + suffix, 1, 1, "trainName", "asc");

        assertEquals(1, firstPage.size());
        assertEquals(2, firstPage.totalElements());
        assertEquals(2, firstPage.totalPages());
        assertEquals("A Test Train " + suffix, firstPage.trains().get(0).trainName());
        assertEquals("Z Test Train " + suffix, secondPage.trains().get(0).trainName());
    }

    private Train train(String number, String name, boolean active) {
        Train train = new Train();
        train.setTrainNumber(number);
        train.setTrainName(name);
        train.setTrainType(TrainType.EXPRESS);
        train.setActive(active);
        return train;
    }

    private Station station(String code) {
        Station station = new Station();
        station.setStationCode(code);
        station.setStationName("Management " + code);
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

    private String trainNumber() {
        return "T" + suffix();
    }

    private String suffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
