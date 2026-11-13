package com.trainreservation.backend.entity;

import com.trainreservation.backend.enums.TrainClassCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class TrainClassCodeConverter implements AttributeConverter<TrainClassCode, String> {

    @Override
    public String convertToDatabaseColumn(TrainClassCode attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public TrainClassCode convertToEntityAttribute(String value) {
        return value == null ? null : TrainClassCode.fromCode(value);
    }
}
