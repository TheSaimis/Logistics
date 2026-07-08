package com.logistics.inventory.controller;

import com.logistics.inventory.dto.InventoryDtos.SupplierDto;
import com.logistics.inventory.dto.InventoryDtos.SupplierRequest;
import com.logistics.inventory.entity.Supplier;
import com.logistics.inventory.exception.NotFoundException;
import com.logistics.inventory.repository.SupplierRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierRepository supplierRepository;

    @GetMapping
    public List<SupplierDto> list() {
        return supplierRepository.findAll(Sort.by("name")).stream().map(SupplierDto::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierDto create(@Valid @RequestBody SupplierRequest request) {
        Supplier supplier = Supplier.builder()
                .name(request.name())
                .contactEmail(request.contactEmail())
                .phone(request.phone())
                .address(request.address())
                .build();
        supplierRepository.save(supplier);
        return SupplierDto.from(supplier);
    }

    @PutMapping("/{id}")
    public SupplierDto update(@PathVariable Long id, @Valid @RequestBody SupplierRequest request) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Supplier", id));
        supplier.setName(request.name());
        supplier.setContactEmail(request.contactEmail());
        supplier.setPhone(request.phone());
        supplier.setAddress(request.address());
        supplierRepository.save(supplier);
        return SupplierDto.from(supplier);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!supplierRepository.existsById(id)) {
            throw NotFoundException.of("Supplier", id);
        }
        supplierRepository.deleteById(id);
    }
}
