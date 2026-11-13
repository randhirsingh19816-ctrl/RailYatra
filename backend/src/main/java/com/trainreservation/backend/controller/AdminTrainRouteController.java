package com.trainreservation.backend.controller;

import com.trainreservation.backend.dto.request.AddTrainRouteStopRequest;
import com.trainreservation.backend.dto.request.TrainRouteRequest;
import com.trainreservation.backend.dto.request.TrainRouteStopRequest;
import com.trainreservation.backend.dto.response.ApiResponse;
import com.trainreservation.backend.dto.response.TrainRouteResponse;
import com.trainreservation.backend.service.TrainRouteManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/trains/{trainId}/route")
public class AdminTrainRouteController {

    private final TrainRouteManagementService routeManagementService;

    public AdminTrainRouteController(TrainRouteManagementService routeManagementService) {
        this.routeManagementService = routeManagementService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TrainRouteResponse>> createRoute(
            @PathVariable Long trainId, @Valid @RequestBody TrainRouteRequest request) {
        TrainRouteResponse route = routeManagementService.createRoute(trainId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Train route created successfully", route));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<TrainRouteResponse>> getRoute(@PathVariable Long trainId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Train route retrieved successfully", routeManagementService.getRoute(trainId)));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<TrainRouteResponse>> updateRoute(
            @PathVariable Long trainId, @Valid @RequestBody TrainRouteRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Train route updated successfully", routeManagementService.updateRoute(trainId, request)));
    }

    @PostMapping("/stops")
    public ResponseEntity<ApiResponse<TrainRouteResponse>> addStop(
            @PathVariable Long trainId, @Valid @RequestBody AddTrainRouteStopRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Route stop added successfully",
                        routeManagementService.addStop(trainId, request)));
    }

    @PutMapping("/stops/{stopId}")
    public ResponseEntity<ApiResponse<TrainRouteResponse>> updateStop(
            @PathVariable Long trainId, @PathVariable Long stopId,
            @Valid @RequestBody TrainRouteStopRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Route stop updated successfully",
                routeManagementService.updateStop(trainId, stopId, request)));
    }

    @DeleteMapping("/stops/{stopId}")
    public ResponseEntity<ApiResponse<TrainRouteResponse>> removeStop(
            @PathVariable Long trainId, @PathVariable Long stopId) {
        return ResponseEntity.ok(ApiResponse.success("Route stop removed successfully",
                routeManagementService.removeStop(trainId, stopId)));
    }
}
