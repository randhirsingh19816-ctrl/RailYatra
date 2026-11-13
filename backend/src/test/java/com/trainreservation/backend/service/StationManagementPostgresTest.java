package com.trainreservation.backend.service;

import com.trainreservation.backend.dto.response.StationResponse;
import com.trainreservation.backend.entity.Station;
import com.trainreservation.backend.entity.Train;
import com.trainreservation.backend.entity.TrainStop;
import com.trainreservation.backend.enums.TrainType;
import com.trainreservation.backend.repository.StationRepository;
import com.trainreservation.backend.repository.TrainRepository;
import com.trainreservation.backend.repository.TrainStopRepository;
import com.trainreservation.backend.service.impl.StationManagementServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(StationManagementServiceImpl.class)
class StationManagementPostgresTest {

    @Autowired
    private StationManagementServiceImpl stationService;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private TrainRepository trainRepository;

    @Autowired
    private TrainStopRepository trainStopRepository;

    @Test
    void postgresUniqueConstraintRejectsDuplicateStationCode() {
        String code = stationCode();
        stationRepository.saveAndFlush(station(code));

        assertThrows(DataIntegrityViolationException.class,
                () -> stationRepository.saveAndFlush(station(code)));
    }

    @Test
    void deactivationPreservesStationAndExistingTrainRouteReference() {
        Station station = stationRepository.saveAndFlush(station(stationCode()));
        Train train = new Train();
        train.setTrainNumber("T" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        train.setTrainName("Station Management Test Train");
        train.setTrainType(TrainType.EXPRESS);
        train.setActive(true);
        trainRepository.saveAndFlush(train);

        TrainStop stop = new TrainStop();
        stop.setTrain(train);
        stop.setStation(station);
        stop.setStopNumber(1);
        stop.setDayNumber(1);
        stop.setDistanceFromSource(BigDecimal.ZERO);
        trainStopRepository.saveAndFlush(stop);

        StationResponse first = stationService.deactivateStation(station.getId());
        StationResponse repeated = stationService.deactivateStation(station.getId());

        assertFalse(first.active());
        assertFalse(repeated.active());
        assertTrue(stationRepository.existsById(station.getId()));
        List<TrainStop> persistedStops = trainStopRepository.findByTrainIdOrderByStopNumberAsc(train.getId());
        assertEquals(1, persistedStops.size());
        assertEquals(station.getId(), persistedStops.get(0).getStation().getId());
        assertFalse(persistedStops.get(0).getStation().isActive());
    }

    @Test
    void postgresSearchSupportsPartialCaseInsensitiveNameCodeAndCityMatches() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 7).toUpperCase();
        Station byName = station("N" + suffix);
        byName.setStationName("Needle " + suffix + " Terminal");
        Station byCode = station("C" + suffix);
        byCode.setStationName("Remote Terminal");
        byCode.setCity("Lakeview " + suffix);
        stationRepository.saveAllAndFlush(List.of(byName, byCode));

        assertEquals(List.of(byName.getId()), stationService.searchStations("needle " + suffix).stream()
                .map(StationResponse::id).toList());
        assertEquals(List.of(byCode.getId()), stationService.searchStations("C" + suffix).stream()
                .map(StationResponse::id).toList());
        assertEquals(List.of(byCode.getId()), stationService.searchStations("lakeview " + suffix).stream()
                .map(StationResponse::id).toList());
        assertTrue(stationService.searchStations("NoMatch" + suffix).isEmpty());
    }

    @Test
    void postgresStationListingAppliesPagination() {
        Station first = station(stationCode());
        first.setStationName("A Station " + UUID.randomUUID());
        Station second = station(stationCode());
        second.setStationName("Z Station " + UUID.randomUUID());
        stationRepository.saveAllAndFlush(List.of(first, second));

        var page = stationService.getStations(0, 10, "stationName", "asc");

        assertEquals(10, page.size());
        assertTrue(page.totalElements() >= 2);
        assertTrue(page.stations().size() <= 10);
    }

    private Station station(String code) {
        Station station = new Station();
        station.setStationCode(code);
        station.setStationName("Station " + code);
        station.setCity("Test City");
        station.setState("Test State");
        return station;
    }

    private String stationCode() {
        return "S" + UUID.randomUUID().toString().replace("-", "").substring(0, 9).toUpperCase();
    }
}
