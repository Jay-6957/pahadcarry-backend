package com.pahadcarry.driver.repository;

import com.pahadcarry.driver.model.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, String> {
    Optional<Driver> findByPhone(String phone);
    List<Driver> findByKycStatus(String kycStatus);
    List<Driver> findByKycStatusAndAvailabilityStatus(String kycStatus, String availabilityStatus);
}
