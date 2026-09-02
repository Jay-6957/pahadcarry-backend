package com.pahadcarry.config.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "system_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfig {

    @Id
    @Builder.Default
    private Integer id = 1;

    @Column(name = "base_fare", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal baseFare = BigDecimal.valueOf(150.00);

    @Column(name = "per_km_rate", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal perKmRate = BigDecimal.valueOf(18.00);

    @Column(name = "per_kg_rate", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal perKgRate = BigDecimal.valueOf(4.00);

    @Column(name = "max_standard_weight_kg", nullable = false, precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal maxStandardWeightKg = BigDecimal.valueOf(100.00);

    @Column(name = "batch_order_threshold", nullable = false)
    @Builder.Default
    private Integer batchOrderThreshold = 8;

    @Column(name = "batch_max_wait_hours", nullable = false)
    @Builder.Default
    private Integer batchMaxWaitHours = 4;

    @Column(name = "payout_strategy", nullable = false, length = 50)
    @Builder.Default
    private String payoutStrategy = "BASE_PLUS_PER_STOP";

    @Column(name = "payout_params_json", nullable = false, columnDefinition = "TEXT")
    @Builder.Default
    private String payoutParamsJson = "{\"baseBatchFee\": 300, \"perStopBonus\": 50, \"abortedStopFee\": 100}";

    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
