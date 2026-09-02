package com.pahadcarry.driver;

import com.pahadcarry.batch.model.Batch;
import com.pahadcarry.batch.model.BatchStop;
import com.pahadcarry.batch.repository.BatchRepository;
import com.pahadcarry.batch.repository.BatchStopRepository;
import com.pahadcarry.common.ApiResponse;
import com.pahadcarry.common.ErrorCode;
import com.pahadcarry.common.PahadCarryException;
import com.pahadcarry.config.service.DriverPayoutService;
import com.pahadcarry.driver.model.Driver;
import com.pahadcarry.driver.repository.DriverRepository;
import com.pahadcarry.order.model.Order;
import com.pahadcarry.order.repository.OrderRepository;
import com.pahadcarry.notification.service.NotificationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/drivers/me")
@RequiredArgsConstructor
public class DriverBatchController {

    private final BatchRepository batchRepository;
    private final BatchStopRepository batchStopRepository;
    private final OrderRepository orderRepository;
    private final DriverRepository driverRepository;
    private final DriverPayoutService payoutService;
    private final NotificationService notificationService;

    @GetMapping("/batches/current")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentBatch(Authentication auth) {
        String driverId = (String) auth.getPrincipal();
        Batch batch = batchRepository.findByDriverIdAndStatus(driverId, "ASSIGNED")
                .or(() -> batchRepository.findByDriverIdAndStatus(driverId, "IN_PROGRESS"))
                .orElse(null);

        if (batch == null) {
            return ResponseEntity.ok(ApiResponse.ok(null));
        }

        List<BatchStop> stops = batchStopRepository.findByBatchIdOrderBySequenceAsc(batch.getId());
        enrichStopsWithOrders(stops);

        Map<String, Object> result = new HashMap<>();
        result.put("batch", batch);
        result.put("stops", stops);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/batches/history")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getBatchHistory(Authentication auth) {
        String driverId = (String) auth.getPrincipal();
        List<Batch> batches = batchRepository.findByDriverIdOrderByCreatedAtDesc(driverId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Batch b : batches) {
            List<BatchStop> stops = batchStopRepository.findByBatchIdOrderBySequenceAsc(b.getId());
            enrichStopsWithOrders(stops);
            Map<String, Object> item = new HashMap<>();
            item.put("batch", b);
            item.put("id", b.getId());
            item.put("areaCluster", b.getAreaCluster());
            item.put("totalWeightKg", b.getTotalWeightKg());
            item.put("driverPayout", b.getDriverPayout());
            item.put("status", b.getStatus());
            item.put("assignedAt", b.getAssignedAt());
            item.put("completedAt", b.getCompletedAt());
            item.put("stops", stops);
            result.add(item);
        }

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    private void enrichStopsWithOrders(List<BatchStop> stops) {
        if (stops == null || stops.isEmpty()) return;
        List<String> orderIds = stops.stream()
                .map(BatchStop::getOrderId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();
        if (orderIds.isEmpty()) return;

        Map<String, Order> orderMap = orderRepository.findAllById(orderIds).stream()
                .collect(java.util.stream.Collectors.toMap(Order::getId, o -> o));

        for (BatchStop stop : stops) {
            if (stop.getOrderId() != null) {
                stop.setOrder(orderMap.get(stop.getOrderId()));
            }
        }
    }

    @GetMapping("/earnings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEarnings(Authentication auth) {
        String driverId = (String) auth.getPrincipal();
        List<Batch> batches = batchRepository.findByDriverIdOrderByCreatedAtDesc(driverId);

        BigDecimal totalPayout = batches.stream()
                .filter(b -> "COMPLETED".equals(b.getStatus()) && b.getDriverPayout() != null)
                .map(Batch::getDriverPayout)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long completedTrips = batches.stream()
                .filter(b -> "COMPLETED".equals(b.getStatus()))
                .count();

        Map<String, Object> stats = Map.of(
                "totalPayout", totalPayout,
                "completedTrips", completedTrips,
                "assignedTrips", batches.stream().filter(b -> "ASSIGNED".equals(b.getStatus()) || "IN_PROGRESS".equals(b.getStatus())).count(),
                "allBatches", batches
        );
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    @GetMapping("/orders/completed")
    public ResponseEntity<ApiResponse<List<Order>>> getCompletedOrders(Authentication auth) {
        String driverId = (String) auth.getPrincipal();
        List<Batch> driverBatches = batchRepository.findByDriverIdOrderByCreatedAtDesc(driverId);
        List<String> batchIds = driverBatches.stream().map(Batch::getId).toList();
        if (batchIds.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok(List.of()));
        }
        List<Order> deliveredOrders = orderRepository.findAll().stream()
                .filter(o -> o.getBatchId() != null && batchIds.contains(o.getBatchId()) && "DELIVERED".equals(o.getStatus()))
                .sorted((a, b) -> (b.getDeliveredAt() != null && a.getDeliveredAt() != null) ? b.getDeliveredAt().compareTo(a.getDeliveredAt()) : 0)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(deliveredOrders));
    }

    @PostMapping("/stops/{stopId}/complete")
    @Transactional
    public ResponseEntity<ApiResponse<BatchStop>> completeStop(
            Authentication auth,
            @PathVariable String stopId,
            @RequestBody(required = false) StopCompleteDto body) {
        String driverId = (String) auth.getPrincipal();
        BatchStop stop = batchStopRepository.findById(stopId)
                .orElseThrow(() -> PahadCarryException.notFound(ErrorCode.NOT_FOUND, "Stop not found"));

        Batch batch = batchRepository.findById(stop.getBatchId())
                .orElseThrow(() -> PahadCarryException.notFound(ErrorCode.NOT_FOUND, "Batch not found"));

        if (!driverId.equals(batch.getDriverId())) {
            throw PahadCarryException.forbidden(ErrorCode.FORBIDDEN, "Not your assigned batch");
        }

        stop.setStatus("COMPLETED");
        stop.setCompletedAt(Instant.now());
        if (body != null) {
            if (body.getProofPhotoUrl() != null) stop.setProofPhotoUrl(body.getProofPhotoUrl());
            if (body.getCashCollected() != null) stop.setCashCollected(body.getCashCollected());
        }
        batchStopRepository.save(stop);

        // Update corresponding order status
        if (stop.getOrderId() != null) {
            orderRepository.findById(stop.getOrderId()).ifPresent(order -> {
                if ("PICKUP".equals(stop.getStopType())) {
                    order.setStatus("PICKED_UP");
                    order.setPickedUpAt(Instant.now());
                } else if ("DROP".equals(stop.getStopType())) {
                    order.setStatus("DELIVERED");
                    order.setDeliveredAt(Instant.now());
                    if (stop.getCashCollected() != null && stop.getCashCollected().compareTo(BigDecimal.ZERO) > 0) {
                        order.setPaymentStatus("PAID");
                    }
                }
                orderRepository.save(order);
            });
        }

        if ("ASSIGNED".equals(batch.getStatus())) {
            batch.setStatus("IN_PROGRESS");
            batchRepository.save(batch);
        }

        // Find next pending stop for telemetry
        List<BatchStop> allStops = batchStopRepository.findByBatchIdOrderBySequenceAsc(batch.getId());
        BatchStop nextPending = allStops.stream().filter(s -> !"COMPLETED".equals(s.getStatus()) && !"CANCELLED".equals(s.getStatus())).findFirst().orElse(null);

        // Update Driver telemetry
        driverRepository.findById(driverId).ifPresent(driver -> {
            driver.setCurrentLat(stop.getLat());
            driver.setCurrentLng(stop.getLng());
            driver.setCurrentLocationName(stop.getAddress());
            String actionDetail = stop.getStopType() + " at " + stop.getAddress() + (stop.getCashCollected() != null && stop.getCashCollected().compareTo(BigDecimal.ZERO) > 0 ? " (Collected COD ₹" + stop.getCashCollected() + ")" : "");
            driver.setLastActionDescription("Completed " + actionDetail);
            driver.setLastActionTimestamp(Instant.now());
            if (nextPending != null) {
                driver.setNextActionDescription("En route to Stop #" + nextPending.getSequence() + " (" + nextPending.getStopType() + ") at " + nextPending.getAddress());
            } else {
                driver.setNextActionDescription("All batch stops completed · Ready to finalize payout");
            }
            driverRepository.save(driver);
        });

        return ResponseEntity.ok(ApiResponse.ok(stop));
    }

    @PostMapping("/batches/{batchId}/complete")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> completeBatch(
            Authentication auth,
            @PathVariable String batchId) {
        String driverId = (String) auth.getPrincipal();
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> PahadCarryException.notFound(ErrorCode.NOT_FOUND, "Batch not found"));

        if (!driverId.equals(batch.getDriverId())) {
            throw PahadCarryException.forbidden(ErrorCode.FORBIDDEN, "Not your assigned batch");
        }

        BigDecimal payout = payoutService.calculateAndRecordPayout(batchId);

        Driver driver = driverRepository.findById(driverId).orElse(null);
        if (driver != null) {
            driver.setAvailabilityStatus("ONLINE");
            driver.setLastActionDescription("Finalized batch #" + batchId.substring(0, Math.min(8, batchId.length())) + " (Payout: ₹" + payout + ")");
            driver.setLastActionTimestamp(Instant.now());
            driver.setNextActionDescription("Online & available for next corridor batch");
            driverRepository.save(driver);
        }

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "batchId", batchId,
                "status", "COMPLETED",
                "driverPayout", payout,
                "payoutStrategy", batch.getPayoutStrategyUsed()
        )));
    }

    @Data
    public static class StopCompleteDto {
        private String proofPhotoUrl;
        private BigDecimal cashCollected;
    }
}
