package com.trainreservation.backend.service;

import com.trainreservation.backend.dto.request.CreateBookingRequest;
import com.trainreservation.backend.dto.response.BookingConfirmationResponse;
import com.trainreservation.backend.dto.response.BookingHistoryPageResponse;
import com.trainreservation.backend.dto.response.CancellationResponse;

public interface BookingService {

    BookingConfirmationResponse createBooking(CreateBookingRequest request);

    BookingHistoryPageResponse getMyBookings(int page, int size);

    BookingConfirmationResponse getBookingByPnr(String pnrNumber);

    CancellationResponse cancelBooking(String pnrNumber);
}
