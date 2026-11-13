package com.trainreservation.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record AddTrainRouteStopRequest(
        @NotNull @Valid TrainRouteStopRequest stop,
        @NotNull LocalTime previousStopDepartureTime
) {
}
