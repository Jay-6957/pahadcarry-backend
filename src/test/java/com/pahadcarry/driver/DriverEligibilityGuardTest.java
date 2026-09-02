package com.pahadcarry.driver;

import com.pahadcarry.common.DriverEligibilityGuard;
import com.pahadcarry.common.ErrorCode;
import com.pahadcarry.common.PahadCarryException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DriverEligibilityGuardTest {

    private final DriverEligibilityGuard guard = new DriverEligibilityGuard();

    @Test
    void testApprovedKycCanGoOnline() {
        assertDoesNotThrow(() -> guard.validateKycApproved("APPROVED"));
    }

    @Test
    void testPendingKycCannotGoOnline() {
        PahadCarryException ex = assertThrows(PahadCarryException.class,
                () -> guard.validateKycApproved("PENDING"));
        assertEquals(ErrorCode.KYC_NOT_APPROVED, ex.getErrorCode());
    }

    @Test
    void testApprovedAndOnlinePassesAssignment() {
        assertDoesNotThrow(() -> guard.validateDriverEligibleForAssignment("APPROVED", "ONLINE"));
    }

    @Test
    void testOfflineDriverBlockedFromAssignment() {
        PahadCarryException ex = assertThrows(PahadCarryException.class,
                () -> guard.validateDriverEligibleForAssignment("APPROVED", "OFFLINE"));
        assertEquals(ErrorCode.DRIVER_OFFLINE, ex.getErrorCode());
    }
}
