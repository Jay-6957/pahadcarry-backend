package com.pahadcarry.batch.repository;

import com.pahadcarry.batch.model.HubCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HubCenterRepository extends JpaRepository<HubCenter, String> {
}
