package com.pahadcarry.order;

import com.pahadcarry.config.model.SystemConfig;
import com.pahadcarry.config.repository.SystemConfigRepository;
import com.pahadcarry.order.service.PricingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

    @Mock
    private SystemConfigRepository configRepository;

    @InjectMocks
    private PricingService pricingService;

    private SystemConfig config;

    @BeforeEach
    void setUp() {
        config = SystemConfig.builder()
                .baseFare(BigDecimal.valueOf(150.00))
                .perKmRate(BigDecimal.valueOf(18.00))
                .perKgRate(BigDecimal.valueOf(4.00))
                .maxStandardWeightKg(BigDecimal.valueOf(100.00))
                .build();
    }

    @Test
    void testStandardFareCalculation() {
        when(configRepository.getConfig()).thenReturn(config);

        // Distance approx ~19 km from Haldwani (29.2183, 79.5130) to Nainital (29.3803, 79.4636)
        // 50 kg weight
        BigDecimal fare = pricingService.calculateFare(
                BigDecimal.valueOf(50.0),
                29.2183, 79.5130,
                29.3803, 79.4636
        );

        assertNotNull(fare);
        // Base(150) + Dist(~18.8 * 18 = 338.4) + Weight(50 * 4 = 200) ≈ 688
        assertTrue(fare.compareTo(BigDecimal.valueOf(600)) > 0);
        assertTrue(fare.compareTo(BigDecimal.valueOf(800)) < 0);
    }

    @Test
    void testDedicatedTripThreshold() {
        when(configRepository.getConfig()).thenReturn(config);

        assertFalse(pricingService.isDedicatedTrip(BigDecimal.valueOf(50)));
        assertFalse(pricingService.isDedicatedTrip(BigDecimal.valueOf(100)));
        assertTrue(pricingService.isDedicatedTrip(BigDecimal.valueOf(100.5)));
        assertTrue(pricingService.isDedicatedTrip(BigDecimal.valueOf(250)));
    }
}
