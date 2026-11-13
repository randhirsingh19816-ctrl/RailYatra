package com.trainreservation.backend.dto.response;

import java.util.List;

public record TrainRouteResponse(
        Long trainId,
        String trainNumber,
        List<ManagedTrainRouteStopResponse> stops
) {
}
