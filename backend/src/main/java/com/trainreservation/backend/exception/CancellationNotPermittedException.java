package com.trainreservation.backend.exception;

public class CancellationNotPermittedException extends RuntimeException {

    public CancellationNotPermittedException(String message) {
        super(message);
    }
}
