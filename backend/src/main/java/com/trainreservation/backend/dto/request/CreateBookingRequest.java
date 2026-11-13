package com.trainreservation.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CreateBookingRequest(
        @NotNull Long trainRunId,
        @NotBlank @Size(max = 32) String sourceStation,
        @NotBlank @Size(max = 32) String destinationStation,
        @NotNull LocalDate journeyDate,
        @NotBlank @Size(max = 20) String classCode,
        @NotEmpty List<@NotNull @Valid BookingPassengerRequest> passengers
) {
}
