package com.retailai.dto.forecast;

import java.util.List;
import java.util.Map;

public class ForecastExplanationResponseDto {
    private Long storeId;
    private String storeCode;
    private Long productId;
    private String sku;
    private Double predictedDemand;
    private List<Map<String, Object>> factors;
    private Map<String, Object> inventoryFactors;

    public ForecastExplanationResponseDto() {}

    public ForecastExplanationResponseDto(Long storeId, String storeCode, Long productId, String sku, Double predictedDemand, List<Map<String, Object>> factors, Map<String, Object> inventoryFactors) {
        this.storeId = storeId;
        this.storeCode = storeCode;
        this.productId = productId;
        this.sku = sku;
        this.predictedDemand = predictedDemand;
        this.factors = factors;
        this.inventoryFactors = inventoryFactors;
    }

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public String getStoreCode() { return storeCode; }
    public void setStoreCode(String storeCode) { this.storeCode = storeCode; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public Double getPredictedDemand() { return predictedDemand; }
    public void setPredictedDemand(Double predictedDemand) { this.predictedDemand = predictedDemand; }

    public List<Map<String, Object>> getFactors() { return factors; }
    public void setFactors(List<Map<String, Object>> factors) { this.factors = factors; }

    public Map<String, Object> getInventoryFactors() { return inventoryFactors; }
    public void setInventoryFactors(Map<String, Object> inventoryFactors) { this.inventoryFactors = inventoryFactors; }
}
