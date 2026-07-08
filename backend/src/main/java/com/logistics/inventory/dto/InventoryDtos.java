package com.logistics.inventory.dto;

import com.logistics.inventory.entity.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class InventoryDtos {

    private InventoryDtos() {}

    public record CategoryDto(Long id, String name, String description) {
        public static CategoryDto from(Category c) {
            return new CategoryDto(c.getId(), c.getName(), c.getDescription());
        }
    }

    public record CategoryRequest(@NotBlank @Size(max = 128) String name,
                                  @Size(max = 512) String description) {}

    public record SupplierDto(Long id, String name, String contactEmail, String phone, String address) {
        public static SupplierDto from(Supplier s) {
            return new SupplierDto(s.getId(), s.getName(), s.getContactEmail(), s.getPhone(), s.getAddress());
        }
    }

    public record SupplierRequest(@NotBlank @Size(max = 255) String name,
                                  @Email String contactEmail,
                                  @Size(max = 64) String phone,
                                  @Size(max = 512) String address) {}

    public record WarehouseDto(Long id, String code, String name, String location, Integer capacity,
                               Long totalUnits) {
        public static WarehouseDto from(Warehouse w, Long totalUnits) {
            return new WarehouseDto(w.getId(), w.getCode(), w.getName(), w.getLocation(), w.getCapacity(),
                    totalUnits);
        }
    }

    public record WarehouseRequest(@NotBlank @Size(max = 16) String code,
                                   @NotBlank @Size(max = 255) String name,
                                   @Size(max = 255) String location,
                                   @PositiveOrZero Integer capacity) {}

    public record ProductDto(Long id, String sku, String name, String description,
                             Long categoryId, String categoryName,
                             Long supplierId, String supplierName,
                             BigDecimal unitPrice, int reorderLevel, boolean active,
                             int totalQuantity, Instant createdAt, Instant updatedAt) {
        public static ProductDto from(Product p, int totalQuantity) {
            return new ProductDto(p.getId(), p.getSku(), p.getName(), p.getDescription(),
                    p.getCategory() != null ? p.getCategory().getId() : null,
                    p.getCategory() != null ? p.getCategory().getName() : null,
                    p.getSupplier() != null ? p.getSupplier().getId() : null,
                    p.getSupplier() != null ? p.getSupplier().getName() : null,
                    p.getUnitPrice(), p.getReorderLevel(), p.isActive(),
                    totalQuantity, p.getCreatedAt(), p.getUpdatedAt());
        }
    }

    public record ProductRequest(@NotBlank @Size(max = 64) String sku,
                                 @NotBlank @Size(max = 255) String name,
                                 @Size(max = 1024) String description,
                                 Long categoryId,
                                 Long supplierId,
                                 @NotNull @DecimalMin("0.0") BigDecimal unitPrice,
                                 @PositiveOrZero int reorderLevel,
                                 Boolean active) {}

    public record StockLevelDto(Long id, Long productId, String productSku, String productName,
                                Long warehouseId, String warehouseCode, String warehouseName,
                                int quantity, int reorderLevel) {
        public static StockLevelDto from(StockLevel s) {
            return new StockLevelDto(s.getId(),
                    s.getProduct().getId(), s.getProduct().getSku(), s.getProduct().getName(),
                    s.getWarehouse().getId(), s.getWarehouse().getCode(), s.getWarehouse().getName(),
                    s.getQuantity(), s.getProduct().getReorderLevel());
        }
    }

    public record StockMovementDto(Long id, Long productId, String productSku, String productName,
                                   Long warehouseId, String warehouseCode,
                                   Long targetWarehouseId, String targetWarehouseCode,
                                   String type, int quantity, String reference, String note,
                                   String createdBy, Instant createdAt) {
        public static StockMovementDto from(StockMovement m) {
            return new StockMovementDto(m.getId(),
                    m.getProduct().getId(), m.getProduct().getSku(), m.getProduct().getName(),
                    m.getWarehouse().getId(), m.getWarehouse().getCode(),
                    m.getTargetWarehouse() != null ? m.getTargetWarehouse().getId() : null,
                    m.getTargetWarehouse() != null ? m.getTargetWarehouse().getCode() : null,
                    m.getType().name(), m.getQuantity(), m.getReference(), m.getNote(),
                    m.getCreatedBy(), m.getCreatedAt());
        }
    }

    public record StockMovementRequest(@NotNull Long productId,
                                       @NotNull Long warehouseId,
                                       Long targetWarehouseId,
                                       @NotNull StockMovement.Type type,
                                       @NotNull Integer quantity,
                                       @Size(max = 128) String reference,
                                       @Size(max = 512) String note) {}

    public record DashboardStats(long totalProducts, long totalWarehouses, long totalSuppliers,
                                 long totalUnits, BigDecimal stockValue,
                                 List<StockLevelDto> lowStock,
                                 List<StockMovementDto> recentMovements) {}
}
