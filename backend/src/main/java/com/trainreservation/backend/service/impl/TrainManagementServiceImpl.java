package com.trainreservation.backend.service.impl;

import com.trainreservation.backend.dto.request.TrainRequest;
import com.trainreservation.backend.dto.response.TrainPageResponse;
import com.trainreservation.backend.dto.response.TrainResponse;
import com.trainreservation.backend.entity.Train;
import com.trainreservation.backend.exception.DuplicateTrainNumberException;
import com.trainreservation.backend.exception.InvalidRequestException;
import com.trainreservation.backend.exception.TrainNotFoundException;
import com.trainreservation.backend.repository.TrainRepository;
import com.trainreservation.backend.service.TrainManagementService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Service
public class TrainManagementServiceImpl implements TrainManagementService {

    private static final Set<String> SORT_FIELDS = Set.of(
            "id", "trainNumber", "trainName", "trainType", "active");

    private final TrainRepository trainRepository;

    public TrainManagementServiceImpl(TrainRepository trainRepository) {
        this.trainRepository = trainRepository;
    }

    @Override
    @Transactional
    public TrainResponse createTrain(TrainRequest request) {
        Train train = new Train();
        applyRequest(train, request);
        if (trainRepository.existsByTrainNumberIgnoreCase(train.getTrainNumber())) {
            throw new DuplicateTrainNumberException(train.getTrainNumber());
        }
        return toResponse(trainRepository.saveAndFlush(train));
    }

    @Override
    @Transactional(readOnly = true)
    public TrainPageResponse getTrains(int page, int size, String sortBy, String direction, Boolean active) {
        Pageable pageable = createPageable(page, size, sortBy, direction);
        Page<Train> trainPage = active == null
                ? trainRepository.findAll(pageable)
                : trainRepository.findByActive(active, pageable);
        return toPageResponse(trainPage);
    }

    @Override
    @Transactional(readOnly = true)
    public TrainResponse getTrainById(Long id) {
        return trainRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new TrainNotFoundException(id));
    }

    @Override
    @Transactional
    public TrainResponse updateTrain(Long id, TrainRequest request) {
        Train train = trainRepository.findById(id)
                .orElseThrow(() -> new TrainNotFoundException(id));
        applyRequest(train, request);
        if (trainRepository.existsByTrainNumberIgnoreCaseAndIdNot(train.getTrainNumber(), id)) {
            throw new DuplicateTrainNumberException(train.getTrainNumber());
        }
        return toResponse(trainRepository.saveAndFlush(train));
    }

    @Override
    @Transactional
    public TrainResponse deactivateTrain(Long id) {
        Train train = trainRepository.findById(id)
                .orElseThrow(() -> new TrainNotFoundException(id));
        if (Boolean.TRUE.equals(train.getActive())) {
            train.setActive(false);
            train = trainRepository.saveAndFlush(train);
        }
        return toResponse(train);
    }

    @Override
    @Transactional(readOnly = true)
    public TrainPageResponse searchTrains(String keyword, int page, int size,
                                          String sortBy, String direction) {
        Pageable pageable = createPageable(page, size, sortBy, direction);
        if (keyword == null || keyword.isBlank()) {
            return new TrainPageResponse(java.util.List.of(), page, size, 0, 0);
        }
        String searchTerm = keyword.trim();
        Page<Train> trainPage = trainRepository
                .findByTrainNumberContainingIgnoreCaseOrTrainNameContainingIgnoreCase(
                        searchTerm, searchTerm, pageable);
        return toPageResponse(trainPage);
    }

    private Pageable createPageable(int page, int size, String sortBy, String direction) {
        if (page < 0 || size < 1 || size > 100) {
            throw new InvalidRequestException("Page must be non-negative and size must be between 1 and 100");
        }
        if (sortBy == null || !SORT_FIELDS.contains(sortBy)) {
            throw new InvalidRequestException("Unsupported train sort field");
        }

        Sort.Direction sortDirection;
        try {
            sortDirection = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException("Sort direction must be asc or desc");
        }
        Sort sort = Sort.by(sortDirection, sortBy);
        if (!"id".equals(sortBy)) {
            sort = sort.and(Sort.by("id"));
        }
        return PageRequest.of(page, size, sort);
    }

    private void applyRequest(Train train, TrainRequest request) {
        if (request == null) {
            throw new InvalidRequestException("Train details are required");
        }
        train.setTrainNumber(request.trainNumber().trim().toUpperCase(Locale.ROOT));
        train.setTrainName(request.trainName().trim());
        train.setTrainType(request.trainType());
        if (train.getActive() == null) {
            train.setActive(true);
        }
    }

    private TrainPageResponse toPageResponse(Page<Train> trainPage) {
        return new TrainPageResponse(
                trainPage.getContent().stream().map(this::toResponse).toList(),
                trainPage.getNumber(),
                trainPage.getSize(),
                trainPage.getTotalElements(),
                trainPage.getTotalPages());
    }

    private TrainResponse toResponse(Train train) {
        return new TrainResponse(train.getId(), train.getTrainNumber(), train.getTrainName(),
                train.getTrainType(), Boolean.TRUE.equals(train.getActive()));
    }
}
