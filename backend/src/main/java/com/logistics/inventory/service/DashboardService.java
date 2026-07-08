package com.logistics.inventory.service;

import com.logistics.inventory.dto.InventoryDtos.DashboardStats;
import com.logistics.inventory.dto.InventoryDtos.StockLevelDto;
import com.logistics.inventory.dto.InventoryDtos.StockMovementDto;
import com.logistics.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final SupplierRepository supplierRepository;
    private final StockLevelRepository stockLevelRepository;
    private final StockMovementRepository stockMovementRepository;

    @Transactional(readOnly = true)
    public DashboardStats stats(Long warehouseId) {
        List<StockLevelDto> lowStock = stockLevelRepository.findLowStock(warehouseId).stream()
                .map(StockLevelDto::from).toList();
        List<StockMovementDto> recent = stockMovementRepository
                .findRecent(warehouseId, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(StockMovementDto::from).getContent();
        return new DashboardStats(
                productRepository.count(),
                warehouseRepository.count(),
                supplierRepository.count(),
                stockLevelRepository.totalUnits(warehouseId),
                stockLevelRepository.totalStockValue(warehouseId),
                lowStock,
                recent);
    }
}
