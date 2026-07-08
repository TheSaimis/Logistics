package com.logistics.inventory.repository;

import com.logistics.inventory.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    boolean existsByCodeIgnoreCase(String code);
}
