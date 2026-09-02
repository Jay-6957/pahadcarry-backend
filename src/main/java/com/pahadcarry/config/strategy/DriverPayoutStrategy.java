package com.pahadcarry.config.strategy;

import java.math.BigDecimal;

public interface DriverPayoutStrategy {
    /**
     * Calculates the total payout for a driver after completing a batch.
     *
     * @param completedStops number of delivery stops completed
     * @param totalOrderValue sum of all order prices in the batch
     * @param paramsJson JSON string of strategy-specific parameters
     * @return calculated payout in INR
     */
    BigDecimal calculate(int completedStops, BigDecimal totalOrderValue, String paramsJson);

    String strategyName();
}
