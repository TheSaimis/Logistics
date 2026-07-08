package com.logistics.inventory.service;

import com.logistics.inventory.dto.InventoryDtos.StockLevelDto;
import com.logistics.inventory.dto.InventoryDtos.StockMovementDto;
import com.logistics.inventory.dto.InventoryDtos.StockMovementRequest;
import com.logistics.inventory.entity.*;
import com.logistics.inventory.exception.BadRequestException;
import com.logistics.inventory.exception.NotFoundException;
import com.logistics.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockLevelRepository stockLevelRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;

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

    private void setAbsolute(Product product, Warehouse warehouse, int quantity) {
        StockLevel level = stockLevelRepository
                .findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .orElseGet(() -> StockLevel.builder().product(product).warehouse(warehouse).quantity(0).build());
        level.setQuantity(quantity);
        stockLevelRepository.save(level);
    }
}
