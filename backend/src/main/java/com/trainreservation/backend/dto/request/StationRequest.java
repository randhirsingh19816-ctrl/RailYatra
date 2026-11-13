package com.trainreservation.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record StationRequest(
        @NotBlank @Size(max = 120) String stationName,
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9]{2,10}$") String stationCode,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 100) String state
) {
}
