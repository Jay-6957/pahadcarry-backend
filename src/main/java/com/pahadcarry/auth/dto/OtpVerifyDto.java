package com.pahadcarry.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OtpVerifyDto {
    @NotBlank(message = "Phone number is required")
    private String phone;

    @NotBlank(message = "OTP code is required")
    private String otp;

    private String userType = "CUSTOMER"; // CUSTOMER or DRIVER
    private String name; // optional for first time signup
}
