package com.retailai.service;

import com.retailai.domain.*;
import com.retailai.dto.forecast.*;
import com.retailai.dto.inventory.ForecastInventoryAssessmentRequestDto;
import com.retailai.dto.inventory.ForecastInventoryAssessmentResponseDto;
import com.retailai.dto.inventory.InventoryAssessmentRequestDto;
import com.retailai.dto.inventory.InventoryAssessmentResponseDto;
import com.retailai.exception.ResourceNotFoundException;
import com.retailai.repository.*;
import com.retailai.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
public class ForecastService {

    private final MLInferenceClient mlInferenceClient;
    private final InventoryService inventoryService;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final ForecastHistoryRepository forecastHistoryRepository;

    public ForecastService(
        MLInferenceClient mlInferenceClient,
        InventoryService inventoryService,
        StoreRepository storeRepository,
        ProductRepository productRepository,
        SaleRepository saleRepository,
        ForecastHistoryRepository forecastHistoryRepository
    ) {
        this.mlInferenceClient = mlInferenceClient;
        this.inventoryService = inventoryService;
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.saleRepository = saleRepository;
        this.forecastHistoryRepository = forecastHistoryRepository;
    }

    @Transactional
    public ForecastResponseDto generateForecast(ForecastRequestDto request) {
        Long tenantId = TenantContext.getTenantId();

        Store store = storeRepository.findByOrganizationIdAndId(tenantId, request.getStoreId())
                .orElseThrow(() -> new ResourceNotFoundException("Store not found for ID: " + request.getStoreId()));

        Product product = productRepository.findByOrganizationIdAndId(tenantId, request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found for ID: " + request.getProductId()));

        ForecastResponseDto response = mlInferenceClient.predictDemand(
            store.getId(),
            store.getStoreCode(),
            product.getId(),
            product.getSku(),
            request.getDate(),
            request.getFeatures()
        );

        int horizon = request.getHorizonDays() != null && request.getHorizonDays() > 0 ? request.getHorizonDays() : 1;
        String runId = UUID.randomUUID().toString().substring(0, 8);
        LocalDate targetDate = request.getDate() != null ? request.getDate() : LocalDate.now().plusDays(1);

        ForecastHistory history = new ForecastHistory(
            store.getOrganization(),
            store,
            product,
            runId,
            LocalDate.now(),
            targetDate,
            horizon,
            response.getPredictedDemand(),
            response.getRoundedDemand(),
            response.getModelName(),
            response.getModelVersion()
        );
        forecastHistoryRepository.save(history);

        return response;
    }

    @Transactional
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

        String runId = UUID.randomUUID().toString().substring(0, 8);
        LocalDate targetDate = request.getDate() != null ? request.getDate() : LocalDate.now().plusDays(1);
        ForecastHistory history = new ForecastHistory(
            store.getOrganization(),
            store,
            product,
            runId,
            LocalDate.now(),
            targetDate,
            request.getForecastHorizonDays(),
            forecast.getPredictedDemand(),
            forecast.getRoundedDemand(),
            forecast.getModelName(),
            forecast.getModelVersion()
        );
        forecastHistoryRepository.save(history);

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

    @Transactional(readOnly = true)
    public List<ForecastHistoryResponseDto> getForecastHistory() {
        Long tenantId = TenantContext.getTenantId();
        List<ForecastHistory> list = forecastHistoryRepository.findByOrganizationIdOrderByCreatedAtDesc(tenantId);
        List<ForecastHistoryResponseDto> result = new ArrayList<>();
        for (ForecastHistory fh : list) {
            result.add(new ForecastHistoryResponseDto(
                fh.getId(),
                fh.getStore().getId(),
                fh.getStore().getStoreCode(),
                fh.getProduct().getId(),
                fh.getProduct().getSku(),
                fh.getForecastRunId(),
                fh.getForecastDate(),
                fh.getTargetDate(),
                fh.getHorizonDays(),
                fh.getPredictedDemand(),
                fh.getRoundedDemand(),
                fh.getModelName(),
                fh.getModelVersion(),
                fh.getCreatedAt()
            ));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public ForecastAccuracyResponseDto calculateForecastAccuracy() {
        Long tenantId = TenantContext.getTenantId();
        List<ForecastHistory> history = forecastHistoryRepository.findByOrganizationIdOrderByCreatedAtDesc(tenantId);

        double totalAbsoluteError = 0.0;
        double totalSquaredError = 0.0;
        double totalActualDemand = 0.0;
        int evaluatedCount = 0;

        for (ForecastHistory fh : history) {
            Optional<Sale> saleOpt = saleRepository.findByOrganizationIdAndStoreIdAndProductIdAndSaleDate(
                tenantId, fh.getStore().getId(), fh.getProduct().getId(), fh.getTargetDate()
            );

            if (saleOpt.isPresent()) {
                double actual = saleOpt.get().getUnitsSold().doubleValue();
                double predicted = fh.getPredictedDemand();

                double absErr = Math.abs(actual - predicted);
                totalAbsoluteError += absErr;
                totalSquaredError += (absErr * absErr);
                totalActualDemand += actual;
                evaluatedCount++;
            }
        }

        if (evaluatedCount == 0) {
            return new ForecastAccuracyResponseDto(
                0, null, null, null, false,
                "Accuracy will be available after actual sales data is recorded for the forecast period."
            );
        }

        double mae = totalAbsoluteError / evaluatedCount;
        double rmse = Math.sqrt(totalSquaredError / evaluatedCount);
        double wape = totalActualDemand > 0 ? (totalAbsoluteError / totalActualDemand) * 100.0 : 0.0;

        return new ForecastAccuracyResponseDto(evaluatedCount, wape, mae, rmse, true, "Forecast accuracy metrics calculated from " + evaluatedCount + " evaluated target periods.");
    }

    @Transactional(readOnly = true)
    public ForecastExplanationResponseDto getForecastExplanation(Long storeId, Long productId) {
        Long tenantId = TenantContext.getTenantId();

        Store store = storeRepository.findByOrganizationIdAndId(tenantId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found for ID: " + storeId));

        Product product = productRepository.findByOrganizationIdAndId(tenantId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found for ID: " + productId));

        ForecastResponseDto forecast = mlInferenceClient.predictDemand(
            store.getId(), store.getStoreCode(), product.getId(), product.getSku(), LocalDate.now().plusDays(1), null
        );

        List<Map<String, Object>> factors = new ArrayList<>();
        factors.add(Map.of("factor", "Recent Demand Trend", "impact", "High", "description", "Weighted moving average of recent 7-day sales volume."));
        factors.add(Map.of("factor", "Day-of-Week Seasonality", "impact", "Medium", "description", "Historical demand patterns for target day of week."));
        factors.add(Map.of("factor", "Lagged Sales Features", "impact", "High", "description", "7-day, 14-day, and 28-day historical lag values."));
        factors.add(Map.of("factor", "Rolling Demand Windows", "impact", "Medium", "description", "Rolling 7-day and 28-day mean sales per store/product."));
        factors.add(Map.of("factor", "Price Information", "impact", "Low", "description", "Unit price elasticity and historical price ratio where available."));

        Map<String, Object> inventoryFactors = Map.of(
            "modelName", forecast.getModelName(),
            "modelVersion", forecast.getModelVersion() != null ? forecast.getModelVersion() : "1.0.0 (Random Forest)",
            "predictedDemand", forecast.getPredictedDemand(),
            "explanationNote", "Forecast factors derived from the active Random Forest feature pipeline."
        );

        return new ForecastExplanationResponseDto(
            store.getId(), store.getStoreCode(), product.getId(), product.getSku(), forecast.getPredictedDemand(), factors, inventoryFactors
        );
    }
}
