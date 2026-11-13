package com.trainreservation.backend.controller;

import com.trainreservation.backend.dto.request.TrainRequest;
import com.trainreservation.backend.dto.response.ApiResponse;
import com.trainreservation.backend.dto.response.TrainPageResponse;
import com.trainreservation.backend.dto.response.TrainResponse;
import com.trainreservation.backend.service.TrainManagementService;
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

@RestController
@RequestMapping("/api/admin/trains")
public class AdminTrainController {

    private final TrainManagementService trainManagementService;

    public AdminTrainController(TrainManagementService trainManagementService) {
        this.trainManagementService = trainManagementService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TrainResponse>> createTrain(
            @Valid @RequestBody TrainRequest request) {
        TrainResponse train = trainManagementService.createTrain(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Train created successfully", train));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<TrainPageResponse>> getTrains(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "trainName") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) Boolean active) {
        TrainPageResponse trains = trainManagementService.getTrains(page, size, sortBy, direction, active);
        return ResponseEntity.ok(ApiResponse.success("Trains retrieved successfully", trains));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<TrainPageResponse>> searchTrains(
            @RequestParam("keyword") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "trainName") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        TrainPageResponse trains = trainManagementService.searchTrains(
                keyword, page, size, sortBy, direction);
        return ResponseEntity.ok(ApiResponse.success("Train search completed", trains));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TrainResponse>> getTrainById(@PathVariable Long id) {
        TrainResponse train = trainManagementService.getTrainById(id);
        return ResponseEntity.ok(ApiResponse.success("Train retrieved successfully", train));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TrainResponse>> updateTrain(
            @PathVariable Long id, @Valid @RequestBody TrainRequest request) {
        TrainResponse train = trainManagementService.updateTrain(id, request);
        return ResponseEntity.ok(ApiResponse.success("Train updated successfully", train));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<TrainResponse>> deactivateTrain(@PathVariable Long id) {
        TrainResponse train = trainManagementService.deactivateTrain(id);
        return ResponseEntity.ok(ApiResponse.success("Train deactivated successfully", train));
    }
}
