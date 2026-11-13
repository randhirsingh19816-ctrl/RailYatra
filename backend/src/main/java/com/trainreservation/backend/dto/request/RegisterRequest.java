package com.trainreservation.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Size(max = 120)
        String fullName,

        @NotBlank
        @Email
        @Size(max = 254)
        String email,

        @NotBlank
        @Pattern(regexp = "^[0-9+() -]{7,20}$")
        String phoneNumber,

        @NotBlank
        @Size(min = 8, max = 72)
        String password
) {
    @Override
    public String toString() {
        return "RegisterRequest[fullName=" + fullName + ", email=" + email
                + ", phoneNumber=" + phoneNumber + ", password=[REDACTED]]";
    }
}
