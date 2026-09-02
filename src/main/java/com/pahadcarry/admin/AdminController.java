package com.pahadcarry.admin;

import com.pahadcarry.admin.repository.AdminUserRepository;
import com.pahadcarry.batch.model.Batch;
import com.pahadcarry.batch.model.BatchStop;
import com.pahadcarry.batch.repository.BatchRepository;
import com.pahadcarry.batch.repository.BatchStopRepository;
import com.pahadcarry.batch.service.BatchingEngine;
import com.pahadcarry.common.ApiResponse;
import com.pahadcarry.common.DriverEligibilityGuard;
import com.pahadcarry.common.ErrorCode;
import com.pahadcarry.common.PahadCarryException;
import com.pahadcarry.config.model.SystemConfig;
import com.pahadcarry.config.repository.SystemConfigRepository;
import com.pahadcarry.config.service.DriverPayoutService;
import com.pahadcarry.driver.model.Driver;
import com.pahadcarry.driver.repository.DriverRepository;
import com.pahadcarry.order.model.Order;
import com.pahadcarry.order.repository.OrderRepository;
import com.pahadcarry.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_OPS','ROLE_SUPER_ADMIN')")
public class AdminController {

    private final DriverRepository driverRepository;
    private final BatchRepository batchRepository;
    private final BatchStopRepository batchStopRepository;
    private final OrderRepository orderRepository;
    private final SystemConfigRepository configRepository;
    private final BatchingEngine batchingEngine;
    private final DriverPayoutService payoutService;
    private final DriverEligibilityGuard eligibilityGuard;
    private final com.pahadcarry.order.service.OrderService orderService;
    private final com.pahadcarry.order.service.OrderCancellationService cancellationService;
    private final NotificationService notificationService;

    // ---- Driver & KYC Management ----

    @GetMapping("/drivers")
    public ResponseEntity<ApiResponse<List<Driver>>> getAllDrivers() {
        return ResponseEntity.ok(ApiResponse.ok(driverRepository.findAll()));
    }

    @GetMapping("/drivers/eligible")
    public ResponseEntity<ApiResponse<List<Driver>>> getEligibleDrivers() {
        return ResponseEntity.ok(ApiResponse.ok(
                driverRepository.findByKycStatusAndAvailabilityStatus("APPROVED", "ONLINE")
        ));
    }

    @GetMapping("/drivers/pending-kyc")
    public ResponseEntity<ApiResponse<List<Driver>>> getPendingKyc() {
        return ResponseEntity.ok(ApiResponse.ok(driverRepository.findByKycStatus("PENDING")));
    }

