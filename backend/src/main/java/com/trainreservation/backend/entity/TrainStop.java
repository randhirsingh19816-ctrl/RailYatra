package com.trainreservation.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "train_stops",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_train_stop_number", columnNames = {"train_id", "stop_number"}),
                @UniqueConstraint(name = "uk_train_stop_station", columnNames = {"train_id", "station_id"})
        },
        indexes = @Index(name = "idx_train_stops_station", columnList = "station_id"))
@Getter
@Setter
@NoArgsConstructor
public class TrainStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "train_id", nullable = false, foreignKey = @ForeignKey(name = "fk_train_stops_train"))
    private Train train;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "station_id", nullable = false, foreignKey = @ForeignKey(name = "fk_train_stops_station"))
    private Station station;

    @NotNull
    @Min(1)
    @Column(name = "stop_number", nullable = false)
    private Integer stopNumber;

    @Column(name = "arrival_time")
    private LocalTime arrivalTime;

    @Column(name = "departure_time")
    private LocalTime departureTime;

    @NotNull
    @Min(1)
    @Column(name = "day_number", nullable = false)
    private Integer dayNumber;

    @NotNull
    @Min(0)
    @Column(name = "distance_from_source", nullable = false, precision = 10, scale = 2)
    private BigDecimal distanceFromSource;
}
