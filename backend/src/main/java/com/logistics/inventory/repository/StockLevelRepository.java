package com.logistics.inventory.repository;

import com.logistics.inventory.entity.StockLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface StockLevelRepository extends JpaRepository<StockLevel, Long> {

    Optional<StockLevel> findByProductIdAndWarehouseId(Long productId, Long warehouseId);

    List<StockLevel> findByMinQuantityNotNull();

    List<StockLevel> findByProductId(Long productId);

    List<StockLevel> findByWarehouseId(Long warehouseId);

    @Query("select coalesce(sum(s.quantity), 0) from StockLevel s where s.product.id = :productId")
    int totalQuantityForProduct(@Param("productId") Long productId);

    /**
     * Without a warehouse filter, low stock compares the product's total quantity across
     * all warehouses to its reorder level; with a filter, the single warehouse's quantity.
     */
    @Query("""
            select s from StockLevel s
            join fetch s.product p
            join fetch s.warehouse w
            where (cast(:warehouseId as long) is null
                   and (select coalesce(sum(s2.quantity), 0) from StockLevel s2 where s2.product = p) <= p.reorderLevel)
               or (cast(:warehouseId as long) is not null
                   and w.id = :warehouseId and s.quantity <= p.reorderLevel)
            """)
    List<StockLevel> findLowStock(@Param("warehouseId") Long warehouseId);

    @Query("""
            select coalesce(sum(s.quantity * s.product.unitPrice), 0) from StockLevel s
            where (cast(:warehouseId as long) is null or s.warehouse.id = :warehouseId)
            """)
    BigDecimal totalStockValue(@Param("warehouseId") Long warehouseId);

    @Query("""
            select coalesce(sum(s.quantity), 0) from StockLevel s
            where (cast(:warehouseId as long) is null or s.warehouse.id = :warehouseId)
            """)
    long totalUnits(@Param("warehouseId") Long warehouseId);

    @Query("""
            select coalesce(c.name, 'Uncategorized'), sum(s.quantity), sum(s.quantity * p.unitPrice)
            from StockLevel s join s.product p left join p.category c
            group by c.name order by sum(s.quantity * p.unitPrice) desc
            """)
    List<Object[]> stockByCategory();

    @Query("""
            select p.sku, p.name, sum(s.quantity * p.unitPrice)
            from StockLevel s join s.product p
            group by p.id, p.sku, p.name
            order by sum(s.quantity * p.unitPrice) desc
            """)
    List<Object[]> stockValueByProduct();
}
