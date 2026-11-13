package com.trainreservation.backend.dto.response;

import com.trainreservation.backend.enums.TrainType;

public record TrainResponse(
        Long id,
        String trainNumber,
        String trainName,
        TrainType trainType,
        boolean active
) {
}
