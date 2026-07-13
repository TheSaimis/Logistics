package com.logistics.inventory.controller;

import com.logistics.inventory.dto.InventoryDtos.*;
import com.logistics.inventory.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @GetMapping("/product/{productId}")
    public List<StockLevelDto> byProduct(@PathVariable Long productId) {
        return stockService.levelsForProduct(productId);
    }

    @GetMapping("/low")
    public List<StockLevelDto> lowStock() {
        return stockService.lowStock();
    }

    @GetMapping("/movements")
    public Page<StockMovementDto> movements(@RequestParam(required = false) Long productId,
                                            @PageableDefault(size = 20, sort = "createdAt",
                                                    direction = Sort.Direction.DESC) Pageable pageable) {
        return stockService.movements(productId, pageable);
    }

    @PostMapping("/movements")
    @ResponseStatus(HttpStatus.CREATED)
    public StockMovementDto record(@Valid @RequestBody StockMovementRequest request,
                                   Authentication authentication) {
        return stockService.recordMovement(request, authentication.getName());
    }

    @PatchMapping("/levels/{id}")
    public StockLevelDto updateLevelSettings(@PathVariable Long id,
                                             @Valid @RequestBody StockLevelSettingsRequest request) {
        return stockService.updateLevelSettings(id, request);
    }

    @PostMapping("/stocktake")
    public StocktakeResult stocktake(@Valid @RequestBody StocktakeRequest request,
                                     Authentication authentication) {
        return stockService.stocktake(request, authentication.getName());
    }

    @GetMapping("/reorder-suggestions")
    public List<ReorderSuggestion> reorderSuggestions() {
        return stockService.reorderSuggestions();
    }
}
