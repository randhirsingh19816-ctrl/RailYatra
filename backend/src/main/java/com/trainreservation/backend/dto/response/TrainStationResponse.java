package com.trainreservation.backend.dto.response;

public record TrainStationResponse(
        Long id,
        String stationCode,
        String stationName
) {
}
