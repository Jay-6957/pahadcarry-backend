package com.pahadcarry.customer.service;

import com.pahadcarry.common.ErrorCode;
import com.pahadcarry.common.PahadCarryException;
import com.pahadcarry.customer.dto.AddressDto;
import com.pahadcarry.customer.model.Address;
import com.pahadcarry.customer.model.User;
import com.pahadcarry.customer.repository.AddressRepository;
import com.pahadcarry.customer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;

    public User getCustomerProfile(String customerId) {
        return userRepository.findById(customerId)
                .orElseThrow(() -> PahadCarryException.notFound(ErrorCode.NOT_FOUND, "Customer not found"));
    }

    public List<AddressDto> getSavedAddresses(String customerId) {
        return addressRepository.findByUserIdOrderByCreatedAtDesc(customerId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public AddressDto saveAddress(String customerId, AddressDto dto) {
        // verify user exists
        if (!userRepository.existsById(customerId)) {
            throw PahadCarryException.notFound(ErrorCode.NOT_FOUND, "Customer not found");
        }

        Address address = Address.builder()
                .userId(customerId)
                .label(dto.getLabel())
                .lat(dto.getLat())
                .lng(dto.getLng())
                .villageOrTown(dto.getVillageOrTown())
                .landmark(dto.getLandmark())
                .build();

        Address saved = addressRepository.save(address);
        return mapToDto(saved);
    }

    @Transactional
    public AddressDto updateAddress(String customerId, String addressId, AddressDto dto) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> PahadCarryException.notFound(ErrorCode.NOT_FOUND, "Address not found"));

        if (!address.getUserId().equals(customerId)) {
            throw PahadCarryException.forbidden(ErrorCode.FORBIDDEN, "Unauthorized access to address");
        }

        address.setLabel(dto.getLabel());
        address.setLat(dto.getLat());
        address.setLng(dto.getLng());
        address.setVillageOrTown(dto.getVillageOrTown());
        address.setLandmark(dto.getLandmark());

        return mapToDto(addressRepository.save(address));
    }

    @Transactional
    public void deleteAddress(String customerId, String addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> PahadCarryException.notFound(ErrorCode.NOT_FOUND, "Address not found"));

        if (!address.getUserId().equals(customerId)) {
            throw PahadCarryException.forbidden(ErrorCode.FORBIDDEN, "Unauthorized access to address");
        }

        addressRepository.delete(address);
    }

    private AddressDto mapToDto(Address address) {
        return AddressDto.builder()
                .id(address.getId())
                .label(address.getLabel())
                .lat(address.getLat())
                .lng(address.getLng())
                .villageOrTown(address.getVillageOrTown())
                .landmark(address.getLandmark())
                .build();
    }
}
