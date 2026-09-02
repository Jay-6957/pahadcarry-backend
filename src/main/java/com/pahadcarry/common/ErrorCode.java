package com.pahadcarry.common;

public enum ErrorCode {
    BAD_REQUEST("BAD_REQUEST"),
    UNAUTHORIZED("UNAUTHORIZED"),
    FORBIDDEN("FORBIDDEN"),
    NOT_FOUND("NOT_FOUND"),
    VALIDATION_ERROR("VALIDATION_ERROR"),
    CONFLICT("CONFLICT"),
    
    // Auth
    INVALID_OTP("INVALID_OTP"),
    OTP_EXPIRED("OTP_EXPIRED"),
    OTP_LOCKED("OTP_LOCKED"),
    
    // Driver / KYC
    KYC_NOT_APPROVED("KYC_NOT_APPROVED"),
    DRIVER_OFFLINE("DRIVER_OFFLINE"),
    DRIVER_BUSY("DRIVER_BUSY"),
    
    // Orders & Batches
    ORDER_ALREADY_BATCHED("ORDER_ALREADY_BATCHED"),
    ORDER_CANNOT_BE_CANCELLED("ORDER_CANNOT_BE_CANCELLED"),
    BATCH_NOT_FOUND("BATCH_NOT_FOUND"),
    BATCH_ALREADY_ASSIGNED("BATCH_ALREADY_ASSIGNED"),
    STOP_ALREADY_COMPLETED("STOP_ALREADY_COMPLETED"),
    
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR");

    private final String value;

    ErrorCode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
