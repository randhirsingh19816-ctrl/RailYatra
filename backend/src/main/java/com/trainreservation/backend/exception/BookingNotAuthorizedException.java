package com.trainreservation.backend.exception;

public class BookingNotAuthorizedException extends RuntimeException {

    public BookingNotAuthorizedException() {
        super("Only authenticated USER accounts may create bookings");
    }
}
