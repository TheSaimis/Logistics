package com.logistics.inventory.controller;

import com.logistics.inventory.dto.InventoryDtos.ProductDto;
import com.logistics.inventory.dto.InventoryDtos.ProductRequest;
import com.logistics.inventory.exception.BadRequestException;
import com.logistics.inventory.service.ExcelService;
import com.logistics.inventory.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ExcelService excelService;

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

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel() {
        byte[] file = excelService.exportInventory();
        String filename = "inventory-" + LocalDate.now() + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }

    @PostMapping("/import")
    public ExcelService.ImportResult importExcel(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("No file uploaded");
        }
        try {
            return excelService.importProducts(file.getInputStream());
        } catch (IOException e) {
            throw new BadRequestException("Could not read the uploaded file");
        }
    }
}
