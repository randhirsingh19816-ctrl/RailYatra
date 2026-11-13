package com.trainreservation.backend.entity;

import com.trainreservation.backend.enums.TrainClassCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TrainClassCodeConverterTest {

    private final TrainClassCodeConverter converter = new TrainClassCodeConverter();

    @Test
    void persistsNumericTrainClassCodesWithoutChangingTheirPublicValues() {
        assertEquals("3A", converter.convertToDatabaseColumn(TrainClassCode.THREE_A));
        assertEquals(TrainClassCode.THREE_A, converter.convertToEntityAttribute("3A"));
    }

    @Test
    void preservesNullClassCodes() {
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
    }
}
