package com.trainreservation.backend.service.impl;

import com.trainreservation.backend.dto.request.AddTrainRouteStopRequest;
import com.trainreservation.backend.dto.request.TrainRouteRequest;
import com.trainreservation.backend.dto.request.TrainRouteStopRequest;
import com.trainreservation.backend.dto.response.ManagedTrainRouteStopResponse;
import com.trainreservation.backend.dto.response.TrainRouteResponse;
import com.trainreservation.backend.entity.Station;
import com.trainreservation.backend.entity.Train;
import com.trainreservation.backend.entity.TrainStop;
import com.trainreservation.backend.exception.InvalidRequestException;
import com.trainreservation.backend.exception.InvalidRouteException;
import com.trainreservation.backend.exception.StationNotFoundException;
import com.trainreservation.backend.exception.TrainNotFoundException;
import com.trainreservation.backend.exception.TrainRouteConflictException;
import com.trainreservation.backend.exception.TrainRouteNotFoundException;
import com.trainreservation.backend.exception.TrainRouteStopNotFoundException;
import com.trainreservation.backend.repository.StationRepository;
import com.trainreservation.backend.repository.TrainRepository;
import com.trainreservation.backend.repository.TrainRunRepository;
import com.trainreservation.backend.repository.TrainStopRepository;
import com.trainreservation.backend.service.TrainRouteManagementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class TrainRouteManagementServiceImpl implements TrainRouteManagementService {

    private static final long SECONDS_PER_DAY = 24 * 60 * 60;

    private final TrainRepository trainRepository;
    private final TrainStopRepository trainStopRepository;
    private final StationRepository stationRepository;
    private final TrainRunRepository trainRunRepository;

    public TrainRouteManagementServiceImpl(TrainRepository trainRepository,
                                           TrainStopRepository trainStopRepository,
                                           StationRepository stationRepository,
                                           TrainRunRepository trainRunRepository) {
        this.trainRepository = trainRepository;
        this.trainStopRepository = trainStopRepository;
        this.stationRepository = stationRepository;
        this.trainRunRepository = trainRunRepository;
    }

    @Override
    @Transactional
    public TrainRouteResponse createRoute(Long trainId, TrainRouteRequest request) {
        Train train = lockTrain(trainId);
        requireActiveTrain(train);
        if (trainStopRepository.existsByTrainId(trainId)) {
            throw new TrainRouteConflictException("A route already exists for this train; use PUT to replace it");
        }
        requireNoTrainRuns(trainId);

        List<TrainStop> stops = toStops(train, requireStops(request));
        validateRoute(stops);
        return toResponse(train, trainStopRepository.saveAllAndFlush(stops));
    }

    @Override
    @Transactional(readOnly = true)
    public TrainRouteResponse getRoute(Long trainId) {
        Train train = trainRepository.findById(trainId)
                .orElseThrow(() -> new TrainNotFoundException(trainId));
        return toResponse(train, requireExistingRoute(trainId));
    }

    @Override
    @Transactional
    public TrainRouteResponse updateRoute(Long trainId, TrainRouteRequest request) {
        Train train = lockTrain(trainId);
        List<TrainStop> existingStops = requireExistingRoute(trainId);
        requireNoTrainRuns(trainId);

        List<TrainStop> replacementStops = toStops(train, requireStops(request));
        validateRoute(replacementStops);

        trainStopRepository.deleteAll(existingStops);
        trainStopRepository.flush();
        return toResponse(train, trainStopRepository.saveAllAndFlush(replacementStops));
    }

    @Override
    @Transactional
    public TrainRouteResponse addStop(Long trainId, AddTrainRouteStopRequest request) {
        Train train = lockTrain(trainId);
        List<TrainStop> existingStops = requireExistingRoute(trainId);
        requireNoTrainRuns(trainId);
        if (request == null || request.stop() == null || request.previousStopDepartureTime() == null) {
            throw new InvalidRequestException("The new stop and previous stop departure time are required");
        }
        validateRoute(existingStops);

        List<TrainStop> stops = new ArrayList<>(existingStops);
        TrainStop previousDestination = stops.get(stops.size() - 1);
        TrainRouteStopRequest stopRequest = request.stop();
        if (!Integer.valueOf(stops.size() + 1).equals(stopRequest.stopNumber())) {
            throw new TrainRouteConflictException(
                    "A stop can only be appended at the next sequence number; existing stops are not shifted");
        }
        Station station = findActiveStation(stopRequest.stationCode());
        if (stops.stream().anyMatch(stop -> stop.getStation().getId().equals(station.getId()))) {
            throw new TrainRouteConflictException("A station can appear only once in a train route");
        }

        previousDestination.setDepartureTime(request.previousStopDepartureTime());
        stops.add(toStop(train, station, stopRequest));
        validateRoute(stops);

        trainStopRepository.save(previousDestination);
        trainStopRepository.saveAndFlush(stops.get(stops.size() - 1));
        return toResponse(train, stops);
    }

    @Override
    @Transactional
    public TrainRouteResponse updateStop(Long trainId, Long stopId, TrainRouteStopRequest request) {
        Train train = lockTrain(trainId);
        List<TrainStop> stops = requireExistingRoute(trainId);
        requireNoTrainRuns(trainId);
        TrainStop target = trainStopRepository.findByIdAndTrainId(stopId, trainId)
                .orElseThrow(() -> new TrainRouteStopNotFoundException(trainId, stopId));
        if (request == null) {
            throw new InvalidRequestException("Route stop details are required");
        }
        if (!target.getStopNumber().equals(request.stopNumber())) {
            throw new TrainRouteConflictException(
                    "Changing a stop sequence requires replacing the complete route");
        }

        Station station = findActiveStation(request.stationCode());
        target.setStation(station);
        applyStopFields(target, request);
        validateRoute(stops);
        trainStopRepository.saveAndFlush(target);
        return toResponse(train, stops);
    }

    @Override
    @Transactional
    public TrainRouteResponse removeStop(Long trainId, Long stopId) {
        Train train = lockTrain(trainId);
        List<TrainStop> stops = requireExistingRoute(trainId);
        requireNoTrainRuns(trainId);
        TrainStop target = trainStopRepository.findByIdAndTrainId(stopId, trainId)
                .orElseThrow(() -> new TrainRouteStopNotFoundException(trainId, stopId));
        if (stops.size() <= 2) {
            throw new TrainRouteConflictException("A train route must contain at least two stops");
        }

        Map<TrainStop, Integer> originalSequences = new java.util.IdentityHashMap<>();
        stops.forEach(stop -> originalSequences.put(stop, stop.getStopNumber()));
        stops.removeIf(stop -> stop.getId().equals(target.getId()));
        stops.sort(Comparator.comparing(TrainStop::getStopNumber));
        if (target.getStopNumber() == 1) {
            normalizeOrigin(stops);
        } else if (target.getStopNumber() == stops.size() + 1) {
            stops.get(stops.size() - 1).setDepartureTime(null);
        }
        for (int index = 0; index < stops.size(); index++) {
            stops.get(index).setStopNumber(index + 1);
        }
        validateRoute(stops);

        stops.forEach(stop -> stop.setStopNumber(originalSequences.get(stop)));
        trainStopRepository.delete(target);
        trainStopRepository.flush();
        for (int index = 0; index < stops.size(); index++) {
            TrainStop stop = stops.get(index);
            stop.setStopNumber(index + 1);
            trainStopRepository.saveAndFlush(stop);
        }
        return toResponse(train, stops);
    }

    private Train lockTrain(Long trainId) {
        return trainRepository.findByIdForRouteUpdate(trainId)
                .orElseThrow(() -> new TrainNotFoundException(trainId));
    }

    private void requireActiveTrain(Train train) {
        if (!Boolean.TRUE.equals(train.getActive())) {
            throw new TrainRouteConflictException("An inactive train cannot be assigned a route");
        }
    }

    private void requireNoTrainRuns(Long trainId) {
        if (trainRunRepository.existsByTrainId(trainId)) {
            throw new TrainRouteConflictException(
                    "The train route cannot be changed after train runs have been scheduled");
        }
    }

    private List<TrainStop> requireExistingRoute(Long trainId) {
        List<TrainStop> stops = trainStopRepository.findByTrainIdOrderByStopNumberAsc(trainId);
        if (stops.isEmpty()) {
            throw new TrainRouteNotFoundException(trainId);
        }
        return new ArrayList<>(stops);
    }

    private List<TrainRouteStopRequest> requireStops(TrainRouteRequest request) {
        if (request == null || request.stops() == null || request.stops().size() < 2) {
            throw new InvalidRouteException("A train route must contain at least two stops");
        }
        return request.stops();
    }

    private List<TrainStop> toStops(Train train, List<TrainRouteStopRequest> requests) {
        List<TrainStop> stops = new ArrayList<>(requests.size());
        for (TrainRouteStopRequest request : requests) {
            if (request == null) {
                throw new InvalidRouteException("Route stops cannot be null");
            }
            Station station = findActiveStation(request.stationCode());
            stops.add(toStop(train, station, request));
        }
        return stops;
    }

    private TrainStop toStop(Train train, Station station, TrainRouteStopRequest request) {
        TrainStop stop = new TrainStop();
        stop.setTrain(train);
        stop.setStation(station);
        applyStopFields(stop, request);
        return stop;
    }

    private void applyStopFields(TrainStop stop, TrainRouteStopRequest request) {
        if (request.stopNumber() == null || request.dayNumber() == null
                || request.distanceFromSource() == null) {
            throw new InvalidRouteException("Stop number, day number and distance are required");
        }
        stop.setStopNumber(request.stopNumber());
        stop.setArrivalTime(request.arrivalTime());
        stop.setDepartureTime(request.departureTime());
        stop.setDayNumber(request.dayNumber());
        stop.setDistanceFromSource(request.distanceFromSource());
    }

    private Station findActiveStation(String stationCode) {
        if (stationCode == null || stationCode.isBlank()) {
            throw new InvalidRouteException("Station code is required for every route stop");
        }
        String normalizedCode = stationCode.trim().toUpperCase(Locale.ROOT);
        Station station = stationRepository.findByStationCodeIgnoreCase(normalizedCode)
                .orElseThrow(() -> new StationNotFoundException(normalizedCode));
        if (!station.isActive()) {
            throw new TrainRouteConflictException("Inactive station cannot be used in a train route: "
                    + normalizedCode);
        }
        return station;
    }

    private void validateRoute(List<TrainStop> stops) {
        if (stops == null || stops.size() < 2) {
            throw new InvalidRouteException("A train route must contain at least two stops");
        }

        Set<Long> stationIds = new HashSet<>();
        BigDecimal previousDistance = null;
        Integer previousDay = null;
        Long previousDeparture = null;
        for (int index = 0; index < stops.size(); index++) {
            TrainStop stop = stops.get(index);
            int expectedSequence = index + 1;
            if (stop.getStopNumber() == null || stop.getStopNumber() != expectedSequence) {
                throw new InvalidRouteException("Route stop numbers must be consecutive and start at 1");
            }
            if (stop.getStation() == null || stop.getStation().getId() == null
                    || !stationIds.add(stop.getStation().getId())) {
                throw new TrainRouteConflictException("A station can appear only once in a train route");
            }
            if (stop.getDayNumber() == null || stop.getDayNumber() < 1) {
                throw new InvalidRouteException("Route day numbers must be positive");
            }
            if (previousDay != null && stop.getDayNumber() < previousDay) {
                throw new InvalidRouteException("Route day numbers must not decrease");
            }
            if (stop.getDistanceFromSource() == null || stop.getDistanceFromSource().signum() < 0) {
                throw new InvalidRouteException("Route distances must be non-negative");
            }
            if (index == 0 && stop.getDistanceFromSource().compareTo(BigDecimal.ZERO) != 0) {
                throw new InvalidRouteException("The origin distance must be zero");
            }
            if (previousDistance != null && stop.getDistanceFromSource().compareTo(previousDistance) <= 0) {
                throw new InvalidRouteException("Route distances must strictly increase");
            }

            boolean isOrigin = index == 0;
            boolean isDestination = index == stops.size() - 1;
            if (isOrigin && stop.getArrivalTime() != null) {
                throw new InvalidRouteException("The origin must not have an arrival time");
            }
            if (isDestination && stop.getDepartureTime() != null) {
                throw new InvalidRouteException("The destination must not have a departure time");
            }
            if (!isOrigin && stop.getArrivalTime() == null) {
                throw new InvalidRouteException("Every non-origin stop must have an arrival time");
            }
            if (!isDestination && stop.getDepartureTime() == null) {
                throw new InvalidRouteException("Every non-destination stop must have a departure time");
            }
            if (stop.getArrivalTime() != null && stop.getDepartureTime() != null
                    && stop.getArrivalTime().isAfter(stop.getDepartureTime())) {
                throw new InvalidRouteException("A stop arrival cannot be after its departure");
            }

            if (previousDeparture != null) {
                long arrival = toAbsoluteSeconds(stop.getDayNumber(), stop.getArrivalTime());
                if (arrival < previousDeparture) {
                    throw new InvalidRouteException("Route times must be chronological across operating days");
                }
            }
            previousDeparture = stop.getDepartureTime() == null ? null
                    : toAbsoluteSeconds(stop.getDayNumber(), stop.getDepartureTime());
            previousDistance = stop.getDistanceFromSource();
            previousDay = stop.getDayNumber();
        }
    }

    private long toAbsoluteSeconds(int dayNumber, LocalTime time) {
        return (dayNumber - 1L) * SECONDS_PER_DAY + time.toSecondOfDay();
    }

    private void normalizeOrigin(List<TrainStop> stops) {
        TrainStop newOrigin = stops.get(0);
        BigDecimal distanceOffset = newOrigin.getDistanceFromSource();
        int dayOffset = newOrigin.getDayNumber() - 1;
        newOrigin.setArrivalTime(null);
        for (TrainStop stop : stops) {
            stop.setDistanceFromSource(stop.getDistanceFromSource().subtract(distanceOffset));
            stop.setDayNumber(stop.getDayNumber() - dayOffset);
        }
        newOrigin.setDistanceFromSource(BigDecimal.ZERO);
    }

    private TrainRouteResponse toResponse(Train train, List<TrainStop> stops) {
        List<ManagedTrainRouteStopResponse> responses = stops.stream()
                .sorted(Comparator.comparing(TrainStop::getStopNumber))
                .map(stop -> new ManagedTrainRouteStopResponse(
                        stop.getId(),
                        stop.getStation().getId(),
                        stop.getStation().getStationCode(),
                        stop.getStation().getStationName(),
                        stop.getStopNumber(),
                        stop.getDayNumber(),
                        stop.getArrivalTime(),
                        stop.getDepartureTime(),
                        stop.getDistanceFromSource()))
                .toList();
        return new TrainRouteResponse(train.getId(), train.getTrainNumber(), responses);
    }
}
