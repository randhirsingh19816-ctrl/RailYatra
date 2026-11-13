package com.trainreservation.backend.service.impl;

import com.trainreservation.backend.dto.response.TrainDetailsResponse;
import com.trainreservation.backend.dto.response.TrainSearchResponse;
import com.trainreservation.backend.entity.Station;
import com.trainreservation.backend.entity.Train;
import com.trainreservation.backend.entity.TrainClass;
import com.trainreservation.backend.entity.TrainRun;
import com.trainreservation.backend.entity.TrainStop;
import com.trainreservation.backend.enums.RunningStatus;
import com.trainreservation.backend.enums.TrainClassCode;
import com.trainreservation.backend.enums.TrainType;
import com.trainreservation.backend.exception.InvalidRequestException;
import com.trainreservation.backend.exception.InvalidRouteException;
import com.trainreservation.backend.exception.StationNotFoundException;
import com.trainreservation.backend.repository.StationRepository;
import com.trainreservation.backend.repository.TrainClassRepository;
import com.trainreservation.backend.repository.TrainRepository;
import com.trainreservation.backend.repository.TrainRunRepository;
import com.trainreservation.backend.repository.TrainStopRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TrainServiceImplTest {

    private static final LocalDate JOURNEY_DATE = LocalDate.of(2026, 12, 1);

    private StationRepository stationRepository;
    private TrainRepository trainRepository;
    private TrainRunRepository trainRunRepository;
    private TrainStopRepository trainStopRepository;
    private TrainClassRepository trainClassRepository;
    private TrainServiceImpl trainService;
    private Station source;
    private Station destination;
    private Train train;
    private TrainRun trainRun;
    private TrainStop sourceStop;
    private TrainStop destinationStop;
    private TrainClass sleeperClass;

    @BeforeEach
    void setUp() {
        stationRepository = mock(StationRepository.class);
        trainRepository = mock(TrainRepository.class);
        trainRunRepository = mock(TrainRunRepository.class);
        trainStopRepository = mock(TrainStopRepository.class);
        trainClassRepository = mock(TrainClassRepository.class);
        trainService = new TrainServiceImpl(
                new TrainRouteLookupService(stationRepository),
                trainRepository,
                trainRunRepository,
                trainStopRepository,
                trainClassRepository);

        source = station(1L, "NDLS", "New Delhi");
        destination = station(2L, "CNB", "Kanpur Central");
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

        sourceStop = stop(source, 1, 1, LocalTime.of(23, 0), LocalTime.of(23, 10), "0");
        destinationStop = stop(destination, 2, 2, LocalTime.of(4, 0), LocalTime.of(4, 5), "180.50");

        sleeperClass = new TrainClass();
        sleeperClass.setTrain(train);
        sleeperClass.setClassCode(TrainClassCode.SL);
        sleeperClass.setClassName("Sleeper");

        when(stationRepository.findByStationCodeIgnoreCase("NDLS")).thenReturn(Optional.of(source));
        when(stationRepository.findByStationCodeIgnoreCase("CNB")).thenReturn(Optional.of(destination));
        when(stationRepository.findByStationCodeIgnoreCase("2")).thenReturn(Optional.empty());
        when(stationRepository.findById(2L)).thenReturn(Optional.of(destination));
        when(trainRunRepository.findByJourneyDateAndRunningStatusInOrderByTrainIdAsc(
                any(LocalDate.class), anyCollection())).thenReturn(List.of(trainRun));
        when(trainStopRepository.findByTrainIdOrderByStopNumberAsc(10L))
                .thenReturn(List.of(sourceStop, destinationStop));
        when(trainClassRepository.findByTrainId(10L)).thenReturn(List.of(sleeperClass));
    }

    @Test
    void searchesByStationCodeAndStationIdAndUsesRunDateAndStopDays() {
        List<TrainSearchResponse> results = trainService.searchTrains("NDLS", "2", JOURNEY_DATE, "sl");

        assertEquals(1, results.size());
        TrainSearchResponse result = results.get(0);
        assertEquals(10L, result.trainId());
        assertEquals(100L, result.trainRunId());
        assertEquals(JOURNEY_DATE.atTime(23, 10), result.departureTime());
        assertEquals(JOURNEY_DATE.plusDays(1).atTime(4, 0), result.arrivalTime());
        assertEquals(290, result.durationMinutes());
        assertEquals(new BigDecimal("180.50"), result.distance());
        assertEquals(List.of("SL"), result.availableClasses().stream()
                .map(trainClass -> trainClass.classCode()).toList());
    }

    @Test
    void rejectsUnknownSourceStation() {
        when(stationRepository.findByStationCodeIgnoreCase("MISSING")).thenReturn(Optional.empty());

        assertThrows(StationNotFoundException.class,
                () -> trainService.searchTrains("MISSING", "CNB", JOURNEY_DATE, null));
    }

    @Test
    void rejectsPastJourneyDate() {
        assertThrows(InvalidRequestException.class,
                () -> trainService.searchTrains("NDLS", "CNB", LocalDate.now().minusDays(1), null));
    }

    @Test
    void excludesTrainThatAlreadyDepartedFromTheRequestedSource() {
        trainRun.setJourneyDate(LocalDate.now());
        sourceStop.setDepartureTime(LocalTime.MIN);

        assertEquals(0, trainService.searchTrains("NDLS", "CNB", LocalDate.now(), null).size());
    }

    @Test
    void rejectsUnknownDestinationStation() {
        when(stationRepository.findByStationCodeIgnoreCase("MISSING")).thenReturn(Optional.empty());

        assertThrows(StationNotFoundException.class,
                () -> trainService.searchTrains("NDLS", "MISSING", JOURNEY_DATE, null));
    }

    @Test
    void rejectsIdenticalStations() {
        assertThrows(InvalidRouteException.class,
                () -> trainService.searchTrains("NDLS", "NDLS", JOURNEY_DATE, null));
    }

    @Test
    void excludesTrainsWhenDestinationAppearsBeforeSource() {
        sourceStop.setStopNumber(3);
        destinationStop.setStopNumber(2);

        assertEquals(0, trainService.searchTrains("NDLS", "CNB", JOURNEY_DATE, null).size());
    }

    @Test
    void filtersOutTrainsWithoutTheRequestedClass() {
        assertEquals(0, trainService.searchTrains("NDLS", "CNB", JOURNEY_DATE, "3A").size());
    }

    @Test
    void rejectsUnsupportedClassCode() {
        assertThrows(InvalidRequestException.class,
                () -> trainService.searchTrains("NDLS", "CNB", JOURNEY_DATE, "INVALID"));
    }

    @Test
    void doesNotReturnDuplicateTrains() {
        when(trainRunRepository.findByJourneyDateAndRunningStatusInOrderByTrainIdAsc(
                any(LocalDate.class), anyCollection())).thenReturn(List.of(trainRun, trainRun));

        assertEquals(1, trainService.searchTrains("NDLS", "CNB", JOURNEY_DATE, null).size());
    }

    @Test
    void returnsTrainDetailsWithOrderedRouteStops() {
        when(trainRepository.findById(10L)).thenReturn(Optional.of(train));
        when(trainStopRepository.findByTrainIdOrderByStopNumberAsc(10L))
                .thenReturn(List.of(destinationStop, sourceStop));

        TrainDetailsResponse details = trainService.getTrainDetails(10L);

        assertEquals(10L, details.id());
        assertEquals(List.of(1, 2), details.routeStops().stream()
                .map(stop -> stop.stopNumber()).toList());
        assertEquals("NDLS", details.routeStops().get(0).stationCode());
    }

    @Test
    void doesNotReturnInactiveTrainsInSearch() {
        train.setActive(false);

        assertEquals(0, trainService.searchTrains("NDLS", "CNB", JOURNEY_DATE, null).size());
    }

    private Station station(Long id, String code, String name) {
        Station station = new Station();
        station.setId(id);
        station.setStationCode(code);
        station.setStationName(name);
        return station;
    }

    private TrainStop stop(Station station, int stopNumber, int dayNumber,
                          LocalTime arrivalTime, LocalTime departureTime, String distance) {
        TrainStop stop = new TrainStop();
        stop.setTrain(train);
        stop.setStation(station);
        stop.setStopNumber(stopNumber);
        stop.setDayNumber(dayNumber);
        stop.setArrivalTime(arrivalTime);
        stop.setDepartureTime(departureTime);
        stop.setDistanceFromSource(new BigDecimal(distance));
        return stop;
    }
}
