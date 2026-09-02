package com.pahadcarry.order.service;

import com.pahadcarry.config.model.SystemConfig;
import com.pahadcarry.config.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class PricingService {

    private final SystemConfigRepository configRepository;

    /**
     * Calculates order fare using rate card:
     * total = baseFare + (distanceKm * perKmRate) + (weightKg * perKgRate)
     * Rounds to 2 decimal places.
     */
    public BigDecimal calculateFare(BigDecimal weightKg, double pickupLat, double pickupLng,
                                    double dropLat, double dropLng) {
        SystemConfig config = configRepository.getConfig();
        double distanceKm = haversineDistanceKm(pickupLat, pickupLng, dropLat, dropLng);

        BigDecimal distanceCost = config.getPerKmRate()
                .multiply(BigDecimal.valueOf(distanceKm))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal weightCost = config.getPerKgRate()
                .multiply(weightKg)
                .setScale(2, RoundingMode.HALF_UP);

        return config.getBaseFare().add(distanceCost).add(weightCost).setScale(2, RoundingMode.HALF_UP);
    }

    public boolean isDedicatedTrip(BigDecimal weightKg) {
        SystemConfig config = configRepository.getConfig();
        return weightKg.compareTo(config.getMaxStandardWeightKg()) > 0;
    }

    /**
     * Haversine formula to compute straight-line distance in km between two lat/lng points.
     */
    public static double haversineDistanceKm(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
