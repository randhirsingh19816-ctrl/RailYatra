package com.trainreservation.backend.exception;

public class TrainRouteStopNotFoundException extends RuntimeException {

    public TrainRouteStopNotFoundException(Long trainId, Long stopId) {
        super("Route stop " + stopId + " was not found for train: " + trainId);
    }
}
