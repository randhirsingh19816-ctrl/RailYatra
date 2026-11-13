package com.trainreservation.backend.exception;

public class BookingAlreadyCancelledException extends RuntimeException {

    public BookingAlreadyCancelledException() {
        super("Booking has already been cancelled");
    }
}
