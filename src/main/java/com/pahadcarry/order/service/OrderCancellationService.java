package com.pahadcarry.order.service;

import com.pahadcarry.batch.model.BatchStop;
import com.pahadcarry.batch.repository.BatchStopRepository;
import com.pahadcarry.common.ErrorCode;
import com.pahadcarry.common.PahadCarryException;
import com.pahadcarry.config.model.SystemConfig;
import com.pahadcarry.config.repository.SystemConfigRepository;
import com.pahadcarry.order.model.Order;
import com.pahadcarry.order.repository.OrderRepository;
import com.pahadcarry.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCancellationService {

    private final OrderRepository orderRepository;
    private final BatchStopRepository batchStopRepository;
    private final SystemConfigRepository configRepository;
    private final NotificationService notificationService;

    /**
     * Calculate dynamic cancellation quote based on current order status & system strategy.
     */
    public Map<String, Object> getCancellationQuote(String orderId, String customerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> PahadCarryException.notFound(ErrorCode.NOT_FOUND, "Order not found"));

        if (customerId != null && !order.getCustomerId().equals(customerId)) {
            throw PahadCarryException.forbidden(ErrorCode.FORBIDDEN, "Not your order");
        }

        if ("DELIVERED".equals(order.getStatus())) {
            throw PahadCarryException.badRequest(ErrorCode.ORDER_CANNOT_BE_CANCELLED, "Delivered orders cannot be cancelled");
        }
        if ("CANCELLED".equals(order.getStatus())) {
            throw PahadCarryException.badRequest(ErrorCode.ORDER_CANNOT_BE_CANCELLED, "Order is already cancelled");
        }

        BigDecimal estimate = order.getPriceEstimate() != null ? order.getPriceEstimate() : BigDecimal.ZERO;
        BigDecimal fee = calculateDynamicFee(order.getStatus(), estimate);
        boolean isFree = fee.compareTo(BigDecimal.ZERO) == 0;

        String strategyTier;
        String explanation;

        switch (order.getStatus()) {
            case "PLACED", "BATCHED" -> {
                strategyTier = "PRE_PICKUP_FREE";
                explanation = "Free cancellation — your order has not been picked up yet.";
            }
            case "PICKED_UP" -> {
                strategyTier = "POST_PICKUP_COMPENSATION";
                explanation = "Goods have already been collected by the driver. A dynamic abort fee (35% or min ₹100) covers driver mobilization.";
            }
            case "AT_HUB" -> {
                strategyTier = "HUB_IN_TRANSIT_FEE";
                explanation = "Goods are at Haldwani Central Hub. A 50% handling and return routing fee applies.";
            }
            case "OUT_FOR_DELIVERY" -> {
                strategyTier = "LAST_MILE_ABORT_FEE";
                explanation = "Driver is on the final mountain delivery route. A 75% abort fee applies.";
            }
            default -> {
                strategyTier = "STANDARD";
                explanation = "Standard cancellation terms apply.";
            }
        }

        Map<String, Object> quote = new HashMap<>();
        quote.put("orderId", orderId);
        quote.put("currentStatus", order.getStatus());
        quote.put("priceEstimate", estimate);
        quote.put("cancellationFee", fee);
        quote.put("isFreeCancellation", isFree);
        quote.put("strategyTier", strategyTier);
        quote.put("explanation", explanation);
        quote.put("finalPayableAmount", fee);

        return quote;
    }

    /**
     * Cancel the order and apply dynamic cancellation charge.
     */
    @Transactional
    public Order cancelOrder(String orderId, String customerId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> PahadCarryException.notFound(ErrorCode.NOT_FOUND, "Order not found"));

        if (customerId != null && !order.getCustomerId().equals(customerId)) {
            throw PahadCarryException.forbidden(ErrorCode.FORBIDDEN, "Not your order");
        }

        if ("DELIVERED".equals(order.getStatus())) {
            throw PahadCarryException.conflict(ErrorCode.ORDER_CANNOT_BE_CANCELLED, "Delivered orders cannot be cancelled");
        }
        if ("CANCELLED".equals(order.getStatus())) {
            throw PahadCarryException.conflict(ErrorCode.ORDER_CANNOT_BE_CANCELLED, "Order is already cancelled");
        }

        BigDecimal estimate = order.getPriceEstimate() != null ? order.getPriceEstimate() : BigDecimal.ZERO;
        BigDecimal cancellationFee = calculateDynamicFee(order.getStatus(), estimate);

        order.setStatus("CANCELLED");
        order.setCancelledAt(Instant.now());
        order.setCancellationFee(cancellationFee);
        order.setFinalPrice(cancellationFee);
        order.setCancellationReason(reason != null && !reason.isBlank() ? reason : "Cancelled by user");

        // If cancellation fee is 0, mark payment status CANCELLED or keep COD_PENDING if fee > 0
        if (cancellationFee.compareTo(BigDecimal.ZERO) == 0) {
            order.setPaymentStatus("CANCELLED");
        } else {
            order.setPaymentStatus("CANCELLATION_FEE_DUE");
        }

        // Cancel associated batch stops if any
        if (order.getBatchId() != null) {
            List<BatchStop> stops = batchStopRepository.findByBatchIdOrderBySequenceAsc(order.getBatchId());
            for (BatchStop stop : stops) {
                if (orderId.equals(stop.getOrderId()) && !"COMPLETED".equals(stop.getStatus())) {
                    stop.setStatus("CANCELLED");
                    batchStopRepository.save(stop);
                }
            }
        }

        log.info("Order {} cancelled. Previous status: {}, Cancellation Fee: ₹{}",
                orderId, order.getStatus(), cancellationFee);

        Order saved = orderRepository.save(order);
        notificationService.notifyUser(saved.getCustomerId(), "Order cancelled", "Order " + orderId + " has been cancelled.", orderId);
        return saved;
    }

    /**
     * Cancel the order by Admin/Ops with ZERO penalty / cancellation fee.
     */
    @Transactional
    public Order cancelOrderByAdmin(String orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> PahadCarryException.notFound(ErrorCode.NOT_FOUND, "Order not found"));

        if ("DELIVERED".equals(order.getStatus())) {
            throw PahadCarryException.conflict(ErrorCode.ORDER_CANNOT_BE_CANCELLED, "Delivered orders cannot be cancelled");
        }
        if ("CANCELLED".equals(order.getStatus())) {
            throw PahadCarryException.conflict(ErrorCode.ORDER_CANNOT_BE_CANCELLED, "Order is already cancelled");
        }

        order.setStatus("CANCELLED");
        order.setCancelledAt(Instant.now());
        order.setCancellationFee(BigDecimal.ZERO);
        order.setFinalPrice(BigDecimal.ZERO);
        order.setPaymentStatus("CANCELLED");
        order.setCancellationReason(reason != null && !reason.isBlank() ? reason : "Cancelled by Ops / Admin (Zero Penalty)");

        // Cancel associated batch stops if any
        if (order.getBatchId() != null) {
            List<BatchStop> stops = batchStopRepository.findByBatchIdOrderBySequenceAsc(order.getBatchId());
            for (BatchStop stop : stops) {
                if (orderId.equals(stop.getOrderId()) && !"COMPLETED".equals(stop.getStatus())) {
                    stop.setStatus("CANCELLED");
                    batchStopRepository.save(stop);
                }
            }
        }

        log.info("Order {} cancelled by ADMIN with ZERO penalty. Reason: {}", orderId, order.getCancellationReason());

        Order saved = orderRepository.save(order);
        notificationService.notifyUser(saved.getCustomerId(), "Order cancelled",
            "Order " + orderId + " was cancelled by operations.", orderId);
        return saved;
    }

    private BigDecimal calculateDynamicFee(String status, BigDecimal estimate) {
        if ("PLACED".equals(status) || "BATCHED".equals(status)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        // Read system config for rate card if available
        SystemConfig config = configRepository.findById(1).orElse(null);
        BigDecimal minFee = BigDecimal.valueOf(100.00);

        BigDecimal percentage;
        switch (status) {
            case "PICKED_UP" -> percentage = BigDecimal.valueOf(0.35);
            case "AT_HUB" -> {
                percentage = BigDecimal.valueOf(0.50);
                minFee = BigDecimal.valueOf(150.00);
            }
            case "OUT_FOR_DELIVERY" -> {
                percentage = BigDecimal.valueOf(0.75);
                minFee = BigDecimal.valueOf(200.00);
            }
            default -> percentage = BigDecimal.valueOf(0.40);
        }

        BigDecimal calculated = estimate.multiply(percentage).setScale(2, RoundingMode.HALF_UP);
        BigDecimal finalFee = calculated.max(minFee);

        // Cap fee at total estimate so it never exceeds original price
        return finalFee.min(estimate).setScale(2, RoundingMode.HALF_UP);
    }
}
