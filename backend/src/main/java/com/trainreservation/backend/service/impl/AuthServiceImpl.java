package com.trainreservation.backend.service.impl;

import com.trainreservation.backend.dto.request.LoginRequest;
import com.trainreservation.backend.dto.request.RegisterRequest;
import com.trainreservation.backend.dto.response.AuthResponse;
import com.trainreservation.backend.dto.response.UserResponse;
import com.trainreservation.backend.entity.User;
import com.trainreservation.backend.enums.UserRole;
import com.trainreservation.backend.exception.DuplicateEmailException;
import com.trainreservation.backend.exception.InvalidCredentialsException;
import com.trainreservation.backend.exception.InvalidRequestException;
import com.trainreservation.backend.repository.UserRepository;
import com.trainreservation.backend.security.JwtService;
import com.trainreservation.backend.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new InvalidRequestException("Password must not exceed 72 UTF-8 bytes");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException();
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPhoneNumber(request.phoneNumber().trim());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.USER);
        return toUserResponse(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new InvalidCredentialsException();
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email, request.password()));
        } catch (AuthenticationException exception) {
            throw new InvalidCredentialsException();
        }
        if (!authentication.isAuthenticated()) {
            throw new InvalidCredentialsException();
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(InvalidCredentialsException::new);
        String token = jwtService.generateToken(authentication.getName());
        return new AuthResponse(
                "Bearer",
                token,
                jwtService.getExpirationSeconds(),
                toUserResponse(user));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getPhoneNumber(),
                user.getRole(), user.getCreatedAt(), user.getUpdatedAt());
    }
}
