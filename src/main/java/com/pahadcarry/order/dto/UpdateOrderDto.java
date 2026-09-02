package com.pahadcarry.order.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateOrderDto {
    private String pickupAddress;
    private Double pickupLat;
    private Double pickupLng;
    private String pickupContactName;
    private String pickupContactPhone;
    private String dropAddress;
    private Double dropLat;
    private Double dropLng;
    private String dropContactName;
    private String dropContactPhone;
    private String goodsDescription;
    @DecimalMin("0.1")
    private BigDecimal estimatedWeightKg;
    private String quantityNote;
}
