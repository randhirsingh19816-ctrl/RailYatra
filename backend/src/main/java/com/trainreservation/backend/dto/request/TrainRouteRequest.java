package com.trainreservation.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TrainRouteRequest(
        @NotNull @Size(min = 2) List<@Valid TrainRouteStopRequest> stops
) {
}
