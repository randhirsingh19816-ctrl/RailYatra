package com.trainreservation.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "coaches",
        uniqueConstraints = @UniqueConstraint(name = "uk_coach_number_per_run", columnNames = {"train_run_id", "coach_number"}))
@Getter
@Setter
@NoArgsConstructor
public class Coach {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "train_run_id", nullable = false, foreignKey = @ForeignKey(name = "fk_coaches_train_run"))
    private TrainRun trainRun;

    @NotBlank
    @Size(max = 10)
    @Column(name = "coach_number", nullable = false, length = 10)
    private String coachNumber;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "train_class_id", nullable = false, foreignKey = @ForeignKey(name = "fk_coaches_train_class"))
    private TrainClass trainClass;
}
