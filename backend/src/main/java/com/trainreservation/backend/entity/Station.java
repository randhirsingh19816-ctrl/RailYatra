package com.trainreservation.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stations", indexes = {
        @Index(name = "idx_stations_name", columnList = "station_name")
})
@Getter
@Setter
@NoArgsConstructor
public class Station {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Pattern(regexp = "^[A-Z0-9]{2,10}$")
    @Column(name = "station_code", nullable = false, unique = true, length = 10)
    private String stationCode;

    @NotBlank
    @Size(max = 120)
    @Column(name = "station_name", nullable = false, length = 120)
    private String stationName;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String city;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String state;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean active = true;
}
