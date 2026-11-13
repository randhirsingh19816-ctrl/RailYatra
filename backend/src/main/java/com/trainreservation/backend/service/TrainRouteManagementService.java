package com.trainreservation.backend.service;

import com.trainreservation.backend.dto.request.AddTrainRouteStopRequest;
import com.trainreservation.backend.dto.request.TrainRouteRequest;
import com.trainreservation.backend.dto.request.TrainRouteStopRequest;
import com.trainreservation.backend.dto.response.TrainRouteResponse;

public interface TrainRouteManagementService {

    TrainRouteResponse createRoute(Long trainId, TrainRouteRequest request);

    TrainRouteResponse getRoute(Long trainId);

    TrainRouteResponse updateRoute(Long trainId, TrainRouteRequest request);

    TrainRouteResponse addStop(Long trainId, AddTrainRouteStopRequest request);

    TrainRouteResponse updateStop(Long trainId, Long stopId, TrainRouteStopRequest request);

    TrainRouteResponse removeStop(Long trainId, Long stopId);
}
