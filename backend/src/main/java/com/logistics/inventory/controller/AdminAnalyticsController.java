package com.logistics.inventory.controller;

import com.logistics.inventory.dto.AnalyticsDtos.AnalyticsResponse;
import com.logistics.inventory.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Under /api/admin so the ADMIN role rule in SecurityConfig applies. */
@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
public class AdminAnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping
    public AnalyticsResponse analytics() {
        return analyticsService.analytics();
    }
}
