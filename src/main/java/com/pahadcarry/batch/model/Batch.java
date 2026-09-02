package com.pahadcarry.batch.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "batches")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Batch {

    @Id
    @Column(length = 36)
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    @Column(name = "area_cluster", nullable = false, length = 100)
    private String areaCluster;

    @Column(name = "batch_date", nullable = false)
    @Builder.Default
    private LocalDate batchDate = LocalDate.now();

    @Column(name = "driver_id", length = 36)
    private String driverId;

    @Column(name = "hub_center_id", nullable = false, length = 36)
    @Builder.Default
    private String hubCenterId = "hub_haldwani_central";

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "FORMING"; // FORMING, ASSIGNED, IN_PROGRESS, COMPLETED

    @Column(name = "total_weight_kg", nullable = false, precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal totalWeightKg = BigDecimal.ZERO;

    @Column(name = "driver_payout", precision = 10, scale = 2)
    private BigDecimal driverPayout;

    @Column(name = "payout_strategy_used", length = 50)
    private String payoutStrategyUsed;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
