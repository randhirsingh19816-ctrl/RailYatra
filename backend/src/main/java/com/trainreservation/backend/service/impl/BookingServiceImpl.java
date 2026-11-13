package com.trainreservation.backend.service.impl;

import com.trainreservation.backend.dto.request.BookingPassengerRequest;
import com.trainreservation.backend.dto.request.CreateBookingRequest;
import com.trainreservation.backend.dto.response.BookingConfirmationResponse;
import com.trainreservation.backend.dto.response.BookingHistoryItemResponse;
import com.trainreservation.backend.dto.response.BookingHistoryPageResponse;
import com.trainreservation.backend.dto.response.BookingPassengerResponse;
import com.trainreservation.backend.dto.response.CancellationResponse;
import com.trainreservation.backend.dto.response.TrainStationResponse;
import com.trainreservation.backend.entity.Booking;
import com.trainreservation.backend.entity.Cancellation;
import com.trainreservation.backend.entity.Passenger;
import com.trainreservation.backend.entity.Payment;
import com.trainreservation.backend.entity.Seat;
import com.trainreservation.backend.entity.SeatAllocation;
import com.trainreservation.backend.entity.Station;
import com.trainreservation.backend.entity.Train;
import com.trainreservation.backend.entity.TrainClass;
import com.trainreservation.backend.entity.TrainRun;
import com.trainreservation.backend.entity.TrainStop;
import com.trainreservation.backend.entity.User;
import com.trainreservation.backend.enums.BookingStatus;
import com.trainreservation.backend.enums.CancellationStatus;
import com.trainreservation.backend.enums.PaymentStatus;
import com.trainreservation.backend.enums.RunningStatus;
import com.trainreservation.backend.enums.TrainClassCode;
import com.trainreservation.backend.enums.UserRole;
import com.trainreservation.backend.exception.BookingNotAuthorizedException;
import com.trainreservation.backend.exception.BookingAccessDeniedException;
import com.trainreservation.backend.exception.BookingAlreadyCancelledException;
import com.trainreservation.backend.exception.BookingNotFoundException;
import com.trainreservation.backend.exception.CancellationNotPermittedException;
import com.trainreservation.backend.exception.InsufficientSeatAvailabilityException;
import com.trainreservation.backend.exception.InvalidRequestException;
import com.trainreservation.backend.exception.InvalidPnrException;
import com.trainreservation.backend.exception.InvalidRouteException;
import com.trainreservation.backend.exception.InvalidTrainClassException;
import com.trainreservation.backend.exception.PnrGenerationException;
import com.trainreservation.backend.exception.TrainNotOperatingException;
import com.trainreservation.backend.exception.TrainRunNotFoundException;
import com.trainreservation.backend.repository.BookingRepository;
import com.trainreservation.backend.repository.CancellationRepository;
import com.trainreservation.backend.repository.PassengerRepository;
import com.trainreservation.backend.repository.PaymentRepository;
import com.trainreservation.backend.repository.SeatAllocationRepository;
import com.trainreservation.backend.repository.SeatRepository;
import com.trainreservation.backend.repository.TrainClassRepository;
import com.trainreservation.backend.repository.TrainRunRepository;
import com.trainreservation.backend.repository.TrainStopRepository;
import com.trainreservation.backend.repository.UserRepository;
import com.trainreservation.backend.service.BookingService;
import com.trainreservation.backend.service.CancellationRefundPolicy;
import com.trainreservation.backend.service.FareCalculationService;
import com.trainreservation.backend.service.TrainRouteLookupService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class BookingServiceImpl implements BookingService {

    private static final String CURRENCY = "INR";
    private static final int PNR_LENGTH = 16;
    private static final String PNR_PATTERN = "[2-9A-HJ-NP-Z]{16}";
    private static final int MAX_PNR_ATTEMPTS = 5;
    private static final char[] PNR_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final List<BookingStatus> ACTIVE_BOOKING_STATUSES =
            List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);
    private static final List<RunningStatus> OPERATING_STATUSES =
            List.of(RunningStatus.SCHEDULED, RunningStatus.RUNNING);

    private final BookingRepository bookingRepository;
    private final CancellationRepository cancellationRepository;
    private final UserRepository userRepository;
    private final TrainRunRepository trainRunRepository;
    private final TrainStopRepository trainStopRepository;
    private final TrainClassRepository trainClassRepository;
    private final SeatRepository seatRepository;
    private final SeatAllocationRepository seatAllocationRepository;
    private final PassengerRepository passengerRepository;
    private final PaymentRepository paymentRepository;
    private final TrainRouteLookupService trainRouteLookupService;
    private final FareCalculationService fareCalculationService;
    private final CancellationRefundPolicy cancellationRefundPolicy;

    public BookingServiceImpl(BookingRepository bookingRepository,
                              CancellationRepository cancellationRepository,
                              UserRepository userRepository,
                              TrainRunRepository trainRunRepository,
                              TrainStopRepository trainStopRepository,
                              TrainClassRepository trainClassRepository,
                              SeatRepository seatRepository,
                              SeatAllocationRepository seatAllocationRepository,
                              PassengerRepository passengerRepository,
                              PaymentRepository paymentRepository,
                              TrainRouteLookupService trainRouteLookupService,
                              FareCalculationService fareCalculationService,
                              CancellationRefundPolicy cancellationRefundPolicy) {
        this.bookingRepository = bookingRepository;
        this.cancellationRepository = cancellationRepository;
        this.userRepository = userRepository;
        this.trainRunRepository = trainRunRepository;
        this.trainStopRepository = trainStopRepository;
        this.trainClassRepository = trainClassRepository;
        this.seatRepository = seatRepository;
        this.seatAllocationRepository = seatAllocationRepository;
        this.passengerRepository = passengerRepository;
        this.paymentRepository = paymentRepository;
        this.trainRouteLookupService = trainRouteLookupService;
        this.fareCalculationService = fareCalculationService;
        this.cancellationRefundPolicy = cancellationRefundPolicy;
    }

    @Override
    @Transactional
    public BookingConfirmationResponse createBooking(CreateBookingRequest request) {
        User user = findAuthenticatedUser();
        if (request == null || request.trainRunId() == null || request.journeyDate() == null) {
            throw new InvalidRequestException("Train run and journey date are required");
        }
        if (request.journeyDate().isBefore(LocalDate.now())) {
            throw new InvalidRequestException("Journey date cannot be in the past");
        }

        TrainRun trainRun = trainRunRepository.findByIdForBooking(request.trainRunId())
                .orElseThrow(() -> new TrainRunNotFoundException(request.trainRunId()));
        Train train = trainRun.getTrain();
        if (!Boolean.TRUE.equals(train.getActive())
                || !OPERATING_STATUSES.contains(trainRun.getRunningStatus())
                || !request.journeyDate().equals(trainRun.getJourneyDate())) {
            throw new TrainNotOperatingException("Train run is not available on the requested date");
        }

        Station sourceStation = trainRouteLookupService.findStation(request.sourceStation());
        Station destinationStation = trainRouteLookupService.findStation(request.destinationStation());
        if (sourceStation.getId().equals(destinationStation.getId())) {
            throw new InvalidRouteException("Source and destination stations must be different");
        }

        List<TrainStop> stops = trainStopRepository.findByTrainIdOrderByStopNumberAsc(train.getId());
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

        TrainClassCode classCode = parseClassCode(request.classCode());
        TrainClass trainClass = trainClassRepository.findByTrainIdAndClassCode(train.getId(), classCode)
                .orElseThrow(() -> new InvalidTrainClassException(
                        "Requested train class is not available on this train"));

        if (request.passengers() == null || request.passengers().isEmpty()) {
            throw new InvalidRequestException("At least one passenger is required");
        }

        int passengerCount = request.passengers().size();
        int requestedStart = routeSegment.source().getStopNumber();
        int requestedEnd = routeSegment.destination().getStopNumber();
        seatAllocationRepository.deleteInactiveAllocationsForOverlappingSegments(
                trainRun.getId(), trainClass.getId(), requestedStart, requestedEnd,
                ACTIVE_BOOKING_STATUSES);
        long totalSeats = seatRepository.countInventorySeats(trainRun.getId(), trainClass.getId());
        long occupiedSeats = totalSeats == 0 ? 0
                : seatAllocationRepository.countOccupiedSeatsForOverlappingSegments(
                        trainRun.getId(), trainClass.getId(), requestedStart, requestedEnd,
                        ACTIVE_BOOKING_STATUSES);
        long availableSeats = Math.max(0, totalSeats - Math.min(totalSeats, occupiedSeats));
        if (passengerCount > availableSeats) {
            throw new InsufficientSeatAvailabilityException(passengerCount, availableSeats);
        }

        List<Seat> availableSeatList = seatRepository.findAvailableSeatsForOverlappingSegments(
                trainRun.getId(), trainClass.getId(), requestedStart, requestedEnd,
                ACTIVE_BOOKING_STATUSES, PageRequest.of(0, passengerCount));
        if (availableSeatList.size() < passengerCount) {
            throw new InsufficientSeatAvailabilityException(passengerCount, availableSeatList.size());
        }

        BigDecimal journeyDistance = routeSegment.destination().getDistanceFromSource()
                .subtract(routeSegment.source().getDistanceFromSource());
        if (journeyDistance.signum() < 0) {
            throw new InvalidRouteException("Train route distances are inconsistent");
        }
        BigDecimal farePerPassenger = fareCalculationService.calculateFare(
                trainClass.getBaseFare(), journeyDistance);
        BigDecimal totalFare = farePerPassenger.multiply(BigDecimal.valueOf(passengerCount))
                .setScale(2, java.math.RoundingMode.HALF_UP);

        Booking booking = new Booking();
        booking.setPnrNumber(generateUniquePnr());
        booking.setUser(user);
        booking.setTrainRun(trainRun);
        booking.setSourceStation(routeSegment.source().getStation());
        booking.setDestinationStation(routeSegment.destination().getStation());
        booking.setSourceStopNumber(requestedStart);
        booking.setDestinationStopNumber(requestedEnd);
        booking.setBookingDate(LocalDateTime.now());
        booking.setTotalFare(totalFare);
        booking.setBookingStatus(BookingStatus.PENDING);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking = bookingRepository.saveAndFlush(booking);

        List<Passenger> passengers = new ArrayList<>(passengerCount);
        for (int index = 0; index < passengerCount; index++) {
            BookingPassengerRequest passengerRequest = request.passengers().get(index);
            Passenger passenger = new Passenger();
            passenger.setBooking(booking);
            passenger.setFullName(passengerRequest.fullName().trim());
            passenger.setAge(passengerRequest.age());
            passenger.setGender(passengerRequest.gender());
            passenger.setSeat(availableSeatList.get(index));
            passengers.add(passenger);
        }
        passengers = passengerRepository.saveAllAndFlush(passengers);

        List<SeatAllocation> allocations = new ArrayList<>();
        for (int index = 0; index < passengers.size(); index++) {
            Passenger passenger = passengers.get(index);
            Seat seat = availableSeatList.get(index);
            for (int segmentNumber = requestedStart; segmentNumber < requestedEnd; segmentNumber++) {
                SeatAllocation allocation = new SeatAllocation();
                allocation.setTrainRun(trainRun);
                allocation.setSeat(seat);
                allocation.setBooking(booking);
                allocation.setPassenger(passenger);
                allocation.setSegmentNumber(segmentNumber);
                allocations.add(allocation);
            }
        }
        seatAllocationRepository.saveAllAndFlush(allocations);

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(totalFare);
        payment.setPaymentStatus(PaymentStatus.PENDING);
        paymentRepository.saveAndFlush(payment);

        List<BookingPassengerResponse> passengerResponses = passengers.stream()
                .map(this::toPassengerResponse)
                .toList();
        return new BookingConfirmationResponse(
                booking.getPnrNumber(),
                booking.getBookingStatus(),
                booking.getPaymentStatus(),
                train.getId(),
                train.getTrainNumber(),
                train.getTrainName(),
                trainRun.getJourneyDate(),
                toStationResponse(routeSegment.source().getStation()),
                toStationResponse(routeSegment.destination().getStation()),
                trainClass.getClassCode().getCode(),
                passengerResponses,
                farePerPassenger,
                totalFare,
                CURRENCY,
                booking.getBookingDate());
    }

    @Override
    @Transactional(readOnly = true)
    public BookingHistoryPageResponse getMyBookings(int pageNumber, int pageSize) {
        if (pageNumber < 0 || pageSize < 1 || pageSize > 100) {
            throw new InvalidRequestException("Page must be non-negative and size must be between 1 and 100");
        }
        User user = findRequester().user();
        Page<Booking> bookingPage = bookingRepository.findByUserIdOrderByBookingDateDesc(
                user.getId(), PageRequest.of(pageNumber, pageSize));
        List<BookingHistoryItemResponse> items = bookingPage.getContent().stream()
                .map(booking -> new BookingHistoryItemResponse(
                        booking.getPnrNumber(),
                        booking.getTrainRun().getTrain().getId(),
                        booking.getTrainRun().getTrain().getTrainNumber(),
                        booking.getTrainRun().getTrain().getTrainName(),
                        booking.getTrainRun().getJourneyDate(),
                        toStationResponse(booking.getSourceStation()),
                        toStationResponse(booking.getDestinationStation()),
                        passengerRepository.countByBookingId(booking.getId()),
                        booking.getTotalFare(),
                        booking.getBookingStatus(),
                        booking.getPaymentStatus()))
                .toList();
        return new BookingHistoryPageResponse(items, bookingPage.getNumber(), bookingPage.getSize(),
                bookingPage.getTotalElements(), bookingPage.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public BookingConfirmationResponse getBookingByPnr(String pnrNumber) {
        Requester requester = findRequester();
        String normalizedPnr = normalizePnr(pnrNumber);
        Booking booking = bookingRepository.findByPnrNumber(normalizedPnr)
                .orElseThrow(BookingNotFoundException::new);
        verifyOwnership(booking, requester);
        List<Passenger> passengers = passengerRepository.findByBookingId(booking.getId());
        return toBookingConfirmation(booking, passengers);
    }

    @Override
    @Transactional
    public CancellationResponse cancelBooking(String pnrNumber) {
        Requester requester = findRequester();
        String normalizedPnr = normalizePnr(pnrNumber);
        Long trainRunId = bookingRepository.findTrainRunIdByPnrNumber(normalizedPnr)
                .orElseThrow(BookingNotFoundException::new);
        trainRunRepository.findByIdForBooking(trainRunId)
                .orElseThrow(() -> new TrainRunNotFoundException(trainRunId));
        Booking booking = bookingRepository.findByPnrNumberForUpdate(normalizedPnr)
                .orElseThrow(BookingNotFoundException::new);
        verifyOwnership(booking, requester);
        if (booking.getBookingStatus() == BookingStatus.CANCELLED
                || cancellationRepository.existsByBookingId(booking.getId())) {
            throw new BookingAlreadyCancelledException();
        }
        if (booking.getBookingStatus() != BookingStatus.PENDING
                && booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new CancellationNotPermittedException(
                    "Only pending or confirmed bookings may be cancelled");
        }

        TrainRun trainRun = booking.getTrainRun();
        TrainStop sourceStop = trainStopRepository.findByTrainIdAndStopNumber(
                        trainRun.getTrain().getId(), booking.getSourceStopNumber())
                .orElseThrow(() -> new CancellationNotPermittedException(
                        "Scheduled source departure is unavailable"));
        if (sourceStop.getDepartureTime() == null) {
            throw new CancellationNotPermittedException("Scheduled source departure is unavailable");
        }
        LocalDateTime departureTime = trainRun.getJourneyDate()
                .plusDays(sourceStop.getDayNumber() - 1L)
                .atTime(sourceStop.getDepartureTime());
        if (!LocalDateTime.now().isBefore(departureTime)) {
            throw new CancellationNotPermittedException(
                    "Booking can only be cancelled before the scheduled source departure");
        }

        LocalDateTime cancellationTime = LocalDateTime.now();
        BigDecimal refundAmount = cancellationRefundPolicy.calculateRefundQuote(booking.getTotalFare());
        seatAllocationRepository.deleteByTrainRunIdAndBookingId(trainRun.getId(), booking.getId());
        booking.setBookingStatus(BookingStatus.CANCELLED);
        bookingRepository.saveAndFlush(booking);

        Cancellation cancellation = new Cancellation();
        cancellation.setBooking(booking);
        cancellation.setCancellationDate(cancellationTime);
        cancellation.setRefundAmount(refundAmount);
        cancellation.setCancellationStatus(CancellationStatus.REQUESTED);
        cancellationRepository.saveAndFlush(cancellation);

        return new CancellationResponse(
                booking.getPnrNumber(),
                booking.getBookingStatus(),
                refundAmount,
                cancellation.getCancellationStatus(),
                cancellationTime);
    }

    private BookingConfirmationResponse toBookingConfirmation(Booking booking, List<Passenger> passengers) {
        String classCode = passengers.stream()
                .map(Passenger::getSeat)
                .filter(java.util.Objects::nonNull)
                .map(seat -> seat.getCoach().getTrainClass().getClassCode().getCode())
                .findFirst()
                .orElse(null);
        BigDecimal farePerPassenger = passengers.isEmpty()
                ? BigDecimal.ZERO.setScale(2)
                : booking.getTotalFare().divide(BigDecimal.valueOf(passengers.size()), 2,
                        java.math.RoundingMode.HALF_UP);
        Train train = booking.getTrainRun().getTrain();
        List<BookingPassengerResponse> passengerResponses = passengers.stream()
                .map(this::toPassengerResponse)
                .toList();
        return new BookingConfirmationResponse(
                booking.getPnrNumber(),
                booking.getBookingStatus(),
                booking.getPaymentStatus(),
                train.getId(),
                train.getTrainNumber(),
                train.getTrainName(),
                booking.getTrainRun().getJourneyDate(),
                toStationResponse(booking.getSourceStation()),
                toStationResponse(booking.getDestinationStation()),
                classCode,
                passengerResponses,
                farePerPassenger,
                booking.getTotalFare(),
                CURRENCY,
                booking.getBookingDate());
    }

    private Requester findRequester() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null || authentication.getName().isBlank()
                || "anonymousUser".equals(authentication.getName())) {
            throw new BookingAccessDeniedException();
        }
        User user = userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(BookingAccessDeniedException::new);
        boolean admin = user.getRole() == UserRole.ADMIN;
        if (!admin && user.getRole() != UserRole.USER) {
            throw new BookingAccessDeniedException();
        }
        return new Requester(user, admin);
    }

    private void verifyOwnership(Booking booking, Requester requester) {
        if (!requester.admin() && !booking.getUser().getId().equals(requester.user().getId())) {
            throw new BookingAccessDeniedException();
        }
    }

    private String normalizePnr(String pnrNumber) {
        if (pnrNumber == null || !pnrNumber.trim().toUpperCase(Locale.ROOT).matches(PNR_PATTERN)) {
            throw new InvalidPnrException();
        }
        return pnrNumber.trim().toUpperCase(Locale.ROOT);
    }

    private User findAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null || authentication.getName().isBlank()
                || "anonymousUser".equals(authentication.getName())) {
            throw new BookingNotAuthorizedException();
        }
        User user = userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(BookingNotAuthorizedException::new);
        if (user.getRole() != UserRole.USER) {
            throw new BookingNotAuthorizedException();
        }
        return user;
    }

    private String generateUniquePnr() {
        for (int attempt = 0; attempt < MAX_PNR_ATTEMPTS; attempt++) {
            StringBuilder pnr = new StringBuilder(PNR_LENGTH);
            for (int index = 0; index < PNR_LENGTH; index++) {
                pnr.append(PNR_ALPHABET[SECURE_RANDOM.nextInt(PNR_ALPHABET.length)]);
            }
            String candidate = pnr.toString();
            if (!bookingRepository.existsByPnrNumber(candidate)) {
                return candidate;
            }
        }
        throw new PnrGenerationException();
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

    private BookingPassengerResponse toPassengerResponse(Passenger passenger) {
        Seat seat = passenger.getSeat();
        return new BookingPassengerResponse(passenger.getFullName(), passenger.getAge(), passenger.getGender(),
                seat == null ? null : seat.getCoach().getCoachNumber(),
                seat == null ? null : seat.getSeatNumber());
    }

    private TrainStationResponse toStationResponse(Station station) {
        return new TrainStationResponse(station.getId(), station.getStationCode(), station.getStationName());
    }

    private record Requester(User user, boolean admin) {
    }
}
