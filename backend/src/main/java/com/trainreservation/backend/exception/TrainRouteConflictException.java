package com.trainreservation.backend.exception;

public class TrainRouteConflictException extends RuntimeException {

    public TrainRouteConflictException(String message) {
        super(message);
    }
}
