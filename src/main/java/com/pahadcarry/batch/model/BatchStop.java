package com.pahadcarry.batch.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "batch_stops")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchStop {

    @Id
    @Column(length = 36)
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    @Column(name = "batch_id", nullable = false, length = 36)
    private String batchId;

    @Column(name = "order_id", length = 36)
    private String orderId;

    @Column(nullable = false)
    private Integer sequence;

    @Column(name = "stop_type", nullable = false, length = 20)
    private String stopType; // PICKUP, HUB, DROP

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING"; // PENDING, COMPLETED

    @Column(name = "proof_photo_url", length = 500)
    private String proofPhotoUrl;

    @Column(name = "cash_collected", precision = 10, scale = 2)
    private BigDecimal cashCollected;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Transient
    private com.pahadcarry.order.model.Order order;
}
