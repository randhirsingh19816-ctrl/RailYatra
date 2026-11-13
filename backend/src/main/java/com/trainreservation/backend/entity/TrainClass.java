package com.trainreservation.backend.entity;

import com.trainreservation.backend.enums.TrainClassCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Convert;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "train_classes",
        uniqueConstraints = @UniqueConstraint(name = "uk_train_class_code", columnNames = {"train_id", "class_code"}))
@Getter
@Setter
@NoArgsConstructor
public class TrainClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "train_id", nullable = false, foreignKey = @ForeignKey(name = "fk_train_classes_train"))
    private Train train;

    @NotNull
    @Convert(converter = TrainClassCodeConverter.class)
    @Column(name = "class_code", nullable = false, length = 20)
    private TrainClassCode classCode;

    @NotNull
    @Size(max = 80)
    @Column(name = "class_name", nullable = false, length = 80)
    private String className;

    @NotNull
    @DecimalMin("0.00")
    @Column(name = "base_fare", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseFare;
}
