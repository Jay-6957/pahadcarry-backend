package com.pahadcarry.driver;

import com.pahadcarry.common.ApiResponse;
import com.pahadcarry.driver.dto.DriverRegisterDto;
import com.pahadcarry.driver.model.Driver;
import com.pahadcarry.driver.service.DriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/drivers/me")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    @GetMapping
    public ResponseEntity<ApiResponse<Driver>> getProfile(Authentication auth) {
        String driverId = (String) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.ok(driverService.getProfile(driverId)));
    }

    @PutMapping("/register")
    public ResponseEntity<ApiResponse<Driver>> completeRegistration(
            Authentication auth,
            @Valid @RequestBody DriverRegisterDto dto) {
        String driverId = (String) auth.getPrincipal();
        Driver driver = driverService.completeRegistration(driverId, dto);
        return ResponseEntity.ok(ApiResponse.ok(driver));
    }

    @PatchMapping("/availability")
    public ResponseEntity<ApiResponse<Map<String, String>>> toggleAvailability(
            Authentication auth,
            @RequestParam String status) {
        String driverId = (String) auth.getPrincipal();
        Driver driver = driverService.toggleAvailability(driverId, status);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "availabilityStatus", driver.getAvailabilityStatus(),
                "message", "Availability updated to " + driver.getAvailabilityStatus()
        )));
    }

    @PostMapping("/location")
    public ResponseEntity<ApiResponse<Map<String, String>>> updateLocation(
            Authentication auth,
            @RequestParam double lat,
            @RequestParam double lng) {
        String driverId = (String) auth.getPrincipal();
        driverService.updateLocation(driverId, lat, lng);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Location updated")));
    }
}
