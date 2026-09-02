package com.pahadcarry.order.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_photos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPhoto {

    @Id
    @Column(length = 36)
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    @Column(name = "order_id", nullable = false, length = 36)
    private String orderId;

    @Column(name = "photo_url", nullable = false, length = 500)
    private String photoUrl;

    @Column(name = "photo_type", nullable = false, length = 30)
    @Builder.Default
    private String photoType = "ITEM_CARGO"; // ITEM_CARGO, DELIVERY_PROOF

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant uploadedAt = Instant.now();
}
