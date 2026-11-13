package com.trainreservation.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TrainAvailabilityResponse(
        Long trainId,
        Long trainRunId,
        String trainNumber,
        String trainName,
        TrainStationResponse sourceStation,
        TrainStationResponse destinationStation,
        LocalDate journeyDate,
        int sourceStopNumber,
        int destinationStopNumber,
        String classCode,
        BigDecimal distance,
        long totalSeats,
        long availableSeats,
        BigDecimal farePerPassenger,
        String currency
) {
}
