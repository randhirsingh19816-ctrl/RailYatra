package com.trainreservation.backend.service.impl;

import com.trainreservation.backend.dto.request.StationRequest;
import com.trainreservation.backend.dto.response.StationPageResponse;
import com.trainreservation.backend.dto.response.StationResponse;
import com.trainreservation.backend.entity.Station;
import com.trainreservation.backend.exception.DuplicateStationCodeException;
import com.trainreservation.backend.exception.InvalidRequestException;
import com.trainreservation.backend.exception.StationNotFoundException;
import com.trainreservation.backend.repository.StationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StationManagementServiceImplTest {

    private StationRepository stationRepository;
    private StationManagementServiceImpl stationService;

    @BeforeEach
    void setUp() {
        stationRepository = mock(StationRepository.class);
        stationService = new StationManagementServiceImpl(stationRepository);
    }

    @Test
    void createsStationWithUppercaseCodeAndActiveStatus() {
        when(stationRepository.existsByStationCodeIgnoreCase("NDLS")).thenReturn(false);
        when(stationRepository.saveAndFlush(any(Station.class))).thenAnswer(invocation -> {
            Station station = invocation.getArgument(0);
            station.setId(7L);
            return station;
        });

        StationResponse response = stationService.createStation(request(" ndls "));

        assertEquals(7L, response.id());
        assertEquals("NDLS", response.stationCode());
        assertEquals("New Delhi", response.stationName());
        assertTrue(response.active());
    }

    @Test
    void rejectsDuplicateStationCodeWithoutSaving() {
        when(stationRepository.existsByStationCodeIgnoreCase("NDLS")).thenReturn(true);

        assertThrows(DuplicateStationCodeException.class, () -> stationService.createStation(request("ndls")));

        verify(stationRepository, never()).saveAndFlush(any());
    }

    @Test
    void retrievesStationAndReturnsNotFoundForUnknownId() {
        Station station = station(10L, "NDLS");
        when(stationRepository.findById(10L)).thenReturn(Optional.of(station));

        assertEquals("NDLS", stationService.getStationById(10L).stationCode());
        assertThrows(StationNotFoundException.class, () -> stationService.getStationById(999L));
    }

    @Test
    void updatesStationWithoutChangingItsId() {
        Station station = station(10L, "NDLS");
        when(stationRepository.findById(10L)).thenReturn(Optional.of(station));
        when(stationRepository.existsByStationCodeIgnoreCaseAndIdNot("NDLS", 10L)).thenReturn(false);
        when(stationRepository.saveAndFlush(station)).thenReturn(station);

        StationResponse response = stationService.updateStation(10L, request("ndls"));

        assertEquals(10L, response.id());
        assertEquals("New Delhi", response.stationName());
        verify(stationRepository).existsByStationCodeIgnoreCaseAndIdNot("NDLS", 10L);
    }

    @Test
    void rejectsDuplicateCodeWhenUpdatingAnotherStation() {
        Station station = station(10L, "NDLS");
        when(stationRepository.findById(10L)).thenReturn(Optional.of(station));
        when(stationRepository.existsByStationCodeIgnoreCaseAndIdNot("NDLS", 10L)).thenReturn(true);

        assertThrows(DuplicateStationCodeException.class,
                () -> stationService.updateStation(10L, request("ndls")));

        verify(stationRepository, never()).saveAndFlush(any());
    }

    @Test
    void deactivationIsIdempotentAndDoesNotDeleteStation() {
        Station station = station(10L, "NDLS");
        when(stationRepository.findById(10L)).thenReturn(Optional.of(station));
        when(stationRepository.saveAndFlush(station)).thenReturn(station);

        assertFalse(stationService.deactivateStation(10L).active());
        assertFalse(stationService.deactivateStation(10L).active());

        verify(stationRepository).saveAndFlush(station);
        verify(stationRepository, never()).delete(any());
    }

    @Test
    void returnsPartialSearchMatchesAndEmptyResults() {
        Station station = station(10L, "NDLS");
        when(stationRepository
                .findByStationCodeContainingIgnoreCaseOrStationNameContainingIgnoreCaseOrCityContainingIgnoreCase(
                        "Delhi", "Delhi", "Delhi", Sort.by("stationName").and(Sort.by("id"))))
                .thenReturn(List.of(station));

        assertEquals(List.of("NDLS"), stationService.searchStations(" Delhi ").stream()
                .map(StationResponse::stationCode).toList());
        when(stationRepository
                .findByStationCodeContainingIgnoreCaseOrStationNameContainingIgnoreCaseOrCityContainingIgnoreCase(
                        "Missing", "Missing", "Missing", Sort.by("stationName").and(Sort.by("id"))))
                .thenReturn(List.of());
        when(stationRepository
                .findByStationCodeContainingIgnoreCaseOrStationNameContainingIgnoreCaseOrCityContainingIgnoreCase(
                        "NDL", "NDL", "NDL", Sort.by("stationName").and(Sort.by("id"))))
                .thenReturn(List.of(station));
        assertTrue(stationService.searchStations("Missing").isEmpty());
        assertEquals(1, stationService.searchStations("NDL").size());
        assertTrue(stationService.searchStations("  ").isEmpty());
    }

    @Test
    void paginatesAndSortsByRequestedField() {
        Station station = station(10L, "NDLS");
        Pageable pageable = PageRequest.of(0, 10,
                Sort.by(Sort.Direction.DESC, "stationName").and(Sort.by("id")));
        when(stationRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(station), pageable, 1));

        StationPageResponse response = stationService.getStations(0, 10, "stationName", "desc");

        assertEquals(1, response.totalElements());
        assertEquals(0, response.page());
        assertEquals("NDLS", response.stations().get(0).stationCode());
        verify(stationRepository).findAll(pageable);
    }

    @Test
    void rejectsInvalidPaginationAndSortFields() {
        assertThrows(InvalidRequestException.class,
                () -> stationService.getStations(-1, 10, "stationName", "asc"));
        assertThrows(InvalidRequestException.class,
                () -> stationService.getStations(0, 101, "stationName", "asc"));
        assertThrows(InvalidRequestException.class,
                () -> stationService.getStations(0, 10, "password", "asc"));
        assertThrows(InvalidRequestException.class,
                () -> stationService.getStations(0, 10, null, "asc"));
        assertThrows(InvalidRequestException.class,
                () -> stationService.getStations(0, 10, "stationName", "sideways"));
    }

    private StationRequest request(String stationCode) {
        return new StationRequest("New Delhi", stationCode, "New Delhi", "Delhi");
    }

    private Station station(Long id, String code) {
        Station station = new Station();
        station.setId(id);
        station.setStationCode(code);
        station.setStationName("New Delhi");
        station.setCity("New Delhi");
        station.setState("Delhi");
        return station;
    }
}
