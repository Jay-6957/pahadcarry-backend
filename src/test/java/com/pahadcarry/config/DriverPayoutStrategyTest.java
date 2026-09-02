package com.pahadcarry.config;

import com.pahadcarry.config.strategy.BasePlusPerStopStrategy;
import com.pahadcarry.config.strategy.CommissionPercentageStrategy;
import com.pahadcarry.config.strategy.FlatBatchStrategy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DriverPayoutStrategyTest {

    @Test
    void testBasePlusPerStopStrategy() {
        BasePlusPerStopStrategy strategy = new BasePlusPerStopStrategy();
        String params = "{\"baseBatchFee\": 300, \"perStopBonus\": 50}";

        // 3 stops completed -> 300 + (50 * 3) = 450
        BigDecimal payout = strategy.calculate(3, BigDecimal.valueOf(1500), params);
        assertEquals(BigDecimal.valueOf(450.0), payout);
    }

    @Test
    void testCommissionPercentageStrategy() {
        CommissionPercentageStrategy strategy = new CommissionPercentageStrategy();
        String params = "{\"commissionPercentage\": 75.0}";

        // 75% of 2000 = 1500
        BigDecimal payout = strategy.calculate(4, BigDecimal.valueOf(2000.00), params);
        assertEquals(new BigDecimal("1500.00"), payout);
    }

    @Test
    void testFlatBatchStrategy() {
        FlatBatchStrategy strategy = new FlatBatchStrategy();
        String params = "{\"flatFee\": 600}";

        BigDecimal payout = strategy.calculate(5, BigDecimal.valueOf(2500), params);
        assertEquals(BigDecimal.valueOf(600.0), payout);
    }
}
