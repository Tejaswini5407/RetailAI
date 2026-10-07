package com.retailai.service;

import com.retailai.domain.Product;
import com.retailai.domain.Store;
import com.retailai.dto.forecast.ForecastRequestDto;
import com.retailai.dto.forecast.ForecastResponseDto;
import com.retailai.dto.inventory.ForecastInventoryAssessmentRequestDto;
import com.retailai.dto.inventory.ForecastInventoryAssessmentResponseDto;
import com.retailai.dto.inventory.InventoryAssessmentRequestDto;
import com.retailai.dto.inventory.InventoryAssessmentResponseDto;
import com.retailai.exception.ResourceNotFoundException;
import com.retailai.repository.ProductRepository;
import com.retailai.repository.StoreRepository;
import com.retailai.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ForecastService {

    private final MLInferenceClient mlInferenceClient;
    private final InventoryService inventoryService;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;

    public ForecastService(
        MLInferenceClient mlInferenceClient,
        InventoryService inventoryService,
        StoreRepository storeRepository,
        ProductRepository productRepository
    ) {
        this.mlInferenceClient = mlInferenceClient;
        this.inventoryService = inventoryService;
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public ForecastResponseDto generateForecast(ForecastRequestDto request) {
        Long tenantId = TenantContext.getTenantId();

        Store store = storeRepository.findByOrganizationIdAndId(tenantId, request.getStoreId())
                .orElseThrow(() -> new ResourceNotFoundException("Store not found for ID: " + request.getStoreId()));

        Product product = productRepository.findByOrganizationIdAndId(tenantId, request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found for ID: " + request.getProductId()));

        return mlInferenceClient.predictDemand(
            store.getId(),
            store.getStoreCode(),
            product.getId(),
            product.getSku(),
            request.getDate(),
            request.getFeatures()
        );
    }

    @Transactional(readOnly = true)
    public ForecastInventoryAssessmentResponseDto generateForecastAndInventoryAssessment(ForecastInventoryAssessmentRequestDto request) {
        Long tenantId = TenantContext.getTenantId();

        Store store = storeRepository.findByOrganizationIdAndId(tenantId, request.getStoreId())
                .orElseThrow(() -> new ResourceNotFoundException("Store not found for ID: " + request.getStoreId()));

        Product product = productRepository.findByOrganizationIdAndId(tenantId, request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found for ID: " + request.getProductId()));

        ForecastResponseDto forecast = mlInferenceClient.predictDemand(
            store.getId(),
            store.getStoreCode(),
            product.getId(),
            product.getSku(),
            request.getDate(),
            null
        );

        InventoryAssessmentRequestDto assessReq = new InventoryAssessmentRequestDto(
            store.getId(),
            product.getId(),
            forecast.getPredictedDemand(),
            request.getForecastHorizonDays(),
            request.getLeadTimeDays(),
            request.getSafetyStock(),
            request.getExistingOnOrder(),
            request.getMoq()
        );

        InventoryAssessmentResponseDto assessment = inventoryService.assessInventory(assessReq);

        return new ForecastInventoryAssessmentResponseDto(forecast, assessment);
    }
}
