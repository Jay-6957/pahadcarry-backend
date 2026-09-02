package com.pahadcarry.config.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class CommissionPercentageStrategy implements DriverPayoutStrategy {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public BigDecimal calculate(int completedStops, BigDecimal totalOrderValue, String paramsJson) {
        try {
            JsonNode params = mapper.readTree(paramsJson);
            double percentage = params.path("commissionPercentage").asDouble(75.0);
            return totalOrderValue.multiply(BigDecimal.valueOf(percentage / 100.0))
                    .setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return totalOrderValue.multiply(BigDecimal.valueOf(0.75)).setScale(2, RoundingMode.HALF_UP);
        }
    }

    @Override
    public String strategyName() { return "COMMISSION_PERCENTAGE"; }
}
