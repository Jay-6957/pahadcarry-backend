package com.pahadcarry.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressDto {
    private String id;
    
    @NotBlank(message = "Address label is required (e.g., Home, Shop)")
    private String label;

    @NotNull(message = "Latitude is required")
    private Double lat;

    @NotNull(message = "Longitude is required")
    private Double lng;

    @NotBlank(message = "Village or town is required")
    private String villageOrTown;

    @NotBlank(message = "Landmark is required")
    private String landmark;
}
