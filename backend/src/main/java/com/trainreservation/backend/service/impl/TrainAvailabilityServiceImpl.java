package com.trainreservation.backend.service.impl;

import com.trainreservation.backend.dto.response.TrainAvailabilityResponse;
import com.trainreservation.backend.dto.response.TrainStationResponse;
import com.trainreservation.backend.entity.Station;
import com.trainreservation.backend.entity.Train;
import com.trainreservation.backend.entity.TrainClass;
import com.trainreservation.backend.entity.TrainRun;
import com.trainreservation.backend.entity.TrainStop;
import com.trainreservation.backend.enums.BookingStatus;
import com.trainreservation.backend.enums.RunningStatus;
import com.trainreservation.backend.enums.TrainClassCode;
import com.trainreservation.backend.exception.InvalidRequestException;
import com.trainreservation.backend.exception.InvalidRouteException;
import com.trainreservation.backend.exception.InvalidTrainClassException;
import com.trainreservation.backend.exception.TrainNotFoundException;
import com.trainreservation.backend.exception.TrainNotOperatingException;
import com.trainreservation.backend.repository.SeatAllocationRepository;
import com.trainreservation.backend.repository.SeatRepository;
import com.trainreservation.backend.repository.TrainClassRepository;
import com.trainreservation.backend.repository.TrainRepository;
import com.trainreservation.backend.repository.TrainRunRepository;
import com.trainreservation.backend.repository.TrainStopRepository;
import com.trainreservation.backend.service.FareCalculationService;
import com.trainreservation.backend.service.TrainRouteLookupService;
import com.trainreservation.backend.service.TrainAvailabilityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class TrainAvailabilityServiceImpl implements TrainAvailabilityService {

    private static final String CURRENCY = "INR";
    private static final List<RunningStatus> OPERATING_STATUSES =
            List.of(RunningStatus.SCHEDULED, RunningStatus.RUNNING);
    private static final List<BookingStatus> ACTIVE_BOOKING_STATUSES =
            List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);

    private final TrainRouteLookupService trainRouteLookupService;
    private final TrainRepository trainRepository;
    private final TrainRunRepository trainRunRepository;
    private final TrainStopRepository trainStopRepository;
    private final TrainClassRepository trainClassRepository;
    private final SeatRepository seatRepository;
    private final SeatAllocationRepository seatAllocationRepository;
    private final FareCalculationService fareCalculationService;

    public TrainAvailabilityServiceImpl(TrainRouteLookupService trainRouteLookupService,
                                        TrainRepository trainRepository,
                                        TrainRunRepository trainRunRepository,
                                        TrainStopRepository trainStopRepository,
                                        TrainClassRepository trainClassRepository,
                                        SeatRepository seatRepository,
                                        SeatAllocationRepository seatAllocationRepository,
                                        FareCalculationService fareCalculationService) {
        this.trainRouteLookupService = trainRouteLookupService;
        this.trainRepository = trainRepository;
        this.trainRunRepository = trainRunRepository;
        this.trainStopRepository = trainStopRepository;
        this.trainClassRepository = trainClassRepository;
        this.seatRepository = seatRepository;
        this.seatAllocationRepository = seatAllocationRepository;
        this.fareCalculationService = fareCalculationService;
    }

    @Override
    @Transactional(readOnly = true)
    public TrainAvailabilityResponse getAvailability(Long trainId, String sourceIdentifier,
                                                      String destinationIdentifier, LocalDate journeyDate,
                                                      String requestedClassCode) {
        Train train = trainRepository.findById(trainId)
                .orElseThrow(() -> new TrainNotFoundException(trainId));
        if (!Boolean.TRUE.equals(train.getActive())) {
            throw new TrainNotOperatingException("Train is not active");
        }
        if (journeyDate == null) {
            throw new InvalidRequestException("Journey date is required");
        }
        if (journeyDate.isBefore(LocalDate.now())) {
            throw new InvalidRequestException("Journey date cannot be in the past");
        }

        TrainRun trainRun = trainRunRepository.findByTrainIdAndJourneyDate(trainId, journeyDate)
                .orElseThrow(() -> new TrainNotOperatingException(
                        "Train does not operate on the requested date"));
        if (!OPERATING_STATUSES.contains(trainRun.getRunningStatus())) {
            throw new TrainNotOperatingException("Train is not operating on the requested date");
        }

        Station sourceStation = trainRouteLookupService.findStation(sourceIdentifier);
        Station destinationStation = trainRouteLookupService.findStation(destinationIdentifier);
        if (sourceStation.getId().equals(destinationStation.getId())) {
            throw new InvalidRouteException("Source and destination stations must be different");
        }

        TrainClassCode classCode = parseClassCode(requestedClassCode);
        TrainClass trainClass = trainClassRepository.findByTrainIdAndClassCode(trainId, classCode)
                .orElseThrow(() -> new InvalidTrainClassException(
                        "Requested train class is not available on this train"));

        List<TrainStop> stops = trainStopRepository.findByTrainIdOrderByStopNumberAsc(trainId);
        TrainRouteLookupService.RouteSegment routeSegment = trainRouteLookupService
                .findOrderedSegment(stops, sourceStation.getId(), destinationStation.getId())
                .orElseThrow(() -> new InvalidRouteException(
                        "Source and destination must appear in this order on the train route"));
        if (routeSegment.source().getDepartureTime() == null) {
            throw new TrainNotOperatingException("Scheduled source departure is unavailable");
        }
        if (routeSegment.destination().getArrivalTime() == null) {
            throw new TrainNotOperatingException("Scheduled destination arrival is unavailable");
        }
        LocalDateTime sourceDeparture = trainRun.getJourneyDate()
                .plusDays(routeSegment.source().getDayNumber() - 1L)
                .atTime(routeSegment.source().getDepartureTime());
        if (!sourceDeparture.isAfter(LocalDateTime.now())) {
            throw new TrainNotOperatingException("Train has already departed from the source station");
        }
        BigDecimal journeyDistance = routeSegment.destination().getDistanceFromSource()
                .subtract(routeSegment.source().getDistanceFromSource());
        if (journeyDistance.signum() < 0) {
            throw new InvalidRouteException("Train route distances are inconsistent");
        }

        long totalSeats = seatRepository.countInventorySeats(trainRun.getId(), trainClass.getId());
        long occupiedSeats = totalSeats == 0 ? 0
                : seatAllocationRepository.countOccupiedSeatsForOverlappingSegments(
                        trainRun.getId(),
                        trainClass.getId(),
                        routeSegment.source().getStopNumber(),
                        routeSegment.destination().getStopNumber(),
                        ACTIVE_BOOKING_STATUSES);
        long availableSeats = Math.max(0, totalSeats - Math.min(totalSeats, occupiedSeats));
        BigDecimal fare = fareCalculationService.calculateFare(trainClass.getBaseFare(), journeyDistance);

        return new TrainAvailabilityResponse(
                train.getId(),
                trainRun.getId(),
                train.getTrainNumber(),
                train.getTrainName(),
                toStationResponse(routeSegment.source().getStation()),
                toStationResponse(routeSegment.destination().getStation()),
                trainRun.getJourneyDate(),
                routeSegment.source().getStopNumber(),
                routeSegment.destination().getStopNumber(),
                trainClass.getClassCode().getCode(),
                journeyDistance,
                totalSeats,
                availableSeats,
                fare,
                CURRENCY);
    }

    private TrainClassCode parseClassCode(String classCode) {
        if (classCode == null || classCode.isBlank()) {
            throw new InvalidTrainClassException("Train class code is required");
        }
        try {
            return TrainClassCode.fromCode(classCode.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new InvalidTrainClassException("Unsupported train class code");
        }
    }

    private TrainStationResponse toStationResponse(Station station) {
        return new TrainStationResponse(station.getId(), station.getStationCode(), station.getStationName());
    }
}
