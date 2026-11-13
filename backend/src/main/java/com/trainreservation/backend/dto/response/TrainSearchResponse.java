package com.trainreservation.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TrainSearchResponse(
        Long trainId,
        Long trainRunId,
        String trainNumber,
        String trainName,
        LocalDate journeyDate,
        TrainStationResponse sourceStation,
        TrainStationResponse destinationStation,
        LocalDateTime departureTime,
        LocalDateTime arrivalTime,
        long durationMinutes,
        BigDecimal distance,
        List<TrainClassResponse> availableClasses
) {
}
