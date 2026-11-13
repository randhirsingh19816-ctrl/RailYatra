package com.trainreservation.backend.controller;

import com.trainreservation.backend.dto.request.CreateBookingRequest;
import com.trainreservation.backend.dto.response.ApiResponse;
import com.trainreservation.backend.dto.response.BookingConfirmationResponse;
import com.trainreservation.backend.dto.response.BookingHistoryPageResponse;
import com.trainreservation.backend.dto.response.CancellationResponse;
import com.trainreservation.backend.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BookingConfirmationResponse>> createBooking(
            @Valid @RequestBody CreateBookingRequest request) {
        BookingConfirmationResponse booking = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking created; payment is pending", booking));
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<ApiResponse<BookingHistoryPageResponse>> getMyBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        BookingHistoryPageResponse bookings = bookingService.getMyBookings(page, size);
        return ResponseEntity.ok(ApiResponse.success("Booking history retrieved successfully", bookings));
    }

    @GetMapping("/{pnr}")
    public ResponseEntity<ApiResponse<BookingConfirmationResponse>> getBookingByPnr(
            @PathVariable String pnr) {
        BookingConfirmationResponse booking = bookingService.getBookingByPnr(pnr);
        return ResponseEntity.ok(ApiResponse.success("Booking retrieved successfully", booking));
    }

    @PostMapping("/{pnr}/cancel")
    public ResponseEntity<ApiResponse<CancellationResponse>> cancelBooking(@PathVariable String pnr) {
        CancellationResponse cancellation = bookingService.cancelBooking(pnr);
        return ResponseEntity.ok(ApiResponse.success(
                "Booking cancelled; refund request is pending and no refund has been processed",
                cancellation));
    }
}
