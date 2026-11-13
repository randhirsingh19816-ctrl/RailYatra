package com.trainreservation.backend.exception;

public class BookingAccessDeniedException extends RuntimeException {

    public BookingAccessDeniedException() {
        super("You are not permitted to access this booking");
    }
}
