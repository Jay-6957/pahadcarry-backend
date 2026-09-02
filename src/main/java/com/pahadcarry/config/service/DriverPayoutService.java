package com.pahadcarry.config.service;

import com.pahadcarry.batch.model.Batch;
import com.pahadcarry.batch.repository.BatchRepository;
import com.pahadcarry.batch.repository.BatchStopRepository;
import com.pahadcarry.config.model.SystemConfig;
import com.pahadcarry.config.repository.SystemConfigRepository;
import com.pahadcarry.config.strategy.BasePlusPerStopStrategy;
import com.pahadcarry.config.strategy.CommissionPercentageStrategy;
import com.pahadcarry.config.strategy.DriverPayoutStrategy;
import com.pahadcarry.config.strategy.FlatBatchStrategy;
import com.pahadcarry.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverPayoutService {

    private final SystemConfigRepository configRepository;
    private final BatchRepository batchRepository;
    private final BatchStopRepository batchStopRepository;
    private final OrderRepository orderRepository;
    private final BasePlusPerStopStrategy basePlusPerStop;
    private final CommissionPercentageStrategy commissionPercentage;
    private final FlatBatchStrategy flatBatch;

    private final Map<String, DriverPayoutStrategy> strategyMap = Map.of(
            "BASE_PLUS_PER_STOP", new BasePlusPerStopStrategy(),
            "COMMISSION_PERCENTAGE", new CommissionPercentageStrategy(),
            "FLAT_BATCH", new FlatBatchStrategy()
    );

    @Transactional
    public BigDecimal calculateAndRecordPayout(String batchId) {
        SystemConfig config = configRepository.getConfig();
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found: " + batchId));

        int completedDropStops = (int) batchStopRepository.findByBatchIdOrderBySequenceAsc(batchId).stream()
                .filter(s -> "DROP".equals(s.getStopType()) && "COMPLETED".equals(s.getStatus()))
                .count();

        BigDecimal totalOrderValue = orderRepository.findByBatchId(batchId).stream()
                .map(o -> o.getPriceEstimate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        DriverPayoutStrategy strategy = strategyMap.getOrDefault(
                config.getPayoutStrategy(), basePlusPerStop);

        BigDecimal payout = strategy.calculate(completedDropStops, totalOrderValue, config.getPayoutParamsJson());

        batch.setDriverPayout(payout);
        batch.setPayoutStrategyUsed(config.getPayoutStrategy());
        batch.setStatus("COMPLETED");
        batch.setCompletedAt(Instant.now());
        batchRepository.save(batch);

        log.info("Batch {} payout ₹{} using strategy {}", batchId, payout, config.getPayoutStrategy());
        return payout;
    }
}
