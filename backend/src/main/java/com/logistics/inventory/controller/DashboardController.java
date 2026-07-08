package com.logistics.inventory.controller;

import com.logistics.inventory.dto.InventoryDtos.DashboardStats;
import com.logistics.inventory.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public DashboardStats stats(@RequestParam(required = false) Long warehouseId) {
        return dashboardService.stats(warehouseId);
    }
}
