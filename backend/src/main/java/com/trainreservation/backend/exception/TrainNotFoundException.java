package com.trainreservation.backend.exception;

public class TrainNotFoundException extends RuntimeException {

    public TrainNotFoundException(Long trainId) {
        super("Train was not found: " + trainId);
    }
}
