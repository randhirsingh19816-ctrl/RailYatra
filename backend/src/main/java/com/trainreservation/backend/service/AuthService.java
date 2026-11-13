package com.trainreservation.backend.service;

import com.trainreservation.backend.dto.request.LoginRequest;
import com.trainreservation.backend.dto.request.RegisterRequest;
import com.trainreservation.backend.dto.response.AuthResponse;
import com.trainreservation.backend.dto.response.UserResponse;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
