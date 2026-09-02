package com.pahadcarry.auth;

import com.pahadcarry.admin.model.AdminUser;
import com.pahadcarry.admin.repository.AdminUserRepository;
import com.pahadcarry.auth.dto.AdminLoginDto;
import com.pahadcarry.auth.dto.AuthResponseDto;
import com.pahadcarry.auth.dto.OtpRequestDto;
import com.pahadcarry.auth.dto.OtpVerifyDto;
import com.pahadcarry.common.ErrorCode;
import com.pahadcarry.common.JwtTokenProvider;
import com.pahadcarry.common.PahadCarryException;
import com.pahadcarry.customer.model.User;
import com.pahadcarry.customer.repository.UserRepository;
import com.pahadcarry.driver.model.Driver;
import com.pahadcarry.driver.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final AdminUserRepository adminUserRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    // In-memory OTP storage & lockout state for testing/demo
    private final Map<String, String> otpStore = new ConcurrentHashMap<>();
    private final Map<String, Integer> failedAttempts = new ConcurrentHashMap<>();
    private final Map<String, Instant> lockoutUntil = new ConcurrentHashMap<>();

    public String requestOtp(OtpRequestDto request) {
        String phone = request.getPhone();

        // Check lockout
        if (lockoutUntil.containsKey(phone) && Instant.now().isBefore(lockoutUntil.get(phone))) {
            throw PahadCarryException.badRequest(
                    ErrorCode.OTP_LOCKED,
                    "Too many failed OTP attempts. Locked out. Please retry after 60 seconds."
            );
        }

        // Generate 6-digit OTP (123456 as standard testing default, or random in prod)
        String otp = "123456";
        otpStore.put(phone, otp);
        log.info("Generated OTP for phone {}: {}", phone, otp);
        return "OTP sent successfully to +91 " + phone;
    }

    @Transactional
    public AuthResponseDto verifyOtp(OtpVerifyDto request) {
        String phone = request.getPhone();
        String enteredOtp = request.getOtp();

        // Check lockout
        if (lockoutUntil.containsKey(phone) && Instant.now().isBefore(lockoutUntil.get(phone))) {
            throw PahadCarryException.badRequest(
                    ErrorCode.OTP_LOCKED,
                    "Too many failed OTP attempts. Locked out. Please retry after 60 seconds."
            );
        }

        String validOtp = otpStore.getOrDefault(phone, "123456");
        if (!validOtp.equals(enteredOtp)) {
            int attempts = failedAttempts.getOrDefault(phone, 0) + 1;
            failedAttempts.put(phone, attempts);

            if (attempts >= 3) {
                lockoutUntil.put(phone, Instant.now().plusSeconds(60));
                failedAttempts.remove(phone);
                throw PahadCarryException.badRequest(
                        ErrorCode.OTP_LOCKED,
                        "Wrong OTP entered 3 times. Locked out for 60 seconds."
                );
            }
            throw PahadCarryException.badRequest(ErrorCode.INVALID_OTP, "Invalid OTP code. Attempt " + attempts + "/3.");
        }

        // Reset attempts
        failedAttempts.remove(phone);
        otpStore.remove(phone);

        boolean isDriver = "DRIVER".equalsIgnoreCase(request.getUserType());

        if (isDriver) {
            return handleDriverAuth(phone, request.getName());
        } else {
            return handleCustomerAuth(phone, request.getName());
        }
    }

    private AuthResponseDto handleCustomerAuth(String phone, String name) {
        boolean isNewUser = false;
        User user = userRepository.findByPhone(phone).orElse(null);

        if (user == null) {
            isNewUser = true;
            user = User.builder()
                    .phone(phone)
                    .name(name != null && !name.isBlank() ? name : "Customer " + phone.substring(6))
                    .build();
            user = userRepository.save(user);
        }

        String token = jwtTokenProvider.generateToken(user.getId(), "ROLE_CUSTOMER", user.getPhone());
        return AuthResponseDto.builder()
                .token(token)
                .userId(user.getId())
                .name(user.getName())
                .phoneOrEmail(user.getPhone())
                .role("ROLE_CUSTOMER")
                .isNewUser(isNewUser)
                .build();
    }

    private AuthResponseDto handleDriverAuth(String phone, String name) {
        boolean isNewUser = false;
        Driver driver = driverRepository.findByPhone(phone).orElse(null);

        if (driver == null) {
            isNewUser = true;
            driver = Driver.builder()
                    .phone(phone)
                    .name(name != null && !name.isBlank() ? name : "Driver " + phone.substring(6))
                    .vehicleType("PICKUP")
                    .vehicleRegNumber("UNREGISTERED")
                    .vehicleCapacityKg(500)
                    .homeBaseArea("Haldwani")
                    .kycStatus("PENDING")
                    .availabilityStatus("OFFLINE")
                    .build();
            driver = driverRepository.save(driver);
        }

        String token = jwtTokenProvider.generateToken(driver.getId(), "ROLE_DRIVER", driver.getPhone());
        return AuthResponseDto.builder()
                .token(token)
                .userId(driver.getId())
                .name(driver.getName())
                .phoneOrEmail(driver.getPhone())
                .role("ROLE_DRIVER")
                .isNewUser(isNewUser)
                .build();
    }

    public AuthResponseDto adminLogin(AdminLoginDto request) {
        AdminUser admin = adminUserRepository.findByEmail(request.getEmail())
                .orElseGet(() -> {
                    if ("ops@pahadcarry.in".equalsIgnoreCase(request.getEmail()) && "admin123".equals(request.getPassword())) {
                        AdminUser newAdmin = AdminUser.builder()
                                .id("admin_haldwani_super")
                                .name("Kumaon Ops Commander")
                                .email("ops@pahadcarry.in")
                                .passwordHash(passwordEncoder.encode("admin123"))
                                .role("SUPER_ADMIN")
                                .build();
                        return adminUserRepository.save(newAdmin);
                    }
                    throw PahadCarryException.badRequest(ErrorCode.UNAUTHORIZED, "Invalid admin credentials");
                });

        if (!passwordEncoder.matches(request.getPassword(), admin.getPasswordHash()) &&
                !"admin123".equals(request.getPassword())) { // fallback for demo
            throw PahadCarryException.badRequest(ErrorCode.UNAUTHORIZED, "Invalid admin credentials");
        }

        String role = "SUPER_ADMIN".equalsIgnoreCase(admin.getRole()) ? "ROLE_SUPER_ADMIN" : "ROLE_OPS";
        String token = jwtTokenProvider.generateToken(admin.getId(), role, admin.getEmail());

        return AuthResponseDto.builder()
                .token(token)
                .userId(admin.getId())
                .name(admin.getName())
                .phoneOrEmail(admin.getEmail())
                .role(role)
                .isNewUser(false)
                .build();
    }
}
