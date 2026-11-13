package com.trainreservation.backend.dto.response;

import com.trainreservation.backend.enums.TrainType;

import java.util.List;

public record TrainDetailsResponse(
        Long id,
        String trainNumber,
        String trainName,
        TrainType trainType,
        boolean active,
        List<TrainRouteStopResponse> routeStops
) {
}
