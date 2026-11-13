package com.trainreservation.backend.exception;

public class TrainRouteNotFoundException extends RuntimeException {

    public TrainRouteNotFoundException(Long trainId) {
        super("Route was not found for train: " + trainId);
    }
}
