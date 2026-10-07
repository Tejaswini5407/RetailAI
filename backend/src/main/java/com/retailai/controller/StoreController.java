package com.retailai.controller;

import com.retailai.dto.store.CreateStoreRequestDto;
import com.retailai.dto.store.StoreResponseDto;
import com.retailai.dto.store.UpdateStoreRequestDto;
import com.retailai.service.StoreService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stores")
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER')")
    public ResponseEntity<StoreResponseDto> createStore(@Valid @RequestBody CreateStoreRequestDto request) {
        StoreResponseDto dto = storeService.createStore(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'INVENTORY_MANAGER')")
    public ResponseEntity<Page<StoreResponseDto>> getStores(Pageable pageable) {
        Page<StoreResponseDto> stores = storeService.getStores(pageable);
        return ResponseEntity.ok(stores);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'INVENTORY_MANAGER')")
    public ResponseEntity<StoreResponseDto> getStoreById(@PathVariable Long id) {
        StoreResponseDto dto = storeService.getStoreById(id);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER')")
    public ResponseEntity<StoreResponseDto> updateStore(
        @PathVariable Long id,
        @Valid @RequestBody UpdateStoreRequestDto request
    ) {
        StoreResponseDto dto = storeService.updateStore(id, request);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteStore(@PathVariable Long id) {
        storeService.deleteStore(id);
        return ResponseEntity.noContent().build();
    }
}
