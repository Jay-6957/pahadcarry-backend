package com.pahadcarry.common;

import org.springframework.stereotype.Component;

@Component
public class DriverEligibilityGuard {

    public void validateKycApproved(String kycStatus) {
        if (!"APPROVED".equalsIgnoreCase(kycStatus)) {
            throw PahadCarryException.forbidden(
                    ErrorCode.KYC_NOT_APPROVED,
                    "Driver KYC is not approved (current status: " + kycStatus + "). Please wait for Ops review."
            );
        }
    }

    public void validateDriverEligibleForAssignment(String kycStatus, String availabilityStatus) {
        validateKycApproved(kycStatus);
        if (!"ONLINE".equalsIgnoreCase(availabilityStatus)) {
            throw PahadCarryException.badRequest(
                    ErrorCode.DRIVER_OFFLINE,
                    "Driver is currently offline and unavailable for batch assignment."
            );
        }
    }
}
