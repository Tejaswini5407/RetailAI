package com.retailai.controller;

import com.retailai.dto.inventory.*;
import com.retailai.service.ForecastService;
import com.retailai.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;
    private final ForecastService forecastService;

    public InventoryController(InventoryService inventoryService, ForecastService forecastService) {
        this.inventoryService = inventoryService;
        this.forecastService = forecastService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER')")
    public ResponseEntity<InventoryResponseDto> createInventory(@Valid @RequestBody CreateInventoryRequestDto request) {
        InventoryResponseDto dto = inventoryService.createInventory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'INVENTORY_MANAGER')")
    public ResponseEntity<Page<InventoryResponseDto>> getInventory(
        @RequestParam(required = false) Long storeId,
        @RequestParam(required = false) Long productId,
        Pageable pageable
    ) {
        Page<InventoryResponseDto> inventory = inventoryService.getInventory(storeId, productId, pageable);
        return ResponseEntity.ok(inventory);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'INVENTORY_MANAGER')")
    public ResponseEntity<InventoryResponseDto> getInventoryById(@PathVariable Long id) {
        InventoryResponseDto dto = inventoryService.getInventoryById(id);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER')")
    public ResponseEntity<InventoryResponseDto> updateInventory(
        @PathVariable Long id,
        @Valid @RequestBody UpdateInventoryRequestDto request
    ) {
        InventoryResponseDto dto = inventoryService.updateInventory(id, request);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/assess")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'INVENTORY_MANAGER')")
    public ResponseEntity<InventoryAssessmentResponseDto> assessInventory(@Valid @RequestBody InventoryAssessmentRequestDto request) {
        InventoryAssessmentResponseDto response = inventoryService.assessInventory(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forecast-assessment")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'INVENTORY_MANAGER')")
    public ResponseEntity<ForecastInventoryAssessmentResponseDto> assessForecastInventory(@Valid @RequestBody ForecastInventoryAssessmentRequestDto request) {
        ForecastInventoryAssessmentResponseDto response = forecastService.generateForecastAndInventoryAssessment(request);
        return ResponseEntity.ok(response);
    }
}
