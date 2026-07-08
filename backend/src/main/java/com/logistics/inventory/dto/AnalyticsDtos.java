package com.logistics.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class AnalyticsDtos {

    private AnalyticsDtos() {}

    public record CategorySlice(String label, long units, BigDecimal value) {}

    public record WarehouseLoad(String code, String name, long units, Integer capacity) {}

    public record DailyFlow(LocalDate date, long inbound, long outbound) {}

    public record ProductValue(String sku, String name, BigDecimal value) {}

    public record AnalyticsResponse(List<CategorySlice> stockByCategory,
                                    List<WarehouseLoad> warehouseLoads,
                                    List<DailyFlow> movementsDaily,
                                    List<ProductValue> topProductsByValue) {}
}
