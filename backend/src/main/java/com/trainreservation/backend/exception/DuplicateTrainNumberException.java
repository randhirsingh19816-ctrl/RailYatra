package com.trainreservation.backend.exception;

public class DuplicateTrainNumberException extends RuntimeException {

    public DuplicateTrainNumberException(String trainNumber) {
        super("Train number already exists: " + trainNumber);
    }
}
