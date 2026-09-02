package com.pahadcarry.batch.repository;

import com.pahadcarry.batch.model.Batch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BatchRepository extends JpaRepository<Batch, String> {
    List<Batch> findByStatus(String status);
    List<Batch> findByDriverIdOrderByCreatedAtDesc(String driverId);
    Optional<Batch> findByDriverIdAndStatus(String driverId, String status);
}
