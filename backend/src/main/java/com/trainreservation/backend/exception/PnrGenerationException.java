package com.trainreservation.backend.exception;

public class PnrGenerationException extends RuntimeException {

    public PnrGenerationException() {
        super("Unable to generate a unique PNR at this time");
    }
}
