package com.pahadcarry.order.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @Column(length = 36)
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    @Column(name = "customer_id", nullable = false, length = 36)
    private String customerId;

    @Column(name = "pickup_lat", nullable = false)
    private Double pickupLat;

    @Column(name = "pickup_lng", nullable = false)
    private Double pickupLng;

    @Column(name = "pickup_address", nullable = false, columnDefinition = "TEXT")
    private String pickupAddress;

    @Column(name = "pickup_contact_name", nullable = false, length = 100)
    private String pickupContactName;

    @Column(name = "pickup_contact_phone", nullable = false, length = 15)
    private String pickupContactPhone;

    @Column(name = "drop_lat", nullable = false)
    private Double dropLat;

    @Column(name = "drop_lng", nullable = false)
    private Double dropLng;

    @Column(name = "drop_address", nullable = false, columnDefinition = "TEXT")
    private String dropAddress;

    @Column(name = "drop_contact_name", nullable = false, length = 100)
    private String dropContactName;

    @Column(name = "drop_contact_phone", nullable = false, length = 15)
    private String dropContactPhone;

    @Column(name = "goods_description", nullable = false, columnDefinition = "TEXT")
    private String goodsDescription;

    @Column(name = "estimated_weight_kg", nullable = false, precision = 8, scale = 2)
    private BigDecimal estimatedWeightKg;

    @Column(name = "quantity_note")
    private String quantityNote;

    @Column(name = "order_type", nullable = false, length = 30)
    @Builder.Default
    private String orderType = "STANDARD_POOL"; // STANDARD_POOL, DEDICATED_TRIP

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "PLACED"; // PLACED, BATCHED, PICKED_UP, AT_HUB, OUT_FOR_DELIVERY, DELIVERED, CANCELLED

    @Column(name = "batch_id", length = 36)
    private String batchId;

    @Column(name = "price_estimate", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceEstimate;

    @Column(name = "final_price", precision = 10, scale = 2)
    private BigDecimal finalPrice;

    @Column(name = "payment_status", nullable = false, length = 30)
    @Builder.Default
    private String paymentStatus = "COD_PENDING"; // COD_PENDING, PAID

    @Column(name = "placed_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant placedAt = Instant.now();

    @Column(name = "batched_at")
    private Instant batchedAt;

    @Column(name = "picked_up_at")
    private Instant pickedUpAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_fee", precision = 10, scale = 2)
    private BigDecimal cancellationFee;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;
}
