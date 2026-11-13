package com.trainreservation.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class CancellationRefundPolicy {

    private final BigDecimal refundPercentage;

    public CancellationRefundPolicy(
            @Value("${app.cancellation.refund-percentage:0.80}") BigDecimal refundPercentage) {
        if (refundPercentage.compareTo(BigDecimal.ZERO) < 0
                || refundPercentage.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Cancellation refund percentage must be between 0 and 1");
        }
        this.refundPercentage = refundPercentage;
    }

    public BigDecimal calculateRefundQuote(BigDecimal totalFare) {
        if (totalFare == null || totalFare.signum() < 0) {
            throw new IllegalArgumentException("Total fare must be non-negative");
        }
        return totalFare.multiply(refundPercentage).setScale(2, RoundingMode.HALF_UP);
    }
}
