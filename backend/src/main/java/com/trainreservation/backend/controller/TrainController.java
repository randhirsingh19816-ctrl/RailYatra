package com.trainreservation.backend.controller;

import com.trainreservation.backend.dto.response.ApiResponse;
import com.trainreservation.backend.dto.response.TrainAvailabilityResponse;
import com.trainreservation.backend.dto.response.TrainDetailsResponse;
import com.trainreservation.backend.dto.response.TrainSearchResponse;
import com.trainreservation.backend.service.TrainAvailabilityService;
import com.trainreservation.backend.service.TrainService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/trains")
public class TrainController {

    private final TrainService trainService;
    private final TrainAvailabilityService trainAvailabilityService;

    public TrainController(TrainService trainService, TrainAvailabilityService trainAvailabilityService) {
        this.trainService = trainService;
        this.trainAvailabilityService = trainAvailabilityService;
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<TrainSearchResponse>>> searchTrains(
            @RequestParam("source") String source,
            @RequestParam("destination") String destination,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "classCode", required = false) String classCode) {
        List<TrainSearchResponse> trains = trainService.searchTrains(source, destination, date, classCode);
        return ResponseEntity.ok(ApiResponse.success("Trains retrieved successfully", trains));
    }

    @GetMapping("/{trainId}")
    public ResponseEntity<ApiResponse<TrainDetailsResponse>> getTrainDetails(@PathVariable Long trainId) {
        TrainDetailsResponse train = trainService.getTrainDetails(trainId);
        return ResponseEntity.ok(ApiResponse.success("Train retrieved successfully", train));
    }

    @GetMapping("/{trainId}/availability")
    public ResponseEntity<ApiResponse<TrainAvailabilityResponse>> getAvailability(
            @PathVariable Long trainId,
            @RequestParam("source") String source,
            @RequestParam("destination") String destination,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam("classCode") String classCode) {
        TrainAvailabilityResponse availability = trainAvailabilityService.getAvailability(
                trainId, source, destination, date, classCode);
        return ResponseEntity.ok(ApiResponse.success("Availability retrieved successfully", availability));
    }
}
