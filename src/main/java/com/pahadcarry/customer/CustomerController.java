package com.pahadcarry.customer;

import com.pahadcarry.common.ApiResponse;
import com.pahadcarry.customer.dto.AddressDto;
import com.pahadcarry.customer.model.User;
import com.pahadcarry.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public ResponseEntity<ApiResponse<User>> getProfile(Authentication auth) {
        String customerId = (String) auth.getPrincipal();
        User profile = customerService.getCustomerProfile(customerId);
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    @GetMapping("/addresses")
    public ResponseEntity<ApiResponse<List<AddressDto>>> getAddresses(Authentication auth) {
        String customerId = (String) auth.getPrincipal();
        List<AddressDto> addresses = customerService.getSavedAddresses(customerId);
        return ResponseEntity.ok(ApiResponse.ok(addresses));
    }

    @PostMapping("/addresses")
    public ResponseEntity<ApiResponse<AddressDto>> createAddress(
            Authentication auth,
            @Valid @RequestBody AddressDto dto) {
        String customerId = (String) auth.getPrincipal();
        AddressDto saved = customerService.saveAddress(customerId, dto);
        return ResponseEntity.ok(ApiResponse.ok(saved));
    }

    @PutMapping("/addresses/{id}")
    public ResponseEntity<ApiResponse<AddressDto>> updateAddress(
            Authentication auth,
            @PathVariable("id") String addressId,
            @Valid @RequestBody AddressDto dto) {
        String customerId = (String) auth.getPrincipal();
        AddressDto updated = customerService.updateAddress(customerId, addressId, dto);
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

    @DeleteMapping("/addresses/{id}")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteAddress(
            Authentication auth,
            @PathVariable("id") String addressId) {
        String customerId = (String) auth.getPrincipal();
        customerService.deleteAddress(customerId, addressId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Address deleted successfully")));
    }
}
