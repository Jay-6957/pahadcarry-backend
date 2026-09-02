package com.pahadcarry.config.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class FlatBatchStrategy implements DriverPayoutStrategy {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public BigDecimal calculate(int completedStops, BigDecimal totalOrderValue, String paramsJson) {
        try {
            JsonNode params = mapper.readTree(paramsJson);
            return BigDecimal.valueOf(params.path("flatFee").asDouble(500));
        } catch (Exception e) {
            return BigDecimal.valueOf(500);
        }
    }

    @Override
    public String strategyName() { return "FLAT_BATCH"; }
}
