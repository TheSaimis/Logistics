package com.logistics.inventory.repository;

import com.logistics.inventory.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsBySkuIgnoreCase(String sku);

    @Query("""
            select p from Product p
            where (cast(:search as string) is null
                   or lower(p.name) like lower(concat('%', cast(:search as string), '%'))
                   or lower(p.sku) like lower(concat('%', cast(:search as string), '%')))
              and (cast(:categoryId as long) is null or p.category.id = :categoryId)
              and (cast(:supplierId as long) is null or p.supplier.id = :supplierId)
              and (:status = 'ALL'
                   or (:status = 'ACTIVE' and p.active = true)
                   or (:status = 'INACTIVE' and p.active = false))
              and (:stockStatus = 'ALL'
                   or (:stockStatus = 'OUT'
                       and (select coalesce(sum(sl.quantity), 0) from StockLevel sl where sl.product = p) = 0)
                   or (:stockStatus = 'LOW'
                       and (select coalesce(sum(sl.quantity), 0) from StockLevel sl where sl.product = p) > 0
                       and (select coalesce(sum(sl.quantity), 0) from StockLevel sl where sl.product = p) <= p.reorderLevel)
                   or (:stockStatus = 'IN'
                       and (select coalesce(sum(sl.quantity), 0) from StockLevel sl where sl.product = p) > p.reorderLevel))
            """)
    Page<Product> search(@Param("search") String search,
                         @Param("categoryId") Long categoryId,
                         @Param("supplierId") Long supplierId,
                         @Param("status") String status,
                         @Param("stockStatus") String stockStatus,
                         Pageable pageable);
}
