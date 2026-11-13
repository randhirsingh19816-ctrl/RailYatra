package com.trainreservation.backend.dto.response;

import com.trainreservation.backend.enums.BookingStatus;
import com.trainreservation.backend.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record BookingConfirmationResponse(
        String pnrNumber,
        BookingStatus bookingStatus,
        PaymentStatus paymentStatus,
        Long trainId,
        String trainNumber,
        String trainName,
        LocalDate journeyDate,
        TrainStationResponse sourceStation,
        TrainStationResponse destinationStation,
        String classCode,
        List<BookingPassengerResponse> passengers,
        BigDecimal farePerPassenger,
        BigDecimal totalFare,
        String currency,
        LocalDateTime bookingDate
) {
}
