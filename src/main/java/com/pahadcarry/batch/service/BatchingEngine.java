package com.pahadcarry.batch.service;

import com.pahadcarry.batch.model.Batch;
import com.pahadcarry.batch.model.BatchStop;
import com.pahadcarry.batch.repository.BatchRepository;
import com.pahadcarry.batch.repository.BatchStopRepository;
import com.pahadcarry.config.repository.SystemConfigRepository;
import com.pahadcarry.order.model.Order;
import com.pahadcarry.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchingEngine {

    private final OrderRepository orderRepository;
    private final BatchRepository batchRepository;
    private final BatchStopRepository batchStopRepository;
    private final SystemConfigRepository configRepository;

    // Haldwani Central Hub coordinates
    private static final double HUB_LAT = 29.2183;
    private static final double HUB_LNG = 79.5130;

    /**
     * Runs every 10 minutes. Checks:
     * 1. If open orders >= batchOrderThreshold → form batch immediately
     * 2. If oldest open order >= batchMaxWaitHours → force-form batch
     */
    @Scheduled(fixedDelay = 600_000) // every 10 minutes
    @Transactional
    public void evaluateAndFormBatches() {
        var config = configRepository.getConfig();
        int threshold = config.getBatchOrderThreshold();
        int maxWaitHours = config.getBatchMaxWaitHours();

        List<Order> openOrders = orderRepository.findByStatus("PLACED");
        if (openOrders.isEmpty()) return;

        Instant cutoff = Instant.now().minus(maxWaitHours, ChronoUnit.HOURS);
        boolean hasExpired = openOrders.stream().anyMatch(o -> o.getPlacedAt().isBefore(cutoff));
        boolean thresholdMet = openOrders.size() >= threshold;

        if (thresholdMet || hasExpired) {
            log.info("BatchingEngine triggered: {} open orders (threshold={}, hasExpired={})",
                    openOrders.size(), thresholdMet, hasExpired);
            formBatch(openOrders);
        }
    }

    /**
     * Forms an optimal batch:
     * - Minimum total distance using 2-phase TSP (Nearest Neighbor).
     * - Phase 1 (Pickups): Driver collects all pickups in shortest-path chain P1 -> P2 -> ... -> Pn.
     * - Phase 2 (Drops): Starting from the last pickup location, driver delivers D1 -> D2 -> ... -> Dn in forward order.
     * - Eliminates repetitive return trips to the hub.
     */
    @Transactional
    public Batch formBatch(List<Order> orders) {
        BigDecimal totalWeight = orders.stream()
                .map(Order::getEstimatedWeightKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Batch batch = Batch.builder()
                .areaCluster("Haldwani-Nainital Corridor")
                .totalWeightKg(totalWeight)
                .status("FORMING")
                .build();
        batch = batchRepository.save(batch);
        String batchId = batch.getId();

        // Sequence all stops using Precedence-Constrained Pickup & Delivery (PDP) Nearest-Neighbor
        List<StopCandidate> optimalStops = sequenceOptimalPDPStops(orders, HUB_LAT, HUB_LNG);

        AtomicInteger seq = new AtomicInteger(1);
        for (StopCandidate candidate : optimalStops) {
            BatchStop stop = BatchStop.builder()
                    .batchId(batchId)
                    .orderId(candidate.order.getId())
                    .sequence(seq.getAndIncrement())
                    .stopType(candidate.stopType)
                    .lat(candidate.lat)
                    .lng(candidate.lng)
                    .address(candidate.address)
                    .build();
            batchStopRepository.save(stop);

            // Mark order as batched on pickup stop
            if ("PICKUP".equals(candidate.stopType)) {
                Order order = candidate.order;
                order.setBatchId(batchId);
                order.setStatus("BATCHED");
                order.setBatchedAt(Instant.now());
                orderRepository.save(order);
            }
        }

        log.info("Batch {} optimally formed with {} orders ({} sequenced stops), total {}kg",
                batchId, orders.size(), optimalStops.size(), totalWeight);

        return batch;
    }

    /**
     * Precedence-Constrained Pickup & Delivery Problem (PDP) Routing:
     * - Any order's Drop can only occur AFTER its Pickup has been completed.
     * - At every step, searches all currently eligible stops (all unvisited Pickups + all Drops of picked-up orders).
     * - Selects the nearest eligible stop to minimize total driving distance.
     * - Handles both clustered valley pickups and en-route pickups seamlessly.
     */
    private List<StopCandidate> sequenceOptimalPDPStops(List<Order> orders, double startLat, double startLng) {
        List<StopCandidate> result = new ArrayList<>();
        Set<String> pickedUpOrderIds = new HashSet<>();
        Set<String> completedDropOrderIds = new HashSet<>();
        double curLat = startLat, curLng = startLng;

        while (completedDropOrderIds.size() < orders.size()) {
            StopCandidate nearest = null;
            double minDist = Double.MAX_VALUE;

            for (Order o : orders) {
                // 1. Check if unvisited Pickup is a candidate
                if (!pickedUpOrderIds.contains(o.getId())) {
                    double d = dist(curLat, curLng, o.getPickupLat(), o.getPickupLng());
                    if (d < minDist) {
                        minDist = d;
                        nearest = new StopCandidate("PICKUP", o, o.getPickupLat(), o.getPickupLng(), o.getPickupAddress());
                    }
                }

                // 2. Check if Drop is a candidate (ONLY ELIGIBLE IF PICKUP ALREADY COMPLETED)
                if (pickedUpOrderIds.contains(o.getId()) && !completedDropOrderIds.contains(o.getId())) {
                    double d = dist(curLat, curLng, o.getDropLat(), o.getDropLng());
                    if (d < minDist) {
                        minDist = d;
                        nearest = new StopCandidate("DROP", o, o.getDropLat(), o.getDropLng(), o.getDropAddress());
                    }
                }
            }

            if (nearest != null) {
                result.add(nearest);
                curLat = nearest.lat;
                curLng = nearest.lng;

                if ("PICKUP".equals(nearest.stopType)) {
                    pickedUpOrderIds.add(nearest.order.getId());
                } else if ("DROP".equals(nearest.stopType)) {
                    completedDropOrderIds.add(nearest.order.getId());
                }
            } else {
                break;
            }
        }

        return result;
    }

    private double dist(double lat1, double lng1, double lat2, double lng2) {
        double dLat = lat2 - lat1, dLng = lng2 - lng1;
        return Math.sqrt(dLat * dLat + dLng * dLng);
    }

    @lombok.AllArgsConstructor
    private static class StopCandidate {
        String stopType; // PICKUP or DROP
        Order order;
        double lat;
        double lng;
        String address;
    }
}
