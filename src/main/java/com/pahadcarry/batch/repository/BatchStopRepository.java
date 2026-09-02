package com.pahadcarry.batch.repository;

import com.pahadcarry.batch.model.BatchStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BatchStopRepository extends JpaRepository<BatchStop, String> {
    List<BatchStop> findByBatchIdOrderBySequenceAsc(String batchId);
    List<BatchStop> findByOrderId(String orderId);
}
