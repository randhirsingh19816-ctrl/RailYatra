package com.trainreservation.backend.entity;

import com.trainreservation.backend.enums.CancellationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cancellations",
        uniqueConstraints = @UniqueConstraint(name = "uk_cancellations_booking", columnNames = "booking_id"))
@Getter
@Setter
@NoArgsConstructor
public class Cancellation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cancellations_booking"))
    private Booking booking;

    @NotNull
    @Column(name = "cancellation_date", nullable = false)
    private LocalDateTime cancellationDate;

    @NotNull
    @DecimalMin("0.00")
    @Column(name = "refund_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal refundAmount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_status", nullable = false, length = 20)
    private CancellationStatus cancellationStatus = CancellationStatus.REQUESTED;
}
