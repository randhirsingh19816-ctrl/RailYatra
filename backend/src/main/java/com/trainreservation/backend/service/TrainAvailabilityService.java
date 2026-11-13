package com.trainreservation.backend.service;

import com.trainreservation.backend.dto.response.TrainAvailabilityResponse;

import java.time.LocalDate;

public interface TrainAvailabilityService {

    TrainAvailabilityResponse getAvailability(Long trainId, String source, String destination,
                                               LocalDate journeyDate, String classCode);
}
