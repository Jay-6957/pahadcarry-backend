package com.pahadcarry.driver.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class DriverRegisterDto {
    @NotBlank private String name;
    @NotBlank private String vehicleType;
    @NotBlank private String vehicleRegNumber;
    @NotNull @Min(1) private Integer vehicleCapacityKg;
    @NotBlank private String homeBaseArea;
    private String aadhaarNumber;
    private String licenseNumber;
}
