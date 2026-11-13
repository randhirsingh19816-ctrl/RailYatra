package com.trainreservation.backend.dto.response;

public record StationResponse(
        Long id,
        String stationName,
        String stationCode,
        String city,
        String state,
        boolean active
) {
}
