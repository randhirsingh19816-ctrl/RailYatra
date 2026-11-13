package com.trainreservation.backend.service;

import com.trainreservation.backend.dto.request.TrainRequest;
import com.trainreservation.backend.dto.response.TrainPageResponse;
import com.trainreservation.backend.dto.response.TrainResponse;

public interface TrainManagementService {

    TrainResponse createTrain(TrainRequest request);

    TrainPageResponse getTrains(int page, int size, String sortBy, String direction, Boolean active);

    TrainResponse getTrainById(Long id);

    TrainResponse updateTrain(Long id, TrainRequest request);

    TrainResponse deactivateTrain(Long id);

    TrainPageResponse searchTrains(String keyword, int page, int size, String sortBy, String direction);
}
