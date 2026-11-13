package com.trainreservation.backend.dto.response;

import java.util.List;

public record TrainPageResponse(
        List<TrainResponse> trains,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
