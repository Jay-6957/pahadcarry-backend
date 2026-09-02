package com.pahadcarry.config.repository;

import com.pahadcarry.config.model.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, Integer> {
    default SystemConfig getConfig() {
        return findById(1).orElseGet(SystemConfig::new);
    }
}
