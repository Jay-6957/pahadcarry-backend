package com.pahadcarry.config.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BasePlusPerStopStrategy implements DriverPayoutStrategy {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public BigDecimal calculate(int completedStops, BigDecimal totalOrderValue, String paramsJson) {
        try {
            JsonNode params = mapper.readTree(paramsJson);
            BigDecimal baseBatchFee = BigDecimal.valueOf(params.path("baseBatchFee").asDouble(300));
            BigDecimal perStopBonus = BigDecimal.valueOf(params.path("perStopBonus").asDouble(50));
            return baseBatchFee.add(perStopBonus.multiply(BigDecimal.valueOf(completedStops)));
        } catch (Exception e) {
            return BigDecimal.valueOf(300 + 50L * completedStops);
        }
    }

    @Override
    public String strategyName() { return "BASE_PLUS_PER_STOP"; }
}
