package com.pahadcarry.order;

import com.pahadcarry.common.ApiResponse;
import com.pahadcarry.order.dto.PlaceOrderDto;
import com.pahadcarry.order.model.Order;
import com.pahadcarry.order.service.OrderCancellationService;
import com.pahadcarry.order.service.OrderService;
import com.pahadcarry.order.service.PricingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderCancellationService cancellationService;
    private final PricingService pricingService;

    @GetMapping("/estimate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> estimateFare(
            @RequestParam Double pickupLat,
            @RequestParam Double pickupLng,
            @RequestParam Double dropLat,
            @RequestParam Double dropLng,
            @RequestParam BigDecimal weightKg) {
        PricingService.FareBreakdown breakdown = pricingService.calculateBreakdown(
            weightKg, pickupLat, pickupLng, dropLat, dropLng);
        boolean isDedicated = pricingService.isDedicatedTrip(weightKg);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
            "estimatedFare", breakdown.estimatedFare(),
            "distanceKm", breakdown.distanceKm(),
            "baseFare", breakdown.baseFare(),
            "distanceCharge", breakdown.distanceCharge(),
            "weightCharge", breakdown.weightCharge(),
                "orderType", isDedicated ? "DEDICATED_TRIP" : "STANDARD_POOL",
                "currency", "INR"
        )));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Order>> placeOrder(
            Authentication auth,
            @Valid @RequestBody PlaceOrderDto dto) {
        String customerId = (String) auth.getPrincipal();
        Order order = orderService.placeOrder(customerId, dto);
        return ResponseEntity.ok(ApiResponse.ok(order));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Order>>> getMyOrders(Authentication auth) {
        String customerId = (String) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.ok(orderService.getMyOrders(customerId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Order>> getOrder(
            Authentication auth,
            @PathVariable("id") String orderId) {
        String customerId = (String) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrder(orderId, customerId)));
    }

    @GetMapping("/{id}/invoice")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getInvoice(
            Authentication auth,
            @PathVariable("id") String orderId) {
        Order order = orderService.getOrderForAnyRole(orderId);
        
        BigDecimal netAmount = order.getPriceEstimate();
        BigDecimal gstAmount = netAmount.multiply(BigDecimal.valueOf(0.05)).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal totalAmount = netAmount.add(gstAmount);

        Map<String, Object> invoice = new java.util.LinkedHashMap<>();
        invoice.put("invoiceNumber", "INV-PC-" + order.getId().substring(0, 8).toUpperCase());
        invoice.put("orderId", order.getId());
        invoice.put("orderDate", order.getPlacedAt());
        invoice.put("customerName", order.getPickupContactName());
        invoice.put("customerPhone", order.getPickupContactPhone());
        invoice.put("pickupAddress", order.getPickupAddress());
        invoice.put("dropAddress", order.getDropAddress());
        invoice.put("goodsDescription", order.getGoodsDescription());
        invoice.put("weightKg", order.getEstimatedWeightKg());
        invoice.put("orderType", order.getOrderType());
        invoice.put("status", order.getStatus());
        invoice.put("paymentStatus", order.getPaymentStatus());
        invoice.put("netAmount", netAmount);
        invoice.put("gstRate", "5% GST");
        invoice.put("gstAmount", gstAmount);
        invoice.put("totalAmount", totalAmount);
        invoice.put("corridorHub", "Haldwani Central Hub, Uttarakhand");
        invoice.put("hsnCode", "996511 (Road Freight Transport)");

        return ResponseEntity.ok(ApiResponse.ok(invoice));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Order>> updateOrder(
            Authentication auth,
            @PathVariable("id") String orderId,
            @Valid @RequestBody com.pahadcarry.order.dto.UpdateOrderDto dto) {
        String customerId = (String) auth.getPrincipal();
        Order updated = orderService.updateOrder(orderId, customerId, dto);
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

    @GetMapping("/{id}/cancellation-quote")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCancellationQuote(
            Authentication auth,
            @PathVariable("id") String orderId) {
        String customerId = (String) auth.getPrincipal();
        Map<String, Object> quote = cancellationService.getCancellationQuote(orderId, customerId);
        return ResponseEntity.ok(ApiResponse.ok(quote));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Order>> cancelOrder(
            Authentication auth,
            @PathVariable("id") String orderId,
            @RequestParam(required = false) String reason) {
        String customerId = (String) auth.getPrincipal();
        Order cancelled = cancellationService.cancelOrder(orderId, customerId, reason);
        return ResponseEntity.ok(ApiResponse.ok(cancelled));
    }
}
