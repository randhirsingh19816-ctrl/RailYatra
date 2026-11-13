package com.trainreservation.backend.dto.response;

import java.util.List;

public record BookingHistoryPageResponse(
        List<BookingHistoryItemResponse> bookings,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
