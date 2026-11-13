package com.trainreservation.backend.entity;

import com.trainreservation.backend.enums.BookingStatus;
import com.trainreservation.backend.enums.PaymentStatus;
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
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings",
        indexes = {
                @Index(name = "idx_bookings_user_date", columnList = "user_id, booking_date"),
                @Index(name = "idx_bookings_run", columnList = "train_run_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 20)
    @Column(name = "pnr_number", nullable = false, unique = true, length = 20)
    private String pnrNumber;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_bookings_user"))
    private User user;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "train_run_id", nullable = false, foreignKey = @ForeignKey(name = "fk_bookings_train_run"))
    private TrainRun trainRun;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_station_id", nullable = false, foreignKey = @ForeignKey(name = "fk_bookings_source_station"))
    private Station sourceStation;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_station_id", nullable = false, foreignKey = @ForeignKey(name = "fk_bookings_destination_station"))
    private Station destinationStation;

    @NotNull
    @Column(name = "source_stop_number", nullable = false)
    @Min(1)
    private Integer sourceStopNumber;

    @NotNull
    @Column(name = "destination_stop_number", nullable = false)
    @Min(2)
    private Integer destinationStopNumber;

    @NotNull
    @Column(name = "booking_date", nullable = false)
    private LocalDateTime bookingDate;

    @NotNull
    @DecimalMin("0.00")
    @Column(name = "total_fare", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalFare;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "booking_status", nullable = false, length = 20)
    private BookingStatus bookingStatus = BookingStatus.PENDING;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 30)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;
}
