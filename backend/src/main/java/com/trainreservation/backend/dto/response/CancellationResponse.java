package com.trainreservation.backend.dto.response;

import com.trainreservation.backend.enums.BookingStatus;
import com.trainreservation.backend.enums.CancellationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CancellationResponse(
        String pnrNumber,
        BookingStatus bookingStatus,
        BigDecimal refundAmount,
        CancellationStatus refundStatus,
        LocalDateTime cancellationDate
) {
}
