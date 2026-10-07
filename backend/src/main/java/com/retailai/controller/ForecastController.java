package com.retailai.controller;

import com.retailai.dto.forecast.ForecastRequestDto;
import com.retailai.dto.forecast.ForecastResponseDto;
import com.retailai.service.ForecastService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
}
