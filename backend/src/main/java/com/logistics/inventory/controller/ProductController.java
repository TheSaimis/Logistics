package com.logistics.inventory.controller;

import com.logistics.inventory.dto.InventoryDtos.ProductDto;
import com.logistics.inventory.dto.InventoryDtos.ProductRequest;
import com.logistics.inventory.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public Page<ProductDto> list(@RequestParam(required = false) String search,
                                 @RequestParam(required = false) Long categoryId,
                                 @RequestParam(required = false) Long supplierId,
                                 @RequestParam(required = false) String status,
                                 @RequestParam(required = false) String stockStatus,
                                 @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC)
                                 Pageable pageable) {
        return productService.search(search, categoryId, supplierId, status, stockStatus, pageable);
    }

    @GetMapping("/{id}")
    public ProductDto get(@PathVariable Long id) {
        return productService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto create(@Valid @RequestBody ProductRequest request) {
        return productService.create(request);
    }

    @PutMapping("/{id}")
    public ProductDto update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        productService.delete(id);
    }
}
