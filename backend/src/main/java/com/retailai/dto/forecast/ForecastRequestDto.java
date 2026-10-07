package com.retailai.dto.forecast;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Map;

public class ForecastRequestDto {

    @NotNull(message = "Store ID is required")
    private Long storeId;

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Date is required")
    private LocalDate date;

    private Map<String, Double> features;

    public ForecastRequestDto() {}

    public ForecastRequestDto(Long storeId, Long productId, LocalDate date, Map<String, Double> features) {
        this.storeId = storeId;
        this.productId = productId;
        this.date = date;
        this.features = features;
    }

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public Map<String, Double> getFeatures() { return features; }
    public void setFeatures(Map<String, Double> features) { this.features = features; }
}
