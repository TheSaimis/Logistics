package com.logistics.inventory.service;

import com.logistics.inventory.dto.AnalyticsDtos.*;
import com.logistics.inventory.entity.StockLevel;
import com.logistics.inventory.entity.StockMovement;
import com.logistics.inventory.repository.StockLevelRepository;
import com.logistics.inventory.repository.StockMovementRepository;
import com.logistics.inventory.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final int FLOW_DAYS = 30;
    private static final int TOP_PRODUCTS = 5;

    private final StockLevelRepository stockLevelRepository;
    private final StockMovementRepository stockMovementRepository;
    private final WarehouseRepository warehouseRepository;

    @Transactional(readOnly = true)
    public AnalyticsResponse analytics() {
        return new AnalyticsResponse(
                stockByCategory(),
                warehouseLoads(),
                movementsDaily(),
                topProductsByValue());
    }

    private List<CategorySlice> stockByCategory() {
        return stockLevelRepository.stockByCategory().stream()
                .map(row -> new CategorySlice((String) row[0], (Long) row[1], (BigDecimal) row[2]))
                .toList();
    }

    private List<WarehouseLoad> warehouseLoads() {
        return warehouseRepository.findAll().stream()
                .map(w -> new WarehouseLoad(w.getCode(), w.getName(),
                        stockLevelRepository.findByWarehouseId(w.getId()).stream()
                                .mapToLong(StockLevel::getQuantity).sum(),
                        w.getCapacity()))
                .toList();
    }

    /** Inbound = IN + incoming TRANSFER quantity; outbound = OUT quantity. Last 30 days, gaps filled. */
    private List<DailyFlow> movementsDaily() {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        LocalDate start = today.minusDays(FLOW_DAYS - 1);

        Map<LocalDate, long[]> byDay = new HashMap<>();
        for (StockMovement m : stockMovementRepository.findByCreatedAtAfter(
                Instant.now().minus(Duration.ofDays(FLOW_DAYS)))) {
            LocalDate day = m.getCreatedAt().atZone(zone).toLocalDate();
            long[] flow = byDay.computeIfAbsent(day, d -> new long[2]);
            switch (m.getType()) {
                case IN, TRANSFER -> flow[0] += m.getQuantity();
                case OUT -> flow[1] += m.getQuantity();
                case ADJUSTMENT -> { /* absolute set, not a flow */ }
            }
        }

        List<DailyFlow> result = new ArrayList<>(FLOW_DAYS);
        for (LocalDate d = start; !d.isAfter(today); d = d.plusDays(1)) {
            long[] flow = byDay.getOrDefault(d, new long[2]);
            result.add(new DailyFlow(d, flow[0], flow[1]));
        }
        return result;
    }

    private List<ProductValue> topProductsByValue() {
        return stockLevelRepository.stockValueByProduct().stream()
                .limit(TOP_PRODUCTS)
                .map(row -> new ProductValue((String) row[0], (String) row[1], (BigDecimal) row[2]))
                .toList();
    }
}
