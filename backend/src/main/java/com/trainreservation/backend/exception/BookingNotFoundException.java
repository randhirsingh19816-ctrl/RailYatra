package com.trainreservation.backend.exception;

public class BookingNotFoundException extends RuntimeException {

    public BookingNotFoundException() {
        super("Booking was not found");
    }
}