    @PostMapping("/drivers/{driverId}/kyc/approve")
    public ResponseEntity<ApiResponse<Driver>> approveKyc(
            @PathVariable String driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> PahadCarryException.notFound(ErrorCode.NOT_FOUND, "Driver not found"));
        driver.setKycStatus("APPROVED");
        driver.setReviewedAt(Instant.now());
        Driver saved = driverRepository.save(driver);
        notificationService.notifyDriver(driverId, "KYC approved", "Your driver account has been approved. You can now go online.", driverId);
        return ResponseEntity.ok(ApiResponse.ok(saved));
    }

    @PostMapping("/drivers/{driverId}/kyc/reject")
    public ResponseEntity<ApiResponse<Driver>> rejectKyc(
            @PathVariable String driverId,
            @RequestParam String reason) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> PahadCarryException.notFound(ErrorCode.NOT_FOUND, "Driver not found"));
        driver.setKycStatus("REJECTED");
        driver.setRejectionReason(reason);
        driver.setReviewedAt(Instant.now());
        Driver saved = driverRepository.save(driver);
        notificationService.notifyDriver(driverId, "KYC update", "Your KYC application was rejected: " + reason, driverId);
        return ResponseEntity.ok(ApiResponse.ok(saved));
    }

    // ---- Batch Management ----

    @GetMapping("/batches")
    public ResponseEntity<ApiResponse<List<Batch>>> getBatches(@RequestParam(defaultValue = "FORMING") String status) {
        return ResponseEntity.ok(ApiResponse.ok(batchRepository.findByStatus(status)));
    }

    @GetMapping("/batches/all")
    public ResponseEntity<ApiResponse<List<Batch>>> getAllBatches() {
        return ResponseEntity.ok(ApiResponse.ok(batchRepository.findAll()));
    }

    @GetMapping("/orders/unassigned")
    public ResponseEntity<ApiResponse<List<Order>>> getUnassignedOrders() {
        return ResponseEntity.ok(ApiResponse.ok(orderRepository.findByStatus("PLACED")));
    }

    @PostMapping("/batches/form-now")
    public ResponseEntity<ApiResponse<Batch>> forceFormBatch() {
        List<Order> openOrders = orderRepository.findByStatus("PLACED");
        if (openOrders.isEmpty()) {
            throw PahadCarryException.badRequest(ErrorCode.BAD_REQUEST, "No open orders to batch");
        }
        Batch batch = batchingEngine.formBatch(openOrders);
        return ResponseEntity.ok(ApiResponse.ok(batch));
    }

    @PostMapping("/batches/{batchId}/assign")
    public ResponseEntity<ApiResponse<Batch>> assignDriver(
            @PathVariable String batchId,
            @RequestParam String driverId) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> PahadCarryException.notFound(ErrorCode.NOT_FOUND, "Batch not found"));
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> PahadCarryException.notFound(ErrorCode.NOT_FOUND, "Driver not found"));

        eligibilityGuard.validateDriverEligibleForAssignment(driver.getKycStatus(), driver.getAvailabilityStatus());

        batch.setDriverId(driverId);
        batch.setStatus("ASSIGNED");
        batch.setAssignedAt(Instant.now());
        driver.setAvailabilityStatus("ON_TRIP");
        driverRepository.save(driver);
        notificationService.notifyDriver(driverId, "Batch assigned", "Batch " + batchId + " has been assigned to you.", batchId);
        return ResponseEntity.ok(ApiResponse.ok(batchRepository.save(batch)));
    }

    @GetMapping("/batches/{batchId}/stops")
    public ResponseEntity<ApiResponse<List<BatchStop>>> getBatchStops(@PathVariable String batchId) {
        List<BatchStop> stops = batchStopRepository.findByBatchIdOrderBySequenceAsc(batchId);
        List<String> orderIds = stops.stream()
                .map(BatchStop::getOrderId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();
        if (!orderIds.isEmpty()) {
            Map<String, Order> orderMap = orderRepository.findAllById(orderIds).stream()
                    .collect(java.util.stream.Collectors.toMap(Order::getId, o -> o));
            for (BatchStop stop : stops) {
                if (stop.getOrderId() != null) {
                    stop.setOrder(orderMap.get(stop.getOrderId()));
                }
            }
        }
        return ResponseEntity.ok(ApiResponse.ok(stops));
    }

    // ---- Config Management ----

    @GetMapping("/config")
    public ResponseEntity<ApiResponse<SystemConfig>> getConfig() {
        return ResponseEntity.ok(ApiResponse.ok(configRepository.getConfig()));
    }

    @PutMapping("/config")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SystemConfig>> updateConfig(@RequestBody SystemConfig config) {
        config.setId(1);
        config.setUpdatedAt(Instant.now());
        return ResponseEntity.ok(ApiResponse.ok(configRepository.save(config)));
    }

    // ---- Daily Summary ----

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSummary() {
        List<Order> allOrders = orderRepository.findAll();
        List<Driver> allDrivers = driverRepository.findAll();
        List<Batch> allBatches = batchRepository.findAll();

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalOrders", allOrders.size());
        summary.put("placedOrders", allOrders.stream().filter(o -> "PLACED".equals(o.getStatus())).count());
        summary.put("deliveredOrders", allOrders.stream().filter(o -> "DELIVERED".equals(o.getStatus())).count());
        summary.put("cancelledOrders", allOrders.stream().filter(o -> "CANCELLED".equals(o.getStatus())).count());
        summary.put("activeDrivers", allDrivers.stream().filter(d -> "ONLINE".equals(d.getAvailabilityStatus())).count());
        summary.put("totalDrivers", allDrivers.size());
        summary.put("activeBatches", allBatches.stream().filter(b -> "ASSIGNED".equals(b.getStatus()) || "IN_PROGRESS".equals(b.getStatus())).count());
        summary.put("completedBatches", allBatches.stream().filter(b -> "COMPLETED".equals(b.getStatus())).count());
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    // ---- Financials & Invoices ----

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<List<Order>>> getAllOrders() {
        return ResponseEntity.ok(ApiResponse.ok(orderRepository.findAll()));
    }

    @GetMapping("/orders/{orderId}/cancellation-quote")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCancellationQuote(
            @PathVariable String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> PahadCarryException.notFound(ErrorCode.NOT_FOUND, "Order not found"));

        Map<String, Object> quote = new HashMap<>();
        quote.put("orderId", orderId);
        quote.put("currentStatus", order.getStatus());
        quote.put("priceEstimate", order.getPriceEstimate());
        quote.put("cancellationFee", BigDecimal.ZERO);
        quote.put("isFreeCancellation", true);
        quote.put("strategyTier", "ADMIN_OPS_OVERRIDE_FREE");
        quote.put("explanation", "Admin / Ops Cancellation Override — 100% Free. No penalty or cancellation charges applied.");
        quote.put("finalPayableAmount", BigDecimal.ZERO);

        return ResponseEntity.ok(ApiResponse.ok(quote));
    }

    @PostMapping("/orders/{orderId}/cancel")
    public ResponseEntity<ApiResponse<Order>> cancelOrder(
            @PathVariable String orderId,
            @RequestParam(required = false) String reason) {
        Order cancelled = cancellationService.cancelOrderByAdmin(orderId, reason);
        return ResponseEntity.ok(ApiResponse.ok(cancelled));
    }

    @GetMapping("/drivers/{driverId}")
    public ResponseEntity<ApiResponse<Driver>> getDriverById(@PathVariable String driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> PahadCarryException.notFound(ErrorCode.NOT_FOUND, "Driver not found"));
        return ResponseEntity.ok(ApiResponse.ok(driver));
    }

    @GetMapping("/financials")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFinancials() {
        List<Order> allOrders = orderRepository.findAll();
        List<Batch> allBatches = batchRepository.findAll();

        BigDecimal grossRevenue = allOrders.stream()
                .filter(o -> !"CANCELLED".equals(o.getStatus()))
                .map(Order::getPriceEstimate)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal driverPayouts = allBatches.stream()
                .filter(b -> b.getDriverPayout() != null)
                .map(Batch::getDriverPayout)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netMargin = grossRevenue.subtract(driverPayouts);

        BigDecimal codCollected = allOrders.stream()
                .filter(o -> "PAID".equals(o.getPaymentStatus()))
                .map(Order::getPriceEstimate)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal codPending = grossRevenue.subtract(codCollected);

        Map<String, Object> result = new HashMap<>();
        result.put("grossRevenue", grossRevenue);
        result.put("driverPayouts", driverPayouts);
        result.put("netPlatformMargin", netMargin);
        result.put("codCollected", codCollected);
        result.put("codPending", codPending);
        result.put("totalOrders", allOrders.size());
        result.put("completedBatches", allBatches.stream().filter(b -> "COMPLETED".equals(b.getStatus())).count());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
