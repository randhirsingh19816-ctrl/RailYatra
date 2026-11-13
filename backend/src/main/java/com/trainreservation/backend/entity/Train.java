package com.trainreservation.backend.entity;

import com.trainreservation.backend.enums.TrainType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "trains")
@Getter
@Setter
@NoArgsConstructor
public class Train {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9-]{1,20}$")
    @Column(name = "train_number", nullable = false, unique = true, length = 20)
    private String trainNumber;

    @NotBlank
    @Size(max = 120)
    @Column(name = "train_name", nullable = false, length = 120)
    private String trainName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "train_type", nullable = false, length = 30)
    private TrainType trainType;

    @NotNull
    @Column(nullable = false)
    private Boolean active = true;
}
