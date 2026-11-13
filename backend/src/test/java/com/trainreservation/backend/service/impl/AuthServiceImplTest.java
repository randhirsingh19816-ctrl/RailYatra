package com.trainreservation.backend.service.impl;

import com.trainreservation.backend.dto.request.LoginRequest;
import com.trainreservation.backend.dto.request.RegisterRequest;
import com.trainreservation.backend.dto.response.AuthResponse;
import com.trainreservation.backend.dto.response.UserResponse;
import com.trainreservation.backend.entity.User;
import com.trainreservation.backend.enums.UserRole;
import com.trainreservation.backend.exception.DuplicateEmailException;
import com.trainreservation.backend.exception.InvalidCredentialsException;
import com.trainreservation.backend.repository.UserRepository;
import com.trainreservation.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceImplTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AuthenticationManager authenticationManager;
    private JwtService jwtService;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        authenticationManager = mock(AuthenticationManager.class);
        jwtService = mock(JwtService.class);
        authService = new AuthServiceImpl(userRepository, passwordEncoder, authenticationManager, jwtService);
    }

    @Test
    void registrationStoresBcryptPasswordAndAlwaysAssignsUserRole() {
        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(7L);
            return saved;
        });

        UserResponse response = authService.register(new RegisterRequest(
                "Alice Example", " Alice@Example.com ", "+1 555 123 4567", "trainpass123"));

        org.mockito.ArgumentCaptor<User> userCaptor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals(7L, response.id());
        assertEquals("alice@example.com", response.email());
        assertEquals(UserRole.USER, response.role());
        assertFalse(saved.getPassword().equals("trainpass123"));
        assertTrue(passwordEncoder.matches("trainpass123", saved.getPassword()));
        assertFalse(List.of(UserResponse.class.getRecordComponents()).stream()
                .anyMatch(component -> component.getName().equalsIgnoreCase("password")));
    }

    @Test
    void registrationRejectsAnExistingEmail() {
        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> authService.register(new RegisterRequest(
                "Alice Example", "alice@example.com", "5551234567", "trainpass123")));
    }

    @Test
    void loginAuthenticatesAndReturnsTokenWithoutPassword() {
        String email = "alice@example.com";
        User user = new User();
        user.setId(7L);
        user.setFullName("Alice Example");
        user.setEmail(email);
        user.setPhoneNumber("5551234567");
        user.setRole(UserRole.USER);
        when(authenticationManager.authenticate(any())).thenReturn(
                UsernamePasswordAuthenticationToken.authenticated(email, null, List.of()));
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(jwtService.generateToken(email)).thenReturn("signed.jwt.token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        AuthResponse response = authService.login(new LoginRequest(email, "trainpass123"));

        assertEquals("Bearer", response.tokenType());
        assertEquals("signed.jwt.token", response.accessToken());
        assertEquals(email, response.user().email());
        assertFalse(List.of(UserResponse.class.getRecordComponents()).stream()
                .anyMatch(component -> component.getName().equalsIgnoreCase("password")));
    }

    @Test
    void loginReturnsGenericErrorForInvalidCredentials() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad credentials"));

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("alice@example.com", "wrongpass")));
    }
}
