package com.trainreservation.backend.service.impl;

import com.trainreservation.backend.dto.request.TrainRequest;
import com.trainreservation.backend.dto.response.TrainPageResponse;
import com.trainreservation.backend.dto.response.TrainResponse;
import com.trainreservation.backend.entity.Train;
import com.trainreservation.backend.enums.TrainType;
import com.trainreservation.backend.exception.DuplicateTrainNumberException;
import com.trainreservation.backend.exception.InvalidRequestException;
import com.trainreservation.backend.exception.TrainNotFoundException;
import com.trainreservation.backend.repository.TrainRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrainManagementServiceImplTest {

    private TrainRepository trainRepository;
    private TrainManagementServiceImpl trainService;

    @BeforeEach
    void setUp() {
        trainRepository = mock(TrainRepository.class);
        trainService = new TrainManagementServiceImpl(trainRepository);
    }

    @Test
    void createsTrainWithUppercaseNumberAndActiveStatus() {
        when(trainRepository.existsByTrainNumberIgnoreCase("12951")).thenReturn(false);
        when(trainRepository.saveAndFlush(any(Train.class))).thenAnswer(invocation -> {
            Train train = invocation.getArgument(0);
            train.setId(7L);
            return train;
        });

        TrainResponse response = trainService.createTrain(request("12951"));

        assertEquals(7L, response.id());
        assertEquals("12951", response.trainNumber());
        assertEquals("Mumbai Rajdhani", response.trainName());
        assertEquals(TrainType.EXPRESS, response.trainType());
        assertTrue(response.active());
    }

    @Test
    void normalizesTrainNumberCase() {
        when(trainRepository.existsByTrainNumberIgnoreCase("T12951")).thenReturn(false);
        when(trainRepository.saveAndFlush(any(Train.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals("T12951", trainService.createTrain(request("t12951")).trainNumber());
    }

    @Test
    void rejectsDuplicateTrainNumberWithoutSaving() {
        when(trainRepository.existsByTrainNumberIgnoreCase("12951")).thenReturn(true);

        assertThrows(DuplicateTrainNumberException.class, () -> trainService.createTrain(request("12951")));

        verify(trainRepository, never()).saveAndFlush(any());
    }

    @Test
    void getsTrainByIdAndReturnsNotFoundForUnknownId() {
        when(trainRepository.findById(7L)).thenReturn(Optional.of(train(7L, "12951", true)));

        assertEquals("12951", trainService.getTrainById(7L).trainNumber());
        assertThrows(TrainNotFoundException.class, () -> trainService.getTrainById(999L));
    }

    @Test
    void updatesTrainWithoutChangingIdOrActiveState() {
        Train train = train(7L, "12951", false);
        when(trainRepository.findById(7L)).thenReturn(Optional.of(train));
        when(trainRepository.existsByTrainNumberIgnoreCaseAndIdNot("12951", 7L)).thenReturn(false);
        when(trainRepository.saveAndFlush(train)).thenReturn(train);

        TrainResponse response = trainService.updateTrain(7L, request("12951"));

        assertEquals(7L, response.id());
        assertFalse(response.active());
        assertEquals("Mumbai Rajdhani", response.trainName());
        verify(trainRepository).existsByTrainNumberIgnoreCaseAndIdNot("12951", 7L);
    }

    @Test
    void rejectsDuplicateTrainNumberDuringUpdate() {
        Train train = train(7L, "12951", true);
        when(trainRepository.findById(7L)).thenReturn(Optional.of(train));
        when(trainRepository.existsByTrainNumberIgnoreCaseAndIdNot("12951", 7L)).thenReturn(true);

        assertThrows(DuplicateTrainNumberException.class,
                () -> trainService.updateTrain(7L, request("12951")));

        verify(trainRepository, never()).saveAndFlush(any());
    }

    @Test
    void deactivationIsIdempotent() {
        Train train = train(7L, "12951", true);
        when(trainRepository.findById(7L)).thenReturn(Optional.of(train));
        when(trainRepository.saveAndFlush(train)).thenReturn(train);

        assertFalse(trainService.deactivateTrain(7L).active());
        assertFalse(trainService.deactivateTrain(7L).active());

        verify(trainRepository).saveAndFlush(train);
        verify(trainRepository, never()).delete(any());
    }

    @Test
    void listsWithOptionalActiveFilterPaginationAndSorting() {
        Train train = train(7L, "12951", false);
        Pageable pageable = PageRequest.of(1, 5,
                Sort.by(Sort.Direction.DESC, "trainName").and(Sort.by("id")));
        when(trainRepository.findByActive(false, pageable))
                .thenReturn(new PageImpl<>(List.of(train), pageable, 6));

        TrainPageResponse response = trainService.getTrains(1, 5, "trainName", "desc", false);

        assertEquals(1, response.page());
        assertEquals(5, response.size());
        assertEquals(6, response.totalElements());
        assertFalse(response.trains().get(0).active());
        verify(trainRepository).findByActive(false, pageable);
    }

    @Test
    void listsAllTrainsWhenActiveFilterIsOmitted() {
        Pageable pageable = PageRequest.of(0, 10,
                Sort.by(Sort.Direction.ASC, "trainName").and(Sort.by("id")));
        when(trainRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        assertTrue(trainService.getTrains(0, 10, "trainName", "asc", null).trains().isEmpty());

        verify(trainRepository).findAll(pageable);
    }

    @Test
    void searchesByTrainNumberOrNameAndReturnsEmptyPageForNoMatch() {
        Train train = train(7L, "12951", true);
        Pageable pageable = PageRequest.of(0, 10,
                Sort.by(Sort.Direction.ASC, "trainName").and(Sort.by("id")));
        when(trainRepository.findByTrainNumberContainingIgnoreCaseOrTrainNameContainingIgnoreCase(
                "Rajdhani", "Rajdhani", pageable))
                .thenReturn(new PageImpl<>(List.of(train), pageable, 1));
        when(trainRepository.findByTrainNumberContainingIgnoreCaseOrTrainNameContainingIgnoreCase(
                "Missing", "Missing", pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        assertEquals(1, trainService.searchTrains(" Rajdhani ", 0, 10, "trainName", "asc")
                .totalElements());
        TrainPageResponse noMatch = trainService.searchTrains("Missing", 0, 10, "trainName", "asc");
        assertTrue(noMatch.trains().isEmpty());
        assertEquals(0, noMatch.totalElements());
        assertTrue(trainService.searchTrains(" ", 0, 10, "trainName", "asc").trains().isEmpty());
    }

    @Test
    void rejectsInvalidPaginationAndSorting() {
        assertThrows(InvalidRequestException.class,
                () -> trainService.getTrains(-1, 10, "trainName", "asc", null));
        assertThrows(InvalidRequestException.class,
                () -> trainService.getTrains(0, 101, "trainName", "asc", null));
        assertThrows(InvalidRequestException.class,
                () -> trainService.getTrains(0, 10, "password", "asc", null));
        assertThrows(InvalidRequestException.class,
                () -> trainService.getTrains(0, 10, "trainName", "sideways", null));
    }

    private TrainRequest request(String trainNumber) {
        return new TrainRequest(trainNumber, "Mumbai Rajdhani", TrainType.EXPRESS);
    }

    private Train train(Long id, String number, boolean active) {
        Train train = new Train();
        train.setId(id);
        train.setTrainNumber(number);
        train.setTrainName("Mumbai Rajdhani");
        train.setTrainType(TrainType.EXPRESS);
        train.setActive(active);
        return train;
    }
}
