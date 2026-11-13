package com.trainreservation.backend.exception;

public class StationNotFoundException extends RuntimeException {

    public StationNotFoundException(String identifier) {
        super("Station was not found: " + identifier);
    }
}
