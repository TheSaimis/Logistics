package com.logistics.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stock_levels", uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "warehouse_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @Column(nullable = false)
    @Builder.Default
    private int quantity = 0;

    /** Physical storage location within the warehouse, e.g. "A-03-2" (InvenTree-style). */
    private String bin;

    /** Per-warehouse reorder rule (Odoo-style): refill when quantity drops below min. */
    @Column(name = "min_quantity")
    private Integer minQuantity;

    /** Refill target; suggestion tops stock up to this level (defaults to 2x min when unset). */
    @Column(name = "max_quantity")
    private Integer maxQuantity;
}
