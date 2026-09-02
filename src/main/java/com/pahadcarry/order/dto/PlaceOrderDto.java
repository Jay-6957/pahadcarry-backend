package com.pahadcarry.order.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PlaceOrderDto {
    // Pickup
    @NotNull private Double pickupLat;
    @NotNull private Double pickupLng;
    @NotBlank private String pickupAddress;
    @NotBlank private String pickupContactName;
    @NotBlank @Pattern(regexp = "^[0-9]{10}$") private String pickupContactPhone;

    // Drop
    @NotNull private Double dropLat;
    @NotNull private Double dropLng;
    @NotBlank private String dropAddress;
    @NotBlank private String dropContactName;
    @NotBlank @Pattern(regexp = "^[0-9]{10}$") private String dropContactPhone;

    // Goods
    @NotBlank private String goodsDescription;
    @NotNull @DecimalMin("0.1") private BigDecimal estimatedWeightKg;
    private String quantityNote;
}
