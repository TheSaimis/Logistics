package com.logistics.inventory.service;

import com.logistics.inventory.dto.InventoryDtos.ProductDto;
import com.logistics.inventory.dto.InventoryDtos.ProductRequest;
import com.logistics.inventory.entity.Product;
import com.logistics.inventory.exception.BadRequestException;
import com.logistics.inventory.exception.NotFoundException;
import com.logistics.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final StockLevelRepository stockLevelRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<ProductDto> search(String search, Long categoryId, Long supplierId, String status,
                                   String stockStatus, Pageable pageable) {
        String normalized = (search == null || search.isBlank()) ? null : search.trim();
        String statusFilter = switch (status == null ? "" : status.toUpperCase()) {
            case "ACTIVE" -> "ACTIVE";
            case "INACTIVE" -> "INACTIVE";
            default -> "ALL";
        };
        String stockFilter = switch (stockStatus == null ? "" : stockStatus.toUpperCase()) {
            case "IN" -> "IN";
            case "LOW" -> "LOW";
            case "OUT" -> "OUT";
            default -> "ALL";
        };
        return productRepository.search(normalized, categoryId, supplierId, statusFilter, stockFilter, pageable)
                .map(p -> ProductDto.from(p, stockLevelRepository.totalQuantityForProduct(p.getId())));
    }

    @Transactional(readOnly = true)
    public ProductDto get(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Product", id));
        return ProductDto.from(product, stockLevelRepository.totalQuantityForProduct(id));
    }

    @Transactional
    public ProductDto create(ProductRequest request) {
        if (productRepository.existsBySkuIgnoreCase(request.sku())) {
            throw new BadRequestException("SKU already exists: " + request.sku());
        }
        Product product = new Product();
        apply(product, request);
        productRepository.save(product);
        auditService.record("PRODUCT_CREATED", "Product", product.getId(), product.getSku());
        return ProductDto.from(product, 0);
    }

    @Transactional
    public ProductDto update(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Product", id));
        if (!product.getSku().equalsIgnoreCase(request.sku())
                && productRepository.existsBySkuIgnoreCase(request.sku())) {
            throw new BadRequestException("SKU already exists: " + request.sku());
        }
        apply(product, request);
        auditService.record("PRODUCT_UPDATED", "Product", id, product.getSku());
        return ProductDto.from(product, stockLevelRepository.totalQuantityForProduct(id));
    }

    @Transactional
    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Product", id));
        // Soft delete keeps movement history intact
        product.setActive(false);
        auditService.record("PRODUCT_DEACTIVATED", "Product", id, product.getSku());
    }

    private void apply(Product product, ProductRequest request) {
        product.setSku(request.sku());
        product.setName(request.name());
        product.setDescription(request.description());
        product.setUnitPrice(request.unitPrice());
        product.setReorderLevel(request.reorderLevel());
        if (request.active() != null) {
            product.setActive(request.active());
        }
        product.setCategory(request.categoryId() == null ? null
                : categoryRepository.findById(request.categoryId())
                        .orElseThrow(() -> NotFoundException.of("Category", request.categoryId())));
        product.setSupplier(request.supplierId() == null ? null
                : supplierRepository.findById(request.supplierId())
                        .orElseThrow(() -> NotFoundException.of("Supplier", request.supplierId())));
    }
}
