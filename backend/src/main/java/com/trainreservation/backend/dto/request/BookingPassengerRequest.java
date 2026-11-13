package com.trainreservation.backend.dto.request;

import com.trainreservation.backend.enums.Gender;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BookingPassengerRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotNull @Min(1) @Max(120) Integer age,
        @NotNull Gender gender
) {
}
