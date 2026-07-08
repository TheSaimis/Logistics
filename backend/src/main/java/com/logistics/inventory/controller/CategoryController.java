package com.logistics.inventory.controller;

import com.logistics.inventory.dto.InventoryDtos.CategoryDto;
import com.logistics.inventory.dto.InventoryDtos.CategoryRequest;
import com.logistics.inventory.entity.Category;
import com.logistics.inventory.exception.BadRequestException;
import com.logistics.inventory.exception.NotFoundException;
import com.logistics.inventory.repository.CategoryRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;

    @GetMapping
    public List<CategoryDto> list() {
        return categoryRepository.findAll(Sort.by("name")).stream().map(CategoryDto::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto create(@Valid @RequestBody CategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw new BadRequestException("Category already exists: " + request.name());
        }
        Category category = Category.builder()
                .name(request.name())
                .description(request.description())
                .build();
        categoryRepository.save(category);
        return CategoryDto.from(category);
    }

    @PutMapping("/{id}")
    public CategoryDto update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Category", id));
        category.setName(request.name());
        category.setDescription(request.description());
        categoryRepository.save(category);
        return CategoryDto.from(category);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!categoryRepository.existsById(id)) {
            throw NotFoundException.of("Category", id);
        }
        categoryRepository.deleteById(id);
    }
}
