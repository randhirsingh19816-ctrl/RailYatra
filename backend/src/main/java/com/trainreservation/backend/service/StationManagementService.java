package com.trainreservation.backend.service;

import com.trainreservation.backend.dto.request.StationRequest;
import com.trainreservation.backend.dto.response.StationPageResponse;
import com.trainreservation.backend.dto.response.StationResponse;

import java.util.List;

public interface StationManagementService {

    StationResponse createStation(StationRequest request);

    StationPageResponse getStations(int page, int size, String sortBy, String direction);

    StationResponse getStationById(Long id);

    StationResponse updateStation(Long id, StationRequest request);

    StationResponse deactivateStation(Long id);

    List<StationResponse> searchStations(String keyword);
}
