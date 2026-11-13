package com.trainreservation.backend.dto.response;

import com.trainreservation.backend.enums.Gender;

public record BookingPassengerResponse(
        String fullName,
        Integer age,
        Gender gender,
        String coachNumber,
        String seatNumber
) {
}
