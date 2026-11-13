package com.trainreservation.backend.service.impl;

import com.trainreservation.backend.dto.response.TrainClassResponse;
import com.trainreservation.backend.dto.response.TrainDetailsResponse;
import com.trainreservation.backend.dto.response.TrainRouteStopResponse;
import com.trainreservation.backend.dto.response.TrainSearchResponse;
import com.trainreservation.backend.dto.response.TrainStationResponse;
import com.trainreservation.backend.entity.Station;
import com.trainreservation.backend.entity.Train;
import com.trainreservation.backend.entity.TrainClass;
import com.trainreservation.backend.entity.TrainRun;
import com.trainreservation.backend.entity.TrainStop;
import com.trainreservation.backend.enums.RunningStatus;
import com.trainreservation.backend.enums.TrainClassCode;
import com.trainreservation.backend.exception.InvalidRequestException;
import com.trainreservation.backend.exception.InvalidRouteException;
import com.trainreservation.backend.exception.TrainNotFoundException;
import com.trainreservation.backend.repository.TrainClassRepository;
import com.trainreservation.backend.repository.TrainRepository;
import com.trainreservation.backend.repository.TrainRunRepository;
import com.trainreservation.backend.repository.TrainStopRepository;
import com.trainreservation.backend.service.TrainService;
import com.trainreservation.backend.service.TrainRouteLookupService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class TrainServiceImpl implements TrainService {

    private static final List<RunningStatus> SEARCHABLE_RUN_STATUSES =
            List.of(RunningStatus.SCHEDULED, RunningStatus.RUNNING);

    private final TrainRouteLookupService trainRouteLookupService;
    private final TrainRepository trainRepository;
    private final TrainRunRepository trainRunRepository;
    private final TrainStopRepository trainStopRepository;
    private final TrainClassRepository trainClassRepository;

    public TrainServiceImpl(TrainRouteLookupService trainRouteLookupService,
                            TrainRepository trainRepository,
                            TrainRunRepository trainRunRepository,
                            TrainStopRepository trainStopRepository,
                            TrainClassRepository trainClassRepository) {
        this.trainRouteLookupService = trainRouteLookupService;
        this.trainRepository = trainRepository;
        this.trainRunRepository = trainRunRepository;
        this.trainStopRepository = trainStopRepository;
        this.trainClassRepository = trainClassRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainSearchResponse> searchTrains(String sourceIdentifier, String destinationIdentifier,
                                                   LocalDate date, String requestedClassCode) {
        if (date == null) {
            throw new InvalidRequestException("Journey date is required");
        }
        if (date.isBefore(LocalDate.now())) {
            throw new InvalidRequestException("Journey date cannot be in the past");
        }
        Station sourceStation = trainRouteLookupService.findStation(sourceIdentifier);
        Station destinationStation = trainRouteLookupService.findStation(destinationIdentifier);
        if (sourceStation.getId().equals(destinationStation.getId())) {
            throw new InvalidRouteException("Source and destination stations must be different");
        }

        TrainClassCode requestedClass = parseClassCode(requestedClassCode);
        List<TrainRun> runs = trainRunRepository.findByJourneyDateAndRunningStatusInOrderByTrainIdAsc(
                date, SEARCHABLE_RUN_STATUSES);
        Set<Long> includedTrainIds = new HashSet<>();
        List<TrainSearchResponse> results = new ArrayList<>();

        for (TrainRun run : runs) {
            Train train = run.getTrain();
            if (!Boolean.TRUE.equals(train.getActive()) || includedTrainIds.contains(train.getId())) {
                continue;
            }

            List<TrainStop> stops = trainStopRepository.findByTrainIdOrderByStopNumberAsc(train.getId());
            Optional<TrainRouteLookupService.RouteSegment> routeSegment =
                    trainRouteLookupService.findOrderedSegment(
                    stops, sourceStation.getId(), destinationStation.getId());
            if (routeSegment.isEmpty()) {
                continue;
            }

            TrainRouteLookupService.RouteSegment segment = routeSegment.get();
            if (segment.source().getDepartureTime() == null || segment.destination().getArrivalTime() == null) {
                continue;
            }
            BigDecimal distance = segment.destination().getDistanceFromSource()
                    .subtract(segment.source().getDistanceFromSource());
            if (distance.signum() < 0) {
                continue;
            }

            List<TrainClass> trainClasses = trainClassRepository.findByTrainId(train.getId());
            List<TrainClass> matchingClasses = trainClasses.stream()
                    .filter(trainClass -> requestedClass == null
                            || trainClass.getClassCode() == requestedClass)
                    .toList();
            if (matchingClasses.isEmpty()) {
                continue;
            }

            LocalDateTime departureTime = run.getJourneyDate()
                    .plusDays(segment.source().getDayNumber() - 1L)
                    .atTime(segment.source().getDepartureTime());
            if (!departureTime.isAfter(LocalDateTime.now())) {
                continue;
            }
            LocalDateTime arrivalTime = run.getJourneyDate()
                    .plusDays(segment.destination().getDayNumber() - 1L)
                    .atTime(segment.destination().getArrivalTime());
            Duration duration = Duration.between(departureTime, arrivalTime);
            if (duration.isNegative()) {
                continue;
            }

            includedTrainIds.add(train.getId());
            results.add(new TrainSearchResponse(
                    train.getId(),
                    run.getId(),
                    train.getTrainNumber(),
                    train.getTrainName(),
                    run.getJourneyDate(),
                    toStationResponse(segment.source().getStation()),
                    toStationResponse(segment.destination().getStation()),
                    departureTime,
                    arrivalTime,
                    duration.toMinutes(),
                    distance,
                    toTrainClassResponses(matchingClasses)));
        }
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public TrainDetailsResponse getTrainDetails(Long trainId) {
        Train train = trainRepository.findById(trainId)
                .orElseThrow(() -> new TrainNotFoundException(trainId));
        List<TrainRouteStopResponse> routeStops = trainStopRepository
                .findByTrainIdOrderByStopNumberAsc(trainId)
                .stream()
                .sorted(Comparator.comparing(TrainStop::getStopNumber))
                .map(this::toRouteStopResponse)
                .toList();

        return new TrainDetailsResponse(
                train.getId(),
                train.getTrainNumber(),
                train.getTrainName(),
                train.getTrainType(),
                Boolean.TRUE.equals(train.getActive()),
                routeStops);
    }

    private TrainClassCode parseClassCode(String classCode) {
        if (classCode == null || classCode.isBlank()) {
            return null;
        }
        try {
            return TrainClassCode.fromCode(classCode.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException("Unsupported train class code");
        }
    }

    private TrainStationResponse toStationResponse(Station station) {
        return new TrainStationResponse(station.getId(), station.getStationCode(), station.getStationName());
    }

    private List<TrainClassResponse> toTrainClassResponses(List<TrainClass> trainClasses) {
        return trainClasses.stream()
                .sorted(Comparator.comparing(trainClass -> trainClass.getClassCode().getCode()))
                .map(trainClass -> new TrainClassResponse(
                        trainClass.getClassCode().getCode(), trainClass.getClassName()))
                .toList();
    }

    private TrainRouteStopResponse toRouteStopResponse(TrainStop stop) {
        Station station = stop.getStation();
        return new TrainRouteStopResponse(
                station.getId(),
                station.getStationCode(),
                station.getStationName(),
                stop.getStopNumber(),
                stop.getDayNumber(),
                stop.getArrivalTime(),
                stop.getDepartureTime(),
                stop.getDistanceFromSource());
    }
}
