package com.trainreservation.backend.dto.response;

import java.util.List;

public record StationPageResponse(
        List<StationResponse> stations,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
