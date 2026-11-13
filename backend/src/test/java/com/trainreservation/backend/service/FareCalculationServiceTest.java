package com.trainreservation.backend.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FareCalculationServiceTest {

    @Test
    void calculatesBaseFarePlusDistanceRate() {
        FareCalculationService fareCalculationService =
                new FareCalculationService(new BigDecimal("1.25"));

        BigDecimal fare = fareCalculationService.calculateFare(
                new BigDecimal("100.00"), new BigDecimal("180.50"));

        assertEquals(new BigDecimal("325.63"), fare);
    }

    @Test
    void roundsFareToTwoDecimalsUsingHalfUp() {
        FareCalculationService fareCalculationService =
                new FareCalculationService(new BigDecimal("1.005"));

        BigDecimal fare = fareCalculationService.calculateFare(
                new BigDecimal("100.00"), new BigDecimal("1.00"));

        assertEquals(new BigDecimal("101.01"), fare);
    }
}
