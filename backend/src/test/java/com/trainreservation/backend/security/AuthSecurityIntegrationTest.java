package com.trainreservation.backend.security;

import com.trainreservation.backend.controller.AuthController;
import com.trainreservation.backend.dto.response.AuthResponse;
import com.trainreservation.backend.dto.response.UserResponse;
import com.trainreservation.backend.entity.User;
import com.trainreservation.backend.enums.UserRole;
import com.trainreservation.backend.repository.UserRepository;
import com.trainreservation.backend.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, SecurityProbeController.class})
@Import({
        WebSecurityConfiguration.class,
        JwtAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class AuthSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void registrationEndpointReturnsCreatedUserWithoutPassword() throws Exception {
        when(authService.register(any())).thenReturn(userResponse());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Alice Example",
                                  "email": "alice@example.com",
                                  "phoneNumber": "5551234567",
                                  "password": "trainpass123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("alice@example.com"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void loginEndpointReturnsAccessTokenAndNoPassword() throws Exception {
        when(authService.login(any())).thenReturn(new AuthResponse(
                "Bearer", "signed.jwt.token", 3600, userResponse()));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "alice@example.com",
                                  "password": "trainpass123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("signed.jwt.token"))
                .andExpect(jsonPath("$.data.user.password").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "USER")
    void userCannotAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/security-probe"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/security-probe"))
                .andExpect(status().isOk());
    }

    @Test
    void bearerTokenAuthenticatesUserAndAppliesDatabaseRole() throws Exception {
        String email = "admin@example.com";
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("valid-test-token")
                .header("alg", "HS256")
                .issuer("https://trainreservation.test")
                .subject(email)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60))
                .build();
        User user = new User();
        user.setEmail(email);
        user.setPassword("unused-hash");
        user.setRole(UserRole.ADMIN);
        when(jwtService.validateToken("valid-test-token")).thenReturn(jwt);
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/admin/security-probe")
                        .header("Authorization", "Bearer valid-test-token"))
                .andExpect(status().isOk());
    }

    @Test
    void invalidBearerTokenIsRejected() throws Exception {
        when(jwtService.validateToken("invalid-token"))
                .thenThrow(new org.springframework.security.oauth2.jwt.BadJwtException("invalid"));

        mockMvc.perform(get("/api/admin/security-probe")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedClientCannotAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/security-probe"))
                .andExpect(status().isUnauthorized());
    }

    private UserResponse userResponse() {
        LocalDateTime created = LocalDateTime.of(2026, 1, 1, 0, 0);
        return new UserResponse(1L, "Alice Example", "alice@example.com", "5551234567",
                UserRole.USER, created, created);
    }
}
