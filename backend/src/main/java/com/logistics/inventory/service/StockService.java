package com.logistics.inventory.service;

import com.logistics.inventory.dto.InventoryDtos.*;
import com.logistics.inventory.entity.*;
import com.logistics.inventory.exception.BadRequestException;
import com.logistics.inventory.exception.NotFoundException;
import com.logistics.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockLevelRepository stockLevelRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<StockLevelDto> levelsForProduct(Long productId) {
        return stockLevelRepository.findByProductId(productId).stream().map(StockLevelDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<StockLevelDto> levelsForWarehouse(Long warehouseId) {
        return stockLevelRepository.findByWarehouseId(warehouseId).stream().map(StockLevelDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<StockLevelDto> lowStock() {
        return stockLevelRepository.findLowStock(null).stream().map(StockLevelDto::from).toList();
    }

    @Transactional(readOnly = true)
    public Page<StockMovementDto> movements(Long productId, Pageable pageable) {
        Page<StockMovement> page = productId == null
                ? stockMovementRepository.findAll(pageable)
                : stockMovementRepository.findByProductId(productId, pageable);
        return page.map(StockMovementDto::from);
    }

    @Transactional
    public StockMovementDto recordMovement(StockMovementRequest request, String username) {
        if (request.quantity() <= 0) {
            throw new BadRequestException("Quantity must be positive");
        }
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> NotFoundException.of("Product", request.productId()));
        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> NotFoundException.of("Warehouse", request.warehouseId()));

        Warehouse target = null;
        switch (request.type()) {
            case IN -> adjust(product, warehouse, request.quantity());
            case OUT -> adjust(product, warehouse, -request.quantity());
            case ADJUSTMENT -> setAbsolute(product, warehouse, request.quantity());
            case TRANSFER -> {
                if (request.targetWarehouseId() == null) {
                    throw new BadRequestException("Target warehouse is required for transfers");
                }
                if (request.targetWarehouseId().equals(request.warehouseId())) {
                    throw new BadRequestException("Source and target warehouse must differ");
                }
                target = warehouseRepository.findById(request.targetWarehouseId())
                        .orElseThrow(() -> NotFoundException.of("Warehouse", request.targetWarehouseId()));
                adjust(product, warehouse, -request.quantity());
                adjust(product, target, request.quantity());
            }
        }

        StockMovement movement = StockMovement.builder()
                .product(product)
                .warehouse(warehouse)
                .targetWarehouse(target)
                .type(request.type())
                .quantity(request.quantity())
                .reference(request.reference())
                .note(request.note())
                .createdBy(username)
                .build();
        stockMovementRepository.save(movement);
        return StockMovementDto.from(movement);
    }

    private void adjust(Product product, Warehouse warehouse, int delta) {
        StockLevel level = stockLevelRepository
                .findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .orElseGet(() -> StockLevel.builder().product(product).warehouse(warehouse).quantity(0).build());
        int newQuantity = level.getQuantity() + delta;
        if (newQuantity < 0) {
            throw new BadRequestException("Insufficient stock in " + warehouse.getCode()
                    + ": available " + level.getQuantity() + ", requested " + (-delta));
        }
        level.setQuantity(newQuantity);
        stockLevelRepository.save(level);
    }

    /** Update bin location and per-warehouse reorder rule for a stock level row. */
    @Transactional
    public StockLevelDto updateLevelSettings(Long levelId, StockLevelSettingsRequest request) {
        StockLevel level = stockLevelRepository.findById(levelId)
                .orElseThrow(() -> NotFoundException.of("Stock level", levelId));
        if (request.minQuantity() != null && request.maxQuantity() != null
                && request.maxQuantity() < request.minQuantity()) {
            throw new BadRequestException("Max quantity cannot be below min quantity");
        }
        level.setBin(request.bin() == null || request.bin().isBlank() ? null : request.bin().trim());
        level.setMinQuantity(request.minQuantity());
        level.setMaxQuantity(request.maxQuantity());
        return StockLevelDto.from(level);
    }

    /**
     * Guided stock count for one warehouse (inspired by Odoo's inventory adjustments):
     * every differing count becomes an ADJUSTMENT movement, so corrections stay on the
     * audit trail instead of silently overwriting quantities.
     */
    @Transactional
    public StocktakeResult stocktake(StocktakeRequest request, String username) {
        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> NotFoundException.of("Warehouse", request.warehouseId()));
        String reference = "ST-" + LocalDate.now() + "-" + warehouse.getCode();

        List<StocktakeVariance> variances = new ArrayList<>();
        int adjusted = 0;
        for (StocktakeCount count : request.counts()) {
            Product product = productRepository.findById(count.productId())
                    .orElseThrow(() -> NotFoundException.of("Product", count.productId()));
            StockLevel level = stockLevelRepository
                    .findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                    .orElseGet(() -> StockLevel.builder()
                            .product(product).warehouse(warehouse).quantity(0).build());
            int expected = level.getQuantity();
            int counted = count.counted();
            if (counted != expected) {
                level.setQuantity(counted);
                stockLevelRepository.save(level);
                stockMovementRepository.save(StockMovement.builder()
                        .product(product)
                        .warehouse(warehouse)
                        .type(StockMovement.Type.ADJUSTMENT)
                        .quantity(counted)
                        .reference(reference)
                        .note("Stock take: expected " + expected + ", counted " + counted)
                        .createdBy(username)
                        .build());
                adjusted++;
                variances.add(new StocktakeVariance(product.getId(), product.getSku(),
                        product.getName(), expected, counted, counted - expected));
            }
        }
        auditService.record("STOCKTAKE_COMPLETED", "Warehouse", warehouse.getId(),
                warehouse.getCode() + ": " + request.counts().size() + " items counted, "
                        + adjusted + " adjusted (" + reference + ")");
        return new StocktakeResult(reference, request.counts().size(), adjusted, variances);
    }

    /**
     * Reorder suggestions (inspired by Odoo's reordering rules): per-warehouse min/max
     * rules first; products without any rule fall back to the global reorder level.
     */
    @Transactional(readOnly = true)
    public List<ReorderSuggestion> reorderSuggestions() {
        List<StockLevel> ruled = stockLevelRepository.findByMinQuantityNotNull();
        List<ReorderSuggestion> suggestions = new ArrayList<>();

        for (StockLevel level : ruled) {
            if (level.getQuantity() < level.getMinQuantity() && level.getProduct().isActive()) {
                int target = level.getMaxQuantity() != null
                        ? level.getMaxQuantity() : level.getMinQuantity() * 2;
                Product p = level.getProduct();
                suggestions.add(new ReorderSuggestion(p.getId(), p.getSku(), p.getName(),
                        p.getSupplier() != null ? p.getSupplier().getId() : null,
                        p.getSupplier() != null ? p.getSupplier().getName() : null,
                        level.getWarehouse().getId(), level.getWarehouse().getCode(),
                        level.getQuantity(), level.getMinQuantity(),
                        Math.max(target - level.getQuantity(), 1)));
            }
        }

        Set<Long> productsWithRules = ruled.stream()
                .map(l -> l.getProduct().getId()).collect(Collectors.toSet());
        for (StockLevel low : stockLevelRepository.findLowStock(null)) {
            Product p = low.getProduct();
            if (productsWithRules.contains(p.getId()) || !p.isActive()
                    || suggestions.stream().anyMatch(s -> s.productId().equals(p.getId()))) {
                continue;
            }
            int total = stockLevelRepository.totalQuantityForProduct(p.getId());
            suggestions.add(new ReorderSuggestion(p.getId(), p.getSku(), p.getName(),
                    p.getSupplier() != null ? p.getSupplier().getId() : null,
                    p.getSupplier() != null ? p.getSupplier().getName() : null,
                    null, null,
                    total, p.getReorderLevel(),
                    Math.max(p.getReorderLevel() * 2 - total, 1)));
        }

        suggestions.sort(Comparator
                .comparing((ReorderSuggestion s) -> s.supplierName() == null ? "~" : s.supplierName())
                .thenComparing(ReorderSuggestion::sku));
        return suggestions;
    }

    private void setAbsolute(Product product, Warehouse warehouse, int quantity) {
        StockLevel level = stockLevelRepository
                .findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .orElseGet(() -> StockLevel.builder().product(product).warehouse(warehouse).quantity(0).build());
        level.setQuantity(quantity);
        stockLevelRepository.save(level);
    }
}
