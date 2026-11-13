package com.trainreservation.backend.service.impl;

import com.trainreservation.backend.dto.request.StationRequest;
import com.trainreservation.backend.dto.response.StationPageResponse;
import com.trainreservation.backend.dto.response.StationResponse;
import com.trainreservation.backend.entity.Station;
import com.trainreservation.backend.exception.DuplicateStationCodeException;
import com.trainreservation.backend.exception.InvalidRequestException;
import com.trainreservation.backend.exception.StationNotFoundException;
import com.trainreservation.backend.repository.StationRepository;
import com.trainreservation.backend.service.StationManagementService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class StationManagementServiceImpl implements StationManagementService {

    private static final Set<String> SORT_FIELDS = Set.of(
            "id", "stationCode", "stationName", "city", "state", "active");

    private final StationRepository stationRepository;

    public StationManagementServiceImpl(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    @Override
    @Transactional
    public StationResponse createStation(StationRequest request) {
        Station station = new Station();
        applyRequest(station, request);
        if (stationRepository.existsByStationCodeIgnoreCase(station.getStationCode())) {
            throw new DuplicateStationCodeException(station.getStationCode());
        }
        return toResponse(stationRepository.saveAndFlush(station));
    }

    @Override
    @Transactional(readOnly = true)
    public StationPageResponse getStations(int page, int size, String sortBy, String direction) {
        if (page < 0 || size < 1 || size > 100) {
            throw new InvalidRequestException("Page must be non-negative and size must be between 1 and 100");
        }
        if (sortBy == null || !SORT_FIELDS.contains(sortBy)) {
            throw new InvalidRequestException("Unsupported station sort field");
        }
        Sort.Direction sortDirection;
        try {
            sortDirection = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException("Sort direction must be asc or desc");
        }

        Sort sort = Sort.by(sortDirection, sortBy);
        if (!"id".equals(sortBy)) {
            sort = sort.and(Sort.by("id"));
        }
        Page<Station> stationPage = stationRepository.findAll(PageRequest.of(page, size, sort));
        List<StationResponse> stations = stationPage.getContent().stream()
                .map(this::toResponse)
                .toList();
        return new StationPageResponse(stations, stationPage.getNumber(), stationPage.getSize(),
                stationPage.getTotalElements(), stationPage.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public StationResponse getStationById(Long id) {
        return stationRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new StationNotFoundException(String.valueOf(id)));
    }

    @Override
    @Transactional
    public StationResponse updateStation(Long id, StationRequest request) {
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new StationNotFoundException(String.valueOf(id)));
        applyRequest(station, request);
        if (stationRepository.existsByStationCodeIgnoreCaseAndIdNot(station.getStationCode(), id)) {
            throw new DuplicateStationCodeException(station.getStationCode());
        }
        return toResponse(stationRepository.saveAndFlush(station));
    }

    @Override
    @Transactional
    public StationResponse deactivateStation(Long id) {
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new StationNotFoundException(String.valueOf(id)));
        if (station.isActive()) {
            station.setActive(false);
            station = stationRepository.saveAndFlush(station);
        }
        return toResponse(station);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StationResponse> searchStations(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        String searchTerm = keyword.trim();
        return stationRepository
                .findByStationCodeContainingIgnoreCaseOrStationNameContainingIgnoreCaseOrCityContainingIgnoreCase(
                        searchTerm, searchTerm, searchTerm, Sort.by("stationName").and(Sort.by("id")))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void applyRequest(Station station, StationRequest request) {
        if (request == null) {
            throw new InvalidRequestException("Station details are required");
        }
        String stationCode = request.stationCode().trim().toUpperCase(Locale.ROOT);
        station.setStationName(request.stationName().trim());
        station.setStationCode(stationCode);
        station.setCity(request.city().trim());
        station.setState(request.state().trim());
    }

    private StationResponse toResponse(Station station) {
        return new StationResponse(station.getId(), station.getStationName(), station.getStationCode(),
                station.getCity(), station.getState(), station.isActive());
    }
}
