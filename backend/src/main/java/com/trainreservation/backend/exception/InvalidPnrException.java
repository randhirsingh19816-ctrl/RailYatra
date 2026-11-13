package com.trainreservation.backend.exception;

public class InvalidPnrException extends InvalidRequestException {

    public InvalidPnrException() {
        super("PNR format is invalid");
    }
}
