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

@Entity
@Table(name = "seat_allocations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_seat_allocation_run_seat_segment",
                columnNames = {"train_run_id", "seat_id", "segment_number"}),
        indexes = @Index(name = "idx_seat_allocations_booking", columnList = "booking_id"))
@Getter
@Setter
@NoArgsConstructor
public class SeatAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "train_run_id", nullable = false, foreignKey = @ForeignKey(name = "fk_seat_allocations_train_run"))
    private TrainRun trainRun;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false, foreignKey = @ForeignKey(name = "fk_seat_allocations_seat"))
    private Seat seat;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, foreignKey = @ForeignKey(name = "fk_seat_allocations_booking"))
    private Booking booking;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "passenger_id", nullable = false, foreignKey = @ForeignKey(name = "fk_seat_allocations_passenger"))
    private Passenger passenger;

    @NotNull
    @Min(1)
    @Column(name = "segment_number", nullable = false)
    private Integer segmentNumber;
}
