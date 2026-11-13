package com.trainreservation.backend.repository;

import com.trainreservation.backend.entity.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

public interface StationRepository extends JpaRepository<Station, Long> {

    Optional<Station> findByStationCodeIgnoreCase(String stationCode);

    boolean existsByStationCodeIgnoreCase(String stationCode);

    boolean existsByStationCodeIgnoreCaseAndIdNot(String stationCode, Long id);

    List<Station> findByStationCodeContainingIgnoreCaseOrStationNameContainingIgnoreCaseOrCityContainingIgnoreCase(
            String stationCode, String stationName, String city);

    List<Station> findByStationCodeContainingIgnoreCaseOrStationNameContainingIgnoreCaseOrCityContainingIgnoreCase(
            String stationCode, String stationName, String city, Sort sort);

}
