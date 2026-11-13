package com.trainreservation.backend.dto.response;

public record AuthResponse(
        String tokenType,
        String accessToken,
        long expiresInSeconds,
        UserResponse user
) {
    @Override
    public String toString() {
        return "AuthResponse[tokenType=" + tokenType + ", accessToken=[REDACTED]"
                + ", expiresInSeconds=" + expiresInSeconds + ", user=" + user + "]";
    }
}
