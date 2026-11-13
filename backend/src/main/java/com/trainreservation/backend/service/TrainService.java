package com.trainreservation.backend.service;

import com.trainreservation.backend.dto.response.TrainDetailsResponse;
import com.trainreservation.backend.dto.response.TrainSearchResponse;
import com.trainreservation.backend.enums.TrainClassCode;

import java.time.LocalDate;
import java.util.List;

public interface TrainService {

    List<TrainSearchResponse> searchTrains(String source, String destination, LocalDate date, String classCode);

    TrainDetailsResponse getTrainDetails(Long trainId);
}
