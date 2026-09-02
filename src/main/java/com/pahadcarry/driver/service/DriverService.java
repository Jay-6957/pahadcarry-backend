package com.pahadcarry.driver.service;

import com.pahadcarry.common.DriverEligibilityGuard;
import com.pahadcarry.common.ErrorCode;
import com.pahadcarry.common.PahadCarryException;
import com.pahadcarry.driver.dto.DriverRegisterDto;
import com.pahadcarry.driver.model.Driver;
import com.pahadcarry.driver.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DriverService {

    private final DriverRepository driverRepository;
    private final DriverEligibilityGuard eligibilityGuard;

    public Driver getProfile(String driverId) {
        return driverRepository.findById(driverId)
                .orElseThrow(() -> PahadCarryException.notFound(ErrorCode.NOT_FOUND, "Driver not found"));
    }

    @Transactional
    public Driver completeRegistration(String driverId, DriverRegisterDto dto) {
        Driver driver = getProfile(driverId);
        driver.setName(dto.getName());
        driver.setVehicleType(dto.getVehicleType());
        driver.setVehicleRegNumber(dto.getVehicleRegNumber());
        driver.setVehicleCapacityKg(dto.getVehicleCapacityKg());
        driver.setHomeBaseArea(dto.getHomeBaseArea());
        if (dto.getAadhaarNumber() != null) driver.setAadhaarNumber(dto.getAadhaarNumber());
        if (dto.getLicenseNumber() != null) driver.setLicenseNumber(dto.getLicenseNumber());
        driver.setKycStatus("PENDING");
        return driverRepository.save(driver);
    }

    @Transactional
    public Driver toggleAvailability(String driverId, String newStatus) {
        Driver driver = getProfile(driverId);

        if ("ONLINE".equals(newStatus)) {
            // Guard: only KYC-approved drivers can go online
            eligibilityGuard.validateKycApproved(driver.getKycStatus());
        }

        driver.setAvailabilityStatus(newStatus);
        driver.setLastActionDescription("Toggled availability to " + newStatus);
        driver.setLastActionTimestamp(java.time.Instant.now());
        if ("ONLINE".equals(newStatus)) {
            driver.setNextActionDescription("Online & available for new corridor batch");
        } else if ("OFFLINE".equals(newStatus)) {
            driver.setNextActionDescription("Driver offline / off-duty");
        }
        return driverRepository.save(driver);
    }

    @Transactional
    public void updateLocation(String driverId, double lat, double lng) {
        Driver driver = getProfile(driverId);
        if (!"ONLINE".equals(driver.getAvailabilityStatus()) && !"ON_TRIP".equals(driver.getAvailabilityStatus())) {
            throw PahadCarryException.badRequest(ErrorCode.DRIVER_OFFLINE, "Driver must be ONLINE to send location pings");
        }
        driver.setCurrentLat(lat);
        driver.setCurrentLng(lng);
        driver.setLastActionTimestamp(java.time.Instant.now());
        driverRepository.save(driver);
    }
}
