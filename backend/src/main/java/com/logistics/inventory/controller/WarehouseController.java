package com.logistics.inventory.controller;

import com.logistics.inventory.dto.InventoryDtos.StockLevelDto;
import com.logistics.inventory.dto.InventoryDtos.WarehouseDto;
import com.logistics.inventory.dto.InventoryDtos.WarehouseRequest;
import com.logistics.inventory.entity.StockLevel;
import com.logistics.inventory.entity.Warehouse;
import com.logistics.inventory.exception.BadRequestException;
import com.logistics.inventory.exception.NotFoundException;
import com.logistics.inventory.repository.StockLevelRepository;
import com.logistics.inventory.repository.WarehouseRepository;
import com.logistics.inventory.service.AuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseRepository warehouseRepository;
    private final StockLevelRepository stockLevelRepository;
    private final AuditService auditService;

    @GetMapping
    public List<WarehouseDto> list() {
        return warehouseRepository.findAll(Sort.by("code")).stream()
                .map(w -> WarehouseDto.from(w, stockLevelRepository.findByWarehouseId(w.getId()).stream()
                        .mapToLong(StockLevel::getQuantity).sum()))
                .toList();
    }

    @GetMapping("/{id}/stock")
    public List<StockLevelDto> stock(@PathVariable Long id) {
        if (!warehouseRepository.existsById(id)) {
            throw NotFoundException.of("Warehouse", id);
        }
        return stockLevelRepository.findByWarehouseId(id).stream().map(StockLevelDto::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WarehouseDto create(@Valid @RequestBody WarehouseRequest request) {
        if (warehouseRepository.existsByCodeIgnoreCase(request.code())) {
            throw new BadRequestException("Warehouse code already exists: " + request.code());
        }
        Warehouse warehouse = Warehouse.builder()
                .code(request.code())
                .name(request.name())
                .location(request.location())
                .capacity(request.capacity())
                .build();
        warehouseRepository.save(warehouse);
        auditService.record("WAREHOUSE_CREATED", "Warehouse", warehouse.getId(), warehouse.getCode());
        return WarehouseDto.from(warehouse, 0L);
    }

    @PutMapping("/{id}")
    public WarehouseDto update(@PathVariable Long id, @Valid @RequestBody WarehouseRequest request) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Warehouse", id));
        if (!warehouse.getCode().equalsIgnoreCase(request.code())
                && warehouseRepository.existsByCodeIgnoreCase(request.code())) {
            throw new BadRequestException("Warehouse code already exists: " + request.code());
        }
        warehouse.setCode(request.code());
        warehouse.setName(request.name());
        warehouse.setLocation(request.location());
        warehouse.setCapacity(request.capacity());
        warehouseRepository.save(warehouse);
        long units = stockLevelRepository.findByWarehouseId(id).stream()
                .mapToLong(StockLevel::getQuantity).sum();
        return WarehouseDto.from(warehouse, units);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Warehouse", id));
        warehouseRepository.delete(warehouse);
        auditService.record("WAREHOUSE_DELETED", "Warehouse", id, warehouse.getCode());
    }
}
