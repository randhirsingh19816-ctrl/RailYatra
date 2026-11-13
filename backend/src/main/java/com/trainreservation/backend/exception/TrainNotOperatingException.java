package com.trainreservation.backend.exception;

public class TrainNotOperatingException extends RuntimeException {

    public TrainNotOperatingException(String message) {
        super(message);
    }
}
