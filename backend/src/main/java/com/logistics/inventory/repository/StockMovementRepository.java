package com.logistics.inventory.repository;

import com.logistics.inventory.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    Page<StockMovement> findByProductId(Long productId, Pageable pageable);

    @Query("""
            select m from StockMovement m
            left join m.targetWarehouse t
            where (cast(:warehouseId as long) is null
                   or m.warehouse.id = :warehouseId
                   or t.id = :warehouseId)
            """)
    Page<StockMovement> findRecent(@Param("warehouseId") Long warehouseId, Pageable pageable);

    List<StockMovement> findByCreatedAtAfter(Instant since);
}
