package com.pahadcarry.common;

import com.pahadcarry.admin.model.AdminUser;
import com.pahadcarry.admin.repository.AdminUserRepository;
import com.pahadcarry.batch.model.HubCenter;
import com.pahadcarry.batch.repository.HubCenterRepository;
import com.pahadcarry.config.model.SystemConfig;
import com.pahadcarry.config.repository.SystemConfigRepository;
import com.pahadcarry.driver.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final AdminUserRepository adminUserRepository;
    private final DriverRepository driverRepository;
    private final SystemConfigRepository configRepository;
    private final HubCenterRepository hubCenterRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // 1. Seed Super Admin if not present
        if (!adminUserRepository.existsByEmail("ops@pahadcarry.in")) {
            AdminUser admin = AdminUser.builder()
                    .id("admin_haldwani_super")
                    .name("Kumaon Ops Commander")
                    .email("ops@pahadcarry.in")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .role("SUPER_ADMIN")
                    .build();
            adminUserRepository.save(admin);
            log.info("Seeded default admin user: ops@pahadcarry.in / admin123");
        }

        // 2. Seed System Config if not present
        if (configRepository.findById(1).isEmpty()) {
            SystemConfig config = SystemConfig.builder()
                    .id(1)
                    .baseFare(BigDecimal.valueOf(150.00))
                    .perKmRate(BigDecimal.valueOf(18.00))
                    .perKgRate(BigDecimal.valueOf(4.00))
                    .maxStandardWeightKg(BigDecimal.valueOf(100.00))
                    .batchOrderThreshold(8)
                    .batchMaxWaitHours(4)
                    .payoutStrategy("BASE_PLUS_PER_STOP")
                    .payoutParamsJson("{\"baseBatchFee\": 300, \"perStopBonus\": 50, \"abortedStopFee\": 100}")
                    .build();
            configRepository.save(config);
            log.info("Seeded default system config rate card and payout strategy");
        }

        // 3. Seed Haldwani Central Hub if not present
        if (hubCenterRepository.findById("hub_haldwani_central").isEmpty()) {
            HubCenter hub = HubCenter.builder()
                    .id("hub_haldwani_central")
                    .name("Haldwani Central Hub")
                    .lat(29.2183)
                    .lng(79.5130)
                    .servesAreas("[\"Nainital\", \"Bhowali\", \"Bhimtal\", \"Almora\", \"Kathgodam\"]")
                    .build();
            hubCenterRepository.save(hub);
            log.info("Seeded Haldwani Central Hub");
        }

        // 4. Seed Demo Approved & Online Driver if not present
        if (!driverRepository.existsById("driver_haldwani_demo")) {
            com.pahadcarry.driver.model.Driver driver = com.pahadcarry.driver.model.Driver.builder()
                    .id("driver_haldwani_demo")
                    .name("Bahadur Singh")
                    .phone("9876543210")
                    .vehicleType("Mahindra Bolero Pickup")
                    .vehicleRegNumber("UK04 CA 1234")
                    .vehicleCapacityKg(1000)
                    .homeBaseArea("Haldwani")
                    .kycStatus("APPROVED")
                    .availabilityStatus("ONLINE")
                    .currentLat(29.2183)
                    .currentLng(79.5130)
                    .currentLocationName("Haldwani Central Hub, NH-109")
                    .lastActionDescription("Completed vehicle safety check and went ONLINE")
                    .lastActionTimestamp(java.time.Instant.now().minusSeconds(180))
                    .nextActionDescription("Waiting for corridor batch assignment")
                    .build();
            driverRepository.save(driver);
            log.info("Seeded demo approved driver: Bahadur Singh (9876543210) - ONLINE");
        }
    }
}
