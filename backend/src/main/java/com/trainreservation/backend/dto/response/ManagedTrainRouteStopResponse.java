package com.trainreservation.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalTime;

public record ManagedTrainRouteStopResponse(
        Long id,
        Long stationId,
        String stationCode,
        String stationName,
        int stopNumber,
        int dayNumber,
        LocalTime arrivalTime,
        LocalTime departureTime,
        BigDecimal distanceFromSource
) {
}
