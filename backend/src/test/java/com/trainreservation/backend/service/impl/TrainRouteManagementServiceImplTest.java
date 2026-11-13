package com.trainreservation.backend.service.impl;

import com.trainreservation.backend.dto.request.AddTrainRouteStopRequest;
import com.trainreservation.backend.dto.request.TrainRouteRequest;
import com.trainreservation.backend.dto.request.TrainRouteStopRequest;
import com.trainreservation.backend.dto.response.TrainRouteResponse;
import com.trainreservation.backend.entity.Station;
import com.trainreservation.backend.entity.Train;
import com.trainreservation.backend.entity.TrainStop;
import com.trainreservation.backend.exception.InvalidRouteException;
import com.trainreservation.backend.exception.StationNotFoundException;
import com.trainreservation.backend.exception.TrainNotFoundException;
import com.trainreservation.backend.exception.TrainRouteConflictException;
import com.trainreservation.backend.exception.TrainRouteNotFoundException;
import com.trainreservation.backend.exception.TrainRouteStopNotFoundException;
import com.trainreservation.backend.repository.StationRepository;
import com.trainreservation.backend.repository.TrainRepository;
import com.trainreservation.backend.repository.TrainRunRepository;
import com.trainreservation.backend.repository.TrainStopRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainRouteManagementServiceImplTest {

    @Mock
    private TrainRepository trainRepository;

    @Mock
    private TrainStopRepository trainStopRepository;

    @Mock
    private StationRepository stationRepository;

    @Mock
    private TrainRunRepository trainRunRepository;

    private TrainRouteManagementServiceImpl service;
    private Train train;
    private Station origin;
    private Station middle;
    private Station destination;

    @BeforeEach
    void setUp() {
        service = new TrainRouteManagementServiceImpl(
                trainRepository, trainStopRepository, stationRepository, trainRunRepository);
        train = new Train();
        train.setId(10L);
        train.setTrainNumber("T100");
        train.setActive(true);
        origin = station(1L, "NDLS", true);
        middle = station(2L, "KOTA", true);
        destination = station(3L, "MMCT", true);

        lenient().when(trainRepository.findByIdForRouteUpdate(10L)).thenReturn(Optional.of(train));
        lenient().when(trainRunRepository.existsByTrainId(10L)).thenReturn(false);
    }

    @Test
    void createRoutePersistsValidatedStopsInOrder() {
        when(trainStopRepository.existsByTrainId(10L)).thenReturn(false);
        stubStationLookups(origin, destination);
        when(trainStopRepository.saveAllAndFlush(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TrainRouteResponse result = service.createRoute(10L, new TrainRouteRequest(List.of(
                request("NDLS", 1, null, time("08:00"), 1, "0"),
                request("MMCT", 2, time("10:00"), null, 1, "100"))));

        assertEquals(2, result.stops().size());
        assertEquals("NDLS", result.stops().get(0).stationCode());
        assertEquals("MMCT", result.stops().get(1).stationCode());
        verify(trainStopRepository).saveAllAndFlush(anyList());
    }

    @Test
    void createRouteAllowsOvernightTravelWhenDaysAreMonotonic() {
        when(trainStopRepository.existsByTrainId(10L)).thenReturn(false);
        stubStationLookups(origin, middle, destination);
        when(trainStopRepository.saveAllAndFlush(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TrainRouteResponse result = service.createRoute(10L, new TrainRouteRequest(List.of(
                request("NDLS", 1, null, time("23:00"), 1, "0"),
                request("KOTA", 2, time("01:00"), time("01:10"), 2, "200"),
                request("MMCT", 3, time("04:00"), null, 2, "500"))));

        assertEquals(3, result.stops().size());
    }

    @Test
    void createRouteRejectsMissingTrainInactiveTrainAndDuplicateRoute() {
        when(trainRepository.findByIdForRouteUpdate(10L)).thenReturn(Optional.empty());
        assertThrows(TrainNotFoundException.class, () -> service.createRoute(10L, twoStopRequest()));

        train.setActive(false);
        when(trainRepository.findByIdForRouteUpdate(10L)).thenReturn(Optional.of(train));
        assertThrows(TrainRouteConflictException.class, () -> service.createRoute(10L, twoStopRequest()));
        train.setActive(true);

        when(trainStopRepository.existsByTrainId(10L)).thenReturn(true);
        assertThrows(TrainRouteConflictException.class, () -> service.createRoute(10L, twoStopRequest()));

        when(trainStopRepository.existsByTrainId(10L)).thenReturn(false);
        when(trainRunRepository.existsByTrainId(10L)).thenReturn(true);
        assertThrows(TrainRouteConflictException.class, () -> service.createRoute(10L, twoStopRequest()));
    }

    @Test
    void createRouteRejectsUnknownAndInactiveStations() {
        when(trainStopRepository.existsByTrainId(10L)).thenReturn(false);
        when(stationRepository.findByStationCodeIgnoreCase("NDLS")).thenReturn(Optional.of(origin));
        when(stationRepository.findByStationCodeIgnoreCase("MMCT")).thenReturn(Optional.empty());
        assertThrows(StationNotFoundException.class, () -> service.createRoute(10L, twoStopRequest()));

        Station inactive = station(3L, "MMCT", false);
        when(stationRepository.findByStationCodeIgnoreCase("MMCT")).thenReturn(Optional.of(inactive));
        assertThrows(TrainRouteConflictException.class, () -> service.createRoute(10L, twoStopRequest()));
    }

    @Test
    void createRouteRejectsDuplicateStationsAndSequenceGaps() {
        when(trainStopRepository.existsByTrainId(10L)).thenReturn(false);
        stubStationLookups(origin, destination);

        assertThrows(TrainRouteConflictException.class, () -> service.createRoute(10L, new TrainRouteRequest(List.of(
                request("NDLS", 1, null, time("08:00"), 1, "0"),
                request("NDLS", 2, time("10:00"), null, 1, "100")))));

        assertThrows(InvalidRouteException.class, () -> service.createRoute(10L, new TrainRouteRequest(List.of(
                request("NDLS", 1, null, time("08:00"), 1, "0"),
                request("MMCT", 3, time("10:00"), null, 1, "100")))));
    }

    @Test
    void createRouteRejectsInvalidBoundaryTimesDayTransitionsAndDistances() {
        when(trainStopRepository.existsByTrainId(10L)).thenReturn(false);
        stubStationLookups(origin, middle, destination);

        assertThrows(InvalidRouteException.class, () -> service.createRoute(10L, new TrainRouteRequest(List.of(
                request("NDLS", 1, time("07:00"), time("08:00"), 1, "0"),
                request("MMCT", 2, time("10:00"), null, 1, "100")))));
        assertThrows(InvalidRouteException.class, () -> service.createRoute(10L, new TrainRouteRequest(List.of(
                request("NDLS", 1, null, time("08:00"), 1, "0"),
                request("MMCT", 2, time("10:00"), time("10:05"), 1, "100")))));
        assertThrows(InvalidRouteException.class, () -> service.createRoute(10L, new TrainRouteRequest(List.of(
                request("NDLS", 1, null, time("22:00"), 1, "0"),
                request("MMCT", 2, time("21:00"), null, 1, "100")))));
        assertThrows(InvalidRouteException.class, () -> service.createRoute(10L, new TrainRouteRequest(List.of(
                request("NDLS", 1, null, time("08:00"), 1, "0"),
                request("MMCT", 2, time("10:00"), null, 2, "0")))));
        assertThrows(InvalidRouteException.class, () -> service.createRoute(10L, new TrainRouteRequest(List.of(
                request("NDLS", 1, null, time("08:00"), 1, "0"),
                request("MMCT", 2, time("10:00"), null, 1, "-1")))));
    }

    @Test
    void getRouteReturnsStopsOrderedAndUnknownRouteIsNotFound() {
        when(trainRepository.findById(10L)).thenReturn(Optional.of(train));
        List<TrainStop> stops = new ArrayList<>(List.of(
                stop(destination, 2, time("10:00"), null, 1, "100"),
                stop(origin, 1, null, time("08:00"), 1, "0")));
        when(trainStopRepository.findByTrainIdOrderByStopNumberAsc(10L)).thenReturn(stops);

        TrainRouteResponse result = service.getRoute(10L);

        assertEquals("NDLS", result.stops().get(0).stationCode());
        assertEquals("MMCT", result.stops().get(1).stationCode());
        when(trainStopRepository.findByTrainIdOrderByStopNumberAsc(10L)).thenReturn(List.of());
        assertThrows(TrainRouteNotFoundException.class, () -> service.getRoute(10L));
    }

    @Test
    void updateRouteValidatesReplacementBeforeReplacingExistingStops() {
        when(trainStopRepository.findByTrainIdOrderByStopNumberAsc(10L))
                .thenReturn(twoStops());
        stubStationLookups(origin, middle, destination);
        when(trainStopRepository.saveAllAndFlush(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TrainRouteResponse result = service.updateRoute(10L, new TrainRouteRequest(List.of(
                request("NDLS", 1, null, time("07:00"), 1, "0"),
                request("KOTA", 2, time("09:00"), time("09:10"), 1, "100"),
                request("MMCT", 3, time("11:00"), null, 1, "250"))));

        assertEquals(3, result.stops().size());
        verify(trainStopRepository).deleteAll(org.mockito.ArgumentMatchers.anyList());
        verify(trainStopRepository).flush();
    }

    @Test
    void updateRouteRejectsChangesAfterTrainRunExists() {
        when(trainStopRepository.findByTrainIdOrderByStopNumberAsc(10L)).thenReturn(twoStops());
        when(trainRunRepository.existsByTrainId(10L)).thenReturn(true);

        assertThrows(TrainRouteConflictException.class,
                () -> service.updateRoute(10L, twoStopRequest()));
        verify(trainStopRepository, never()).deleteAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void invalidRouteReplacementDoesNotDeleteExistingStops() {
        when(trainStopRepository.findByTrainIdOrderByStopNumberAsc(10L)).thenReturn(twoStops());
        stubStationLookups(origin, destination);
        TrainRouteRequest invalidReplacement = new TrainRouteRequest(List.of(
                request("NDLS", 1, null, time("08:00"), 1, "0"),
                request("MMCT", 2, time("10:00"), time("10:05"), 1, "100")));

        assertThrows(InvalidRouteException.class, () -> service.updateRoute(10L, invalidReplacement));
        verify(trainStopRepository, never()).deleteAll(org.mockito.ArgumentMatchers.anyList());
        verify(trainStopRepository, never()).saveAllAndFlush(anyList());
    }

    @Test
    void addStopAppendsAndUpdatesFormerDestinationDeparture() {
        List<TrainStop> existing = twoStops();
        when(trainStopRepository.findByTrainIdOrderByStopNumberAsc(10L)).thenReturn(existing);
        lenient().when(stationRepository.findByStationCodeIgnoreCase("KOTA"))
                .thenReturn(Optional.of(middle));
        when(trainStopRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(TrainStop.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TrainRouteResponse result = service.addStop(10L, new AddTrainRouteStopRequest(
                request("KOTA", 3, time("12:00"), null, 1, "200"), time("10:05")));

        assertEquals(3, result.stops().size());
        assertEquals(time("10:05"), existing.get(1).getDepartureTime());
        verify(trainStopRepository).save(existing.get(1));
    }

    @Test
    void addStopRejectsNonAppendSequenceAndDuplicateStation() {
        when(trainStopRepository.findByTrainIdOrderByStopNumberAsc(10L)).thenReturn(twoStops());
        lenient().when(stationRepository.findByStationCodeIgnoreCase("KOTA"))
                .thenReturn(Optional.of(middle));

        assertThrows(TrainRouteConflictException.class, () -> service.addStop(10L,
                new AddTrainRouteStopRequest(
                        request("KOTA", 2, time("12:00"), null, 1, "200"), time("10:05"))));

        when(stationRepository.findByStationCodeIgnoreCase("NDLS")).thenReturn(Optional.of(origin));
        assertThrows(TrainRouteConflictException.class, () -> service.addStop(10L,
                new AddTrainRouteStopRequest(
                        request("NDLS", 3, time("12:00"), null, 1, "200"), time("10:05"))));
    }

    @Test
    void updateStopPreservesSequenceAndValidatesWholeRoute() {
        List<TrainStop> stops = threeStops();
        TrainStop target = stops.get(1);
        when(trainStopRepository.findByTrainIdOrderByStopNumberAsc(10L)).thenReturn(stops);
        when(trainStopRepository.findByIdAndTrainId(22L, 10L)).thenReturn(Optional.of(target));
        when(stationRepository.findByStationCodeIgnoreCase("KOTA")).thenReturn(Optional.of(middle));
        when(trainStopRepository.saveAndFlush(target)).thenReturn(target);

        TrainRouteResponse result = service.updateStop(10L, 22L,
                request("KOTA", 2, time("09:05"), time("09:15"), 1, "120"));

        assertEquals(new BigDecimal("120"), result.stops().get(1).distanceFromSource());
    }

    @Test
    void updateStopRejectsForeignStopAndSequenceReordering() {
        when(trainStopRepository.findByTrainIdOrderByStopNumberAsc(10L)).thenReturn(twoStops());
        when(trainStopRepository.findByIdAndTrainId(999L, 10L)).thenReturn(Optional.empty());
        assertThrows(TrainRouteStopNotFoundException.class, () -> service.updateStop(
                10L, 999L, request("NDLS", 1, null, time("08:00"), 1, "0")));

        TrainStop target = twoStops().get(0);
        when(trainStopRepository.findByIdAndTrainId(21L, 10L)).thenReturn(Optional.of(target));
        assertThrows(TrainRouteConflictException.class, () -> service.updateStop(
                10L, 21L, request("NDLS", 2, null, time("08:00"), 1, "0")));
    }

    @Test
    void removeStopRenumbersRouteAndMaintainsOriginAndDestinationRules() {
        List<TrainStop> stops = threeStops();
        TrainStop middleStop = stops.get(1);
        when(trainStopRepository.findByTrainIdOrderByStopNumberAsc(10L)).thenReturn(stops);
        when(trainStopRepository.findByIdAndTrainId(22L, 10L)).thenReturn(Optional.of(middleStop));

        TrainRouteResponse result = service.removeStop(10L, 22L);

        assertEquals(2, result.stops().size());
        assertEquals(2, result.stops().get(1).stopNumber());
        assertNull(result.stops().get(1).departureTime());
        verify(trainStopRepository).delete(middleStop);
    }

    @Test
    void removeOriginResetsDistanceAndDayOrigin() {
        List<TrainStop> stops = threeStops();
        TrainStop oldOrigin = stops.get(0);
        when(trainStopRepository.findByTrainIdOrderByStopNumberAsc(10L)).thenReturn(stops);
        when(trainStopRepository.findByIdAndTrainId(21L, 10L)).thenReturn(Optional.of(oldOrigin));

        TrainRouteResponse result = service.removeStop(10L, 21L);

        assertEquals(BigDecimal.ZERO, result.stops().get(0).distanceFromSource());
        assertNull(result.stops().get(0).arrivalTime());
        assertEquals(1, result.stops().get(0).dayNumber());
        verify(trainStopRepository).delete(oldOrigin);
    }

    @Test
    void removeStopRejectsTrainRunsAndRoutesThatWouldBecomeTooShort() {
        List<TrainStop> three = threeStops();
        when(trainStopRepository.findByTrainIdOrderByStopNumberAsc(10L)).thenReturn(three);
        when(trainStopRepository.findByIdAndTrainId(22L, 10L)).thenReturn(Optional.of(three.get(1)));
        when(trainRunRepository.existsByTrainId(10L)).thenReturn(true);
        assertThrows(TrainRouteConflictException.class, () -> service.removeStop(10L, 22L));

        when(trainRunRepository.existsByTrainId(10L)).thenReturn(false);
        List<TrainStop> two = twoStops();
        when(trainStopRepository.findByTrainIdOrderByStopNumberAsc(10L)).thenReturn(two);
        when(trainStopRepository.findByIdAndTrainId(22L, 10L)).thenReturn(Optional.of(two.get(1)));
        assertThrows(TrainRouteConflictException.class, () -> service.removeStop(10L, 22L));
    }

    private TrainRouteRequest twoStopRequest() {
        return new TrainRouteRequest(List.of(
                request("NDLS", 1, null, time("08:00"), 1, "0"),
                request("MMCT", 2, time("10:00"), null, 1, "100")));
    }

    private List<TrainStop> twoStops() {
        return new ArrayList<>(List.of(
                stop(origin, 1, null, time("08:00"), 1, "0"),
                stop(destination, 2, time("10:00"), null, 1, "100")));
    }

    private List<TrainStop> threeStops() {
        return new ArrayList<>(List.of(
                stop(origin, 1, null, time("08:00"), 1, "0"),
                stop(middle, 2, time("09:00"), time("09:10"), 1, "100"),
                stop(destination, 3, time("10:00"), null, 1, "200")));
    }

    private TrainStop stop(Station station, int number, LocalTime arrival, LocalTime departure,
                           int day, String distance) {
        TrainStop stop = new TrainStop();
        stop.setId(20L + number);
        stop.setTrain(train);
        stop.setStation(station);
        stop.setStopNumber(number);
        stop.setArrivalTime(arrival);
        stop.setDepartureTime(departure);
        stop.setDayNumber(day);
        stop.setDistanceFromSource(new BigDecimal(distance));
        return stop;
    }

    private TrainRouteStopRequest request(String code, int number, LocalTime arrival, LocalTime departure,
                                          int day, String distance) {
        return new TrainRouteStopRequest(code, number, arrival, departure, day, new BigDecimal(distance));
    }

    private Station station(Long id, String code, boolean active) {
        Station station = new Station();
        station.setId(id);
        station.setStationCode(code);
        station.setStationName(code + " Terminal");
        station.setCity("Test City");
        station.setState("Test State");
        station.setActive(active);
        return station;
    }

    private void stubStationLookups(Station... stations) {
        for (Station station : stations) {
            lenient().when(stationRepository.findByStationCodeIgnoreCase(station.getStationCode()))
                    .thenReturn(Optional.of(station));
        }
    }

    private LocalTime time(String value) {
        return LocalTime.parse(value);
    }
}
