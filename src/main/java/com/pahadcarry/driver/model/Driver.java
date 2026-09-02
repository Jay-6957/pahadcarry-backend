package com.pahadcarry.driver.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "drivers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Driver {

    @Id
    @Column(length = 36)
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 15)
    private String phone;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "vehicle_type", nullable = false, length = 50)
    private String vehicleType;

    @Column(name = "vehicle_reg_number", nullable = false, length = 30)
    private String vehicleRegNumber;

    @Column(name = "vehicle_capacity_kg", nullable = false)
    private Integer vehicleCapacityKg;

    @Column(name = "vehicle_photo_url")
    private String vehiclePhotoUrl;

    @Column(name = "kyc_status", nullable = false, length = 20)
    @Builder.Default
    private String kycStatus = "PENDING"; // PENDING, APPROVED, REJECTED

    @Column(name = "aadhaar_number", length = 20)
    private String aadhaarNumber;

    @Column(name = "aadhaar_doc_url")
    private String aadhaarDocUrl;

    @Column(name = "license_number", length = 30)
    private String licenseNumber;

    @Column(name = "license_doc_url")
    private String licenseDocUrl;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "reviewed_by", length = 36)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "availability_status", nullable = false, length = 20)
    @Builder.Default
    private String availabilityStatus = "OFFLINE"; // OFFLINE, ONLINE, ON_TRIP

    @Column(name = "home_base_area", nullable = false, length = 100)
    private String homeBaseArea;

    @Column(name = "current_lat")
    private Double currentLat;

    @Column(name = "current_lng")
    private Double currentLng;

    @Column(name = "fcm_token")
    private String fcmToken;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "last_action_description")
    private String lastActionDescription;

    @Column(name = "last_action_timestamp")
    private Instant lastActionTimestamp;

    @Column(name = "next_action_description")
    private String nextActionDescription;

    @Column(name = "current_location_name")
    private String currentLocationName;
}
