package com.trainreservation.backend.dto.request;

import com.trainreservation.backend.enums.TrainType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TrainRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9-]{1,20}$") String trainNumber,
        @NotBlank @Size(max = 120) String trainName,
        @NotNull TrainType trainType
) {
}
