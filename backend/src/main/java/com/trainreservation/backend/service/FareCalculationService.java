package com.trainreservation.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class FareCalculationService {

    private final BigDecimal perKilometreRate;

    public FareCalculationService(
            @Value("${app.fare.per-kilometre-rate:1.00}") BigDecimal perKilometreRate) {
        if (perKilometreRate.signum() < 0) {
            throw new IllegalArgumentException("Fare per-kilometre rate cannot be negative");
        }
        this.perKilometreRate = perKilometreRate;
    }

    public BigDecimal calculateFare(BigDecimal classBaseFare, BigDecimal journeyDistance) {
        if (classBaseFare == null || classBaseFare.signum() < 0
                || journeyDistance == null || journeyDistance.signum() < 0) {
            throw new IllegalArgumentException("Fare inputs must be non-negative values");
        }
        return classBaseFare.add(journeyDistance.multiply(perKilometreRate))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
