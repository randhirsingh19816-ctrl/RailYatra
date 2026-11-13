package com.trainreservation.backend.service;

import com.trainreservation.backend.entity.Station;
import com.trainreservation.backend.entity.TrainStop;
import com.trainreservation.backend.exception.InvalidRequestException;
import com.trainreservation.backend.exception.StationNotFoundException;
import com.trainreservation.backend.repository.StationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TrainRouteLookupService {

    private final StationRepository stationRepository;

    public TrainRouteLookupService(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    public Station findStation(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new InvalidRequestException("Source and destination stations are required");
        }
        String normalizedIdentifier = identifier.trim();
        Optional<Station> station = stationRepository.findByStationCodeIgnoreCase(normalizedIdentifier);
        if (station.isPresent()) {
            return station.get();
        }

        try {
            long stationId = Long.parseLong(normalizedIdentifier);
            return stationRepository.findById(stationId)
                    .orElseThrow(() -> new StationNotFoundException(normalizedIdentifier));
        } catch (NumberFormatException exception) {
            throw new StationNotFoundException(normalizedIdentifier);
        }
    }

    public Optional<RouteSegment> findOrderedSegment(List<TrainStop> stops, Long sourceStationId,
                                                     Long destinationStationId) {
        for (TrainStop sourceStop : stops) {
            if (!sourceStop.getStation().getId().equals(sourceStationId)) {
                continue;
            }
            for (TrainStop destinationStop : stops) {
                if (destinationStop.getStation().getId().equals(destinationStationId)
                        && sourceStop.getStopNumber() < destinationStop.getStopNumber()) {
                    return Optional.of(new RouteSegment(sourceStop, destinationStop));
                }
            }
        }
        return Optional.empty();
    }

    public record RouteSegment(TrainStop source, TrainStop destination) {
    }
}
