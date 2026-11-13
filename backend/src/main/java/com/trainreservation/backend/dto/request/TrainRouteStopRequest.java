package com.trainreservation.backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalTime;

public record TrainRouteStopRequest(
        @NotBlank @Size(max = 10) @Pattern(regexp = "^[A-Za-z0-9]{2,10}$") String stationCode,
        @NotNull @Positive Integer stopNumber,
        LocalTime arrivalTime,
        LocalTime departureTime,
        @NotNull @Positive Integer dayNumber,
        @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal distanceFromSource
) {
}
