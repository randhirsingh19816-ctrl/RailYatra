package com.trainreservation.backend.service.impl;

import com.trainreservation.backend.dto.response.TrainAvailabilityResponse;
import com.trainreservation.backend.entity.Station;
import com.trainreservation.backend.entity.Train;
import com.trainreservation.backend.entity.TrainClass;
import com.trainreservation.backend.entity.TrainRun;
import com.trainreservation.backend.entity.TrainStop;
import com.trainreservation.backend.enums.BookingStatus;
import com.trainreservation.backend.enums.RunningStatus;
import com.trainreservation.backend.enums.TrainClassCode;
import com.trainreservation.backend.enums.TrainType;
import com.trainreservation.backend.exception.InvalidRequestException;
import com.trainreservation.backend.exception.InvalidRouteException;
import com.trainreservation.backend.exception.InvalidTrainClassException;
import com.trainreservation.backend.exception.StationNotFoundException;
import com.trainreservation.backend.exception.TrainNotFoundException;
import com.trainreservation.backend.exception.TrainNotOperatingException;
import com.trainreservation.backend.repository.SeatAllocationRepository;
import com.trainreservation.backend.repository.SeatRepository;
import com.trainreservation.backend.repository.StationRepository;
import com.trainreservation.backend.repository.TrainClassRepository;
import com.trainreservation.backend.repository.TrainRepository;
import com.trainreservation.backend.repository.TrainRunRepository;
import com.trainreservation.backend.repository.TrainStopRepository;
import com.trainreservation.backend.service.FareCalculationService;
import com.trainreservation.backend.service.TrainRouteLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrainAvailabilityServiceImplTest {

    private static final LocalDate JOURNEY_DATE = LocalDate.of(2026, 11, 20);

    private StationRepository stationRepository;
    private TrainRepository trainRepository;
    private TrainRunRepository trainRunRepository;
    private TrainStopRepository trainStopRepository;
    private TrainClassRepository trainClassRepository;
    private SeatRepository seatRepository;
    private SeatAllocationRepository seatAllocationRepository;
    private TrainAvailabilityServiceImpl availabilityService;
    private Station source;
    private Station connection;
    private Station destination;
    private Train train;
    private TrainRun trainRun;
    private TrainClass trainClass;
    private TrainStop sourceStop;
    private TrainStop connectionStop;
    private TrainStop destinationStop;

    @BeforeEach
    void setUp() {
        stationRepository = mock(StationRepository.class);
        trainRepository = mock(TrainRepository.class);
        trainRunRepository = mock(TrainRunRepository.class);
        trainStopRepository = mock(TrainStopRepository.class);
        trainClassRepository = mock(TrainClassRepository.class);
        seatRepository = mock(SeatRepository.class);
        seatAllocationRepository = mock(SeatAllocationRepository.class);
        availabilityService = new TrainAvailabilityServiceImpl(
                new TrainRouteLookupService(stationRepository),
                trainRepository,
                trainRunRepository,
                trainStopRepository,
                trainClassRepository,
                seatRepository,
                seatAllocationRepository,
                new FareCalculationService(new BigDecimal("1.00")));

        source = station(1L, "NDLS", "New Delhi");
        connection = station(2L, "CNB", "Kanpur Central");
        destination = station(3L, "LKO", "Lucknow");

        train = new Train();
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

        sourceStop = stop(source, 1, "0.00");
        connectionStop = stop(connection, 2, "180.50");
        destinationStop = stop(destination, 3, "250.75");

        when(trainRepository.findById(10L)).thenReturn(Optional.of(train));
        when(trainRunRepository.findByTrainIdAndJourneyDate(10L, JOURNEY_DATE))
                .thenReturn(Optional.of(trainRun));
        when(stationRepository.findByStationCodeIgnoreCase("NDLS")).thenReturn(Optional.of(source));
        when(stationRepository.findByStationCodeIgnoreCase("CNB")).thenReturn(Optional.of(connection));
        when(stationRepository.findByStationCodeIgnoreCase("LKO")).thenReturn(Optional.of(destination));
        when(stationRepository.findByStationCodeIgnoreCase("MISSING")).thenReturn(Optional.empty());
        when(trainClassRepository.findByTrainIdAndClassCode(10L, TrainClassCode.SL))
                .thenReturn(Optional.of(trainClass));
        when(trainStopRepository.findByTrainIdOrderByStopNumberAsc(10L))
                .thenReturn(List.of(sourceStop, connectionStop, destinationStop));
        when(seatRepository.countInventorySeats(100L, 50L)).thenReturn(100L);
        when(seatAllocationRepository.countOccupiedSeatsForOverlappingSegments(
                100L, 50L, 1, 3, List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)))
                .thenReturn(25L);
    }

    @Test
    void returnsTotalAvailableSeatsAndFareForValidJourney() {
        TrainAvailabilityResponse result =
                availabilityService.getAvailability(10L, "NDLS", "LKO", JOURNEY_DATE, "SL");

        assertEquals(100L, result.trainRunId());
        assertEquals(100L, result.totalSeats());
        assertEquals(75L, result.availableSeats());
        assertEquals(new BigDecimal("250.75"), result.distance());
        assertEquals(new BigDecimal("350.75"), result.farePerPassenger());
        assertEquals("INR", result.currency());
    }

    @Test
    void rejectsUnknownTrain() {
        when(trainRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(TrainNotFoundException.class,
                () -> availabilityService.getAvailability(999L, "NDLS", "LKO", JOURNEY_DATE, "SL"));
    }

    @Test
    void rejectsPastJourneyDate() {
        assertThrows(InvalidRequestException.class,
                () -> availabilityService.getAvailability(
                        10L, "NDLS", "LKO", LocalDate.now().minusDays(1), "SL"));
    }

    @Test
    void rejectsAvailabilityAfterTheTrainDepartedFromTheRequestedSource() {
        trainRun.setJourneyDate(LocalDate.now());
        sourceStop.setDepartureTime(LocalTime.MIN);

        assertThrows(TrainNotOperatingException.class,
                () -> availabilityService.getAvailability(10L, "NDLS", "LKO", LocalDate.now(), "SL"));
    }

    @Test
    void rejectsAvailabilityWhenDestinationArrivalIsUnscheduled() {
        destinationStop.setArrivalTime(null);

        assertThrows(TrainNotOperatingException.class,
                () -> availabilityService.getAvailability(10L, "NDLS", "LKO", JOURNEY_DATE, "SL"));
    }

    @Test
    void rejectsUnknownSourceStation() {
        when(stationRepository.findByStationCodeIgnoreCase("MISSING")).thenReturn(Optional.empty());

        assertThrows(StationNotFoundException.class,
                () -> availabilityService.getAvailability(10L, "MISSING", "LKO", JOURNEY_DATE, "SL"));
    }

    @Test
    void rejectsUnknownDestinationStation() {
        when(stationRepository.findByStationCodeIgnoreCase("MISSING")).thenReturn(Optional.empty());

        assertThrows(StationNotFoundException.class,
                () -> availabilityService.getAvailability(10L, "NDLS", "MISSING", JOURNEY_DATE, "SL"));
    }

    @Test
    void rejectsInvalidRouteOrderAndIdenticalStations() {
        assertThrows(InvalidRouteException.class,
                () -> availabilityService.getAvailability(10L, "NDLS", "NDLS", JOURNEY_DATE, "SL"));

        sourceStop.setStopNumber(3);
        destinationStop.setStopNumber(1);
        assertThrows(InvalidRouteException.class,
                () -> availabilityService.getAvailability(10L, "NDLS", "LKO", JOURNEY_DATE, "SL"));
    }

    @Test
    void rejectsTrainWithoutRunOnRequestedDate() {
        when(trainRunRepository.findByTrainIdAndJourneyDate(10L, JOURNEY_DATE)).thenReturn(Optional.empty());

        assertThrows(TrainNotOperatingException.class,
                () -> availabilityService.getAvailability(10L, "NDLS", "LKO", JOURNEY_DATE, "SL"));
    }

    @Test
    void rejectsCancelledRun() {
        trainRun.setRunningStatus(RunningStatus.CANCELLED);

        assertThrows(TrainNotOperatingException.class,
                () -> availabilityService.getAvailability(10L, "NDLS", "LKO", JOURNEY_DATE, "SL"));
    }

    @Test
    void rejectsInvalidClassCode() {
        assertThrows(InvalidTrainClassException.class,
                () -> availabilityService.getAvailability(10L, "NDLS", "LKO", JOURNEY_DATE, "UNKNOWN"));
    }

    @Test
    void rejectsClassThatDoesNotBelongToTrain() {
        when(trainClassRepository.findByTrainIdAndClassCode(10L, TrainClassCode.SL))
                .thenReturn(Optional.empty());

        assertThrows(InvalidTrainClassException.class,
                () -> availabilityService.getAvailability(10L, "NDLS", "LKO", JOURNEY_DATE, "SL"));
    }

    @Test
    void countsOnlyRequestedRunAndClassSeatInventory() {
        availabilityService.getAvailability(10L, "NDLS", "LKO", JOURNEY_DATE, "SL");

        verify(seatRepository).countInventorySeats(100L, 50L);
        verify(seatAllocationRepository).countOccupiedSeatsForOverlappingSegments(
                100L, 50L, 1, 3, List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED));
    }

    @Test
    void usesHalfOpenSegmentRangeSoConsecutiveJourneysCanReuseASeat() {
        when(seatAllocationRepository.countOccupiedSeatsForOverlappingSegments(
                100L, 50L, 2, 3, List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)))
                .thenReturn(0L);

        TrainAvailabilityResponse result =
                availabilityService.getAvailability(10L, "CNB", "LKO", JOURNEY_DATE, "SL");

        assertEquals(100L, result.availableSeats());
        verify(seatAllocationRepository).countOccupiedSeatsForOverlappingSegments(
                100L, 50L, 2, 3, List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED));
    }

    @Test
    void returnsZeroAvailabilityWhenNoCoachOrSeatInventoryExists() {
        when(seatRepository.countInventorySeats(100L, 50L)).thenReturn(0L);

        TrainAvailabilityResponse result =
                availabilityService.getAvailability(10L, "NDLS", "LKO", JOURNEY_DATE, "SL");

        assertEquals(0L, result.totalSeats());
        assertEquals(0L, result.availableSeats());
        verify(seatAllocationRepository, never()).countOccupiedSeatsForOverlappingSegments(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt(),
                anyCollection());
    }

    @Test
    void rejectsInactiveTrain() {
        train.setActive(false);

        assertThrows(TrainNotOperatingException.class,
                () -> availabilityService.getAvailability(10L, "NDLS", "LKO", JOURNEY_DATE, "SL"));
    }

    private Station station(Long id, String code, String name) {
        Station station = new Station();
        station.setId(id);
        station.setStationCode(code);
        station.setStationName(name);
        return station;
    }

    private TrainStop stop(Station station, int stopNumber, String distance) {
        TrainStop stop = new TrainStop();
        stop.setTrain(train);
        stop.setStation(station);
        stop.setStopNumber(stopNumber);
        stop.setDayNumber(1);
        stop.setArrivalTime(LocalTime.NOON);
        stop.setDepartureTime(LocalTime.NOON);
        stop.setDistanceFromSource(new BigDecimal(distance));
        return stop;
    }
}
