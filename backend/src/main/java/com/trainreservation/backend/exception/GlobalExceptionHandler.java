package com.trainreservation.backend.exception;

import com.trainreservation.backend.dto.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateEmail(DuplicateEmailException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(exception.getMessage(), null));
    }

    @ExceptionHandler(DuplicateStationCodeException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateStationCode(
            DuplicateStationCodeException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(exception.getMessage(), null));
    }

    @ExceptionHandler(DuplicateTrainNumberException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateTrainNumber(
            DuplicateTrainNumberException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(exception.getMessage(), null));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentials(InvalidCredentialsException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.failure(exception.getMessage(), null));
    }

    @ExceptionHandler(BookingNotAuthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleBookingNotAuthorized(BookingNotAuthorizedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.failure(exception.getMessage(), null));
    }

    @ExceptionHandler(BookingAccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleBookingAccessDenied(BookingAccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.failure(exception.getMessage(), null));
    }

    @ExceptionHandler(BookingNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleBookingNotFound(BookingNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(exception.getMessage(), null));
    }

    @ExceptionHandler(BookingAlreadyCancelledException.class)
    public ResponseEntity<ApiResponse<Void>> handleAlreadyCancelled(
            BookingAlreadyCancelledException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(exception.getMessage(), null));
    }

    @ExceptionHandler(CancellationNotPermittedException.class)
    public ResponseEntity<ApiResponse<Void>> handleCancellationNotPermitted(
            CancellationNotPermittedException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(exception.getMessage(), null));
    }

    @ExceptionHandler(InsufficientSeatAvailabilityException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientSeatAvailability(
            InsufficientSeatAvailabilityException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(exception.getMessage(), null));
    }

    @ExceptionHandler(TrainRunNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleTrainRunNotFound(TrainRunNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(exception.getMessage(), null));
    }

    @ExceptionHandler(PnrGenerationException.class)
    public ResponseEntity<ApiResponse<Void>> handlePnrGenerationFailure(PnrGenerationException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.failure(exception.getMessage(), null));
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidRequest(InvalidRequestException exception) {
        return ResponseEntity.badRequest().body(ApiResponse.failure(exception.getMessage(), null));
    }

    @ExceptionHandler({TrainNotFoundException.class, StationNotFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(exception.getMessage(), null));
    }

    @ExceptionHandler(TrainNotOperatingException.class)
    public ResponseEntity<ApiResponse<Void>> handleTrainNotOperating(TrainNotOperatingException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(exception.getMessage(), null));
    }

    @ExceptionHandler(TrainRouteConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleTrainRouteConflict(TrainRouteConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(exception.getMessage(), null));
    }

    @ExceptionHandler({TrainRouteNotFoundException.class, TrainRouteStopNotFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleTrainRouteNotFound(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(exception.getMessage(), null));
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ApiResponse<Void>> handleInvalidParameter() {
        return ResponseEntity.badRequest().body(ApiResponse.failure("Request parameters are invalid", null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(ApiResponse.failure("Request validation failed", errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation() {
        return ResponseEntity.badRequest().body(ApiResponse.failure("Request validation failed", null));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableRequest() {
        return ResponseEntity.badRequest().body(ApiResponse.failure("Request body is invalid", null));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataConflict() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure("The request conflicts with existing data", null));
    }
}
