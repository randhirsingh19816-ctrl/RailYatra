package com.trainreservation.backend.dto.response;

import com.trainreservation.backend.enums.BookingStatus;
import com.trainreservation.backend.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BookingHistoryItemResponse(
        String pnrNumber,
        Long trainId,
        String trainNumber,
        String trainName,
        LocalDate journeyDate,
        TrainStationResponse sourceStation,
        TrainStationResponse destinationStation,
        long passengerCount,
        BigDecimal totalFare,
        BookingStatus bookingStatus,
        PaymentStatus paymentStatus
) {
}
