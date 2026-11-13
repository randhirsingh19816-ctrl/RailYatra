package com.trainreservation.backend.entity;

import com.trainreservation.backend.enums.RunningStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "train_runs",
        uniqueConstraints = @UniqueConstraint(name = "uk_train_run_journey_date", columnNames = {"train_id", "journey_date"}),
        indexes = @Index(name = "idx_train_runs_journey_date", columnList = "journey_date"))
@Getter
@Setter
@NoArgsConstructor
public class TrainRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "train_id", nullable = false, foreignKey = @ForeignKey(name = "fk_train_runs_train"))
    private Train train;

    @NotNull
    @Column(name = "journey_date", nullable = false)
    private LocalDate journeyDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "running_status", nullable = false, length = 20)
    private RunningStatus runningStatus = RunningStatus.SCHEDULED;
}
