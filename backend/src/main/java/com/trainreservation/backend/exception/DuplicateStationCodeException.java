package com.trainreservation.backend.exception;

public class DuplicateStationCodeException extends RuntimeException {

    public DuplicateStationCodeException(String stationCode) {
        super("Station code already exists: " + stationCode);
    }
}
