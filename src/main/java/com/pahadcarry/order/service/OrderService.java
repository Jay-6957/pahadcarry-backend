package com.pahadcarry.order.service;

import com.pahadcarry.common.ErrorCode;
import com.pahadcarry.common.PahadCarryException;
import com.pahadcarry.order.dto.PlaceOrderDto;
import com.pahadcarry.order.model.Order;
import com.pahadcarry.order.repository.OrderRepository;
import com.pahadcarry.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final PricingService pricingService;
    private final NotificationService notificationService;

    @Transactional
    public Order placeOrder(String customerId, PlaceOrderDto dto) {
        BigDecimal weight = dto.getEstimatedWeightKg();
        BigDecimal fare = pricingService.calculateFare(weight,
                dto.getPickupLat(), dto.getPickupLng(),
                dto.getDropLat(), dto.getDropLng());

        String orderType = pricingService.isDedicatedTrip(weight) ? "DEDICATED_TRIP" : "STANDARD_POOL";

        Order order = Order.builder()
                .customerId(customerId)
                .pickupLat(dto.getPickupLat())
                .pickupLng(dto.getPickupLng())
                .pickupAddress(dto.getPickupAddress())
                .pickupContactName(dto.getPickupContactName())
                .pickupContactPhone(dto.getPickupContactPhone())
                .dropLat(dto.getDropLat())
                .dropLng(dto.getDropLng())
                .dropAddress(dto.getDropAddress())
                .dropContactName(dto.getDropContactName())
                .dropContactPhone(dto.getDropContactPhone())
                .goodsDescription(dto.getGoodsDescription())
                .estimatedWeightKg(weight)
                .quantityNote(dto.getQuantityNote())
                .orderType(orderType)
                .status("PLACED")
                .priceEstimate(fare)
                .build();

        Order saved = orderRepository.save(order);
        notificationService.notifyUser(customerId, "Order placed", "Your order " + saved.getId() + " has been placed.", saved.getId());
        return saved;
    }

    public Order getOrder(String orderId, String customerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> PahadCarryException.notFound(ErrorCode.NOT_FOUND, "Order not found"));
        if (!order.getCustomerId().equals(customerId)) {
            throw PahadCarryException.forbidden(ErrorCode.FORBIDDEN, "Access denied to this order");
        }
        return order;
    }

    @Transactional
    public Order updateOrder(String orderId, String customerId, com.pahadcarry.order.dto.UpdateOrderDto dto) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> PahadCarryException.notFound(ErrorCode.NOT_FOUND, "Order not found"));

        if (customerId != null && !order.getCustomerId().equals(customerId)) {
            throw PahadCarryException.forbidden(ErrorCode.FORBIDDEN, "Access denied to this order");
        }

        if (!"PLACED".equals(order.getStatus()) && !"BATCHED".equals(order.getStatus())) {
            throw PahadCarryException.badRequest(
                    ErrorCode.BAD_REQUEST,
                    "Order cannot be edited after pickup (Current status: " + order.getStatus() + ")"
            );
        }

        if (dto.getGoodsDescription() != null && !dto.getGoodsDescription().isBlank()) {
            order.setGoodsDescription(dto.getGoodsDescription());
        }
        if (dto.getQuantityNote() != null) {
            order.setQuantityNote(dto.getQuantityNote());
        }
        if (dto.getPickupAddress() != null && !dto.getPickupAddress().isBlank()) {
            order.setPickupAddress(dto.getPickupAddress());
        }
        if (dto.getPickupLat() != null && dto.getPickupLng() != null) {
            order.setPickupLat(dto.getPickupLat());
            order.setPickupLng(dto.getPickupLng());
        }
        if (dto.getPickupContactName() != null && !dto.getPickupContactName().isBlank()) {
            order.setPickupContactName(dto.getPickupContactName());
        }
        if (dto.getPickupContactPhone() != null && !dto.getPickupContactPhone().isBlank()) {
            order.setPickupContactPhone(dto.getPickupContactPhone());
        }
        if (dto.getDropAddress() != null && !dto.getDropAddress().isBlank()) {
            order.setDropAddress(dto.getDropAddress());
        }
        if (dto.getDropContactName() != null && !dto.getDropContactName().isBlank()) {
            order.setDropContactName(dto.getDropContactName());
        }
        if (dto.getDropContactPhone() != null && !dto.getDropContactPhone().isBlank()) {
            order.setDropContactPhone(dto.getDropContactPhone());
        }
        if (dto.getDropLat() != null && dto.getDropLng() != null) {
            order.setDropLat(dto.getDropLat());
            order.setDropLng(dto.getDropLng());
        }

        if (dto.getEstimatedWeightKg() != null) {
            order.setEstimatedWeightKg(dto.getEstimatedWeightKg());
            // Recalculate fare dynamically
            BigDecimal newFare = pricingService.calculateFare(
                    order.getEstimatedWeightKg(),
                    order.getPickupLat(), order.getPickupLng(),
                    order.getDropLat(), order.getDropLng()
            );
            order.setPriceEstimate(newFare);
            order.setOrderType(pricingService.isDedicatedTrip(order.getEstimatedWeightKg()) ? "DEDICATED_TRIP" : "STANDARD_POOL");
        }

        return orderRepository.save(order);
    }

    public Order getOrderForAnyRole(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> PahadCarryException.notFound(ErrorCode.NOT_FOUND, "Order not found"));
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public List<Order> getMyOrders(String customerId) {
        return orderRepository.findByCustomerIdOrderByPlacedAtDesc(customerId);
    }

    @Transactional
    public void transitionStatus(String orderId, String newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> PahadCarryException.notFound(ErrorCode.NOT_FOUND, "Order not found"));
        order.setStatus(newStatus);
        if ("PICKED_UP".equals(newStatus)) order.setPickedUpAt(Instant.now());
        if ("DELIVERED".equals(newStatus)) order.setDeliveredAt(Instant.now());
        orderRepository.save(order);
        notificationService.notifyUser(order.getCustomerId(), "Order status updated", "Order " + orderId + " is now " + newStatus + ".", orderId);
    }
}
