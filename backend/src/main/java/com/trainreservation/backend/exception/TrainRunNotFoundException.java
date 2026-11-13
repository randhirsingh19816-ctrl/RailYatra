package com.trainreservation.backend.exception;

public class TrainRunNotFoundException extends RuntimeException {

    public TrainRunNotFoundException(Long trainRunId) {
        super("Train run was not found: " + trainRunId);
    }
}
