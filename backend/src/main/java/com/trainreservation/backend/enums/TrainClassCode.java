package com.trainreservation.backend.enums;

public enum TrainClassCode {
    SL("SL"),
    THREE_A("3A"),
    TWO_A("2A"),
    ONE_A("1A"),
    CC("CC"),
    EC("EC");

    private final String code;

    TrainClassCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static TrainClassCode fromCode(String code) {
        for (TrainClassCode classCode : values()) {
            if (classCode.code.equals(code)) {
                return classCode;
            }
        }
        throw new IllegalArgumentException("Unsupported train class code: " + code);
    }
}
