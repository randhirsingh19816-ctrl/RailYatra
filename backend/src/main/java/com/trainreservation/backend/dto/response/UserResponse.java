package com.trainreservation.backend.dto.response;

import com.trainreservation.backend.enums.UserRole;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String fullName,
        String email,
        String phoneNumber,
        UserRole role,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
