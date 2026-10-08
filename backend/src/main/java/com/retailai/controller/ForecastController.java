package com.retailai.controller;

import com.retailai.dto.forecast.*;
import com.retailai.service.ForecastService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/forecasts")
public class ForecastController {

    private final ForecastService forecastService;

    public ForecastController(ForecastService forecastService) {
        this.forecastService = forecastService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'INVENTORY_MANAGER')")
    public ResponseEntity<ForecastResponseDto> generateForecast(@Valid @RequestBody ForecastRequestDto request) {
        ForecastResponseDto dto = forecastService.generateForecast(request);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'INVENTORY_MANAGER')")
    public ResponseEntity<List<ForecastHistoryResponseDto>> getForecastHistory() {
        List<ForecastHistoryResponseDto> list = forecastService.getForecastHistory();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/accuracy")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'INVENTORY_MANAGER')")
    public ResponseEntity<ForecastAccuracyResponseDto> getForecastAccuracy() {
        ForecastAccuracyResponseDto accuracy = forecastService.calculateForecastAccuracy();
        return ResponseEntity.ok(accuracy);
    }

    @GetMapping("/explanation")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'INVENTORY_MANAGER')")
    public ResponseEntity<ForecastExplanationResponseDto> getForecastExplanation(
            @RequestParam("storeId") Long storeId,
            @RequestParam("productId") Long productId
    ) {
        ForecastExplanationResponseDto explanation = forecastService.getForecastExplanation(storeId, productId);
        return ResponseEntity.ok(explanation);
    }
}
