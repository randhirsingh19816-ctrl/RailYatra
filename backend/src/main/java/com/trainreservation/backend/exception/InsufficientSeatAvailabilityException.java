package com.trainreservation.backend.exception;

public class InsufficientSeatAvailabilityException extends RuntimeException {

    public InsufficientSeatAvailabilityException(int requested, long available) {
        super("Insufficient seats available: requested " + requested + ", available " + available);
    }
}
