package com.pahadcarry.auth;

import com.pahadcarry.auth.dto.AdminLoginDto;
import com.pahadcarry.auth.dto.AuthResponseDto;
import com.pahadcarry.auth.dto.OtpRequestDto;
import com.pahadcarry.auth.dto.OtpVerifyDto;
import com.pahadcarry.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/otp/request")
    public ResponseEntity<ApiResponse<Map<String, Object>>> requestOtp(@Valid @RequestBody OtpRequestDto request) {
        String message = authService.requestOtp(request);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "message", message,
                "expiresInSeconds", 300
        )));
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<ApiResponse<AuthResponseDto>> verifyOtp(@Valid @RequestBody OtpVerifyDto request) {
        AuthResponseDto response = authService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/admin/login")
    public ResponseEntity<ApiResponse<AuthResponseDto>> adminLogin(@Valid @RequestBody AdminLoginDto request) {
        AuthResponseDto response = authService.adminLogin(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
