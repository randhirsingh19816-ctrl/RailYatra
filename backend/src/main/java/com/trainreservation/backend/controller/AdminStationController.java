package com.trainreservation.backend.controller;

import com.trainreservation.backend.dto.request.StationRequest;
import com.trainreservation.backend.dto.response.ApiResponse;
import com.trainreservation.backend.dto.response.StationPageResponse;
import com.trainreservation.backend.dto.response.StationResponse;
import com.trainreservation.backend.service.StationManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/stations")
public class AdminStationController {

    private final StationManagementService stationManagementService;

    public AdminStationController(StationManagementService stationManagementService) {
        this.stationManagementService = stationManagementService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StationResponse>> createStation(
            @Valid @RequestBody StationRequest request) {
        StationResponse station = stationManagementService.createStation(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Station created successfully", station));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<StationPageResponse>> getStations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "stationName") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        StationPageResponse stations = stationManagementService.getStations(page, size, sortBy, direction);
        return ResponseEntity.ok(ApiResponse.success("Stations retrieved successfully", stations));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<StationResponse>>> searchStations(
            @RequestParam("keyword") String keyword) {
        List<StationResponse> stations = stationManagementService.searchStations(keyword);
        return ResponseEntity.ok(ApiResponse.success("Station search completed", stations));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StationResponse>> getStationById(@PathVariable Long id) {
        StationResponse station = stationManagementService.getStationById(id);
        return ResponseEntity.ok(ApiResponse.success("Station retrieved successfully", station));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StationResponse>> updateStation(
            @PathVariable Long id, @Valid @RequestBody StationRequest request) {
        StationResponse station = stationManagementService.updateStation(id, request);
        return ResponseEntity.ok(ApiResponse.success("Station updated successfully", station));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<StationResponse>> deactivateStation(@PathVariable Long id) {
        StationResponse station = stationManagementService.deactivateStation(id);
        return ResponseEntity.ok(ApiResponse.success("Station deactivated successfully", station));
    }
}
