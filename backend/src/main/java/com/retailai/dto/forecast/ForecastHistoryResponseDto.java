package com.retailai.dto.forecast;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public class ForecastHistoryResponseDto {
    private Long id;
    private Long storeId;
    private String storeCode;
    private Long productId;
    private String sku;
    private String forecastRunId;
    private LocalDate forecastDate;
    private LocalDate targetDate;
    private Integer horizonDays;
    private Double predictedDemand;
    private Integer roundedDemand;
    private String modelName;
    private String modelVersion;
    private OffsetDateTime createdAt;

    public ForecastHistoryResponseDto() {}

    public ForecastHistoryResponseDto(Long id, Long storeId, String storeCode, Long productId, String sku, String forecastRunId, LocalDate forecastDate, LocalDate targetDate, Integer horizonDays, Double predictedDemand, Integer roundedDemand, String modelName, String modelVersion, OffsetDateTime createdAt) {
        this.id = id;
        this.storeId = storeId;
        this.storeCode = storeCode;
        this.productId = productId;
        this.sku = sku;
        this.forecastRunId = forecastRunId;
        this.forecastDate = forecastDate;
        this.targetDate = targetDate;
        this.horizonDays = horizonDays;
        this.predictedDemand = predictedDemand;
        this.roundedDemand = roundedDemand;
        this.modelName = modelName;
        this.modelVersion = modelVersion;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public String getStoreCode() { return storeCode; }
    public void setStoreCode(String storeCode) { this.storeCode = storeCode; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getForecastRunId() { return forecastRunId; }
    public void setForecastRunId(String forecastRunId) { this.forecastRunId = forecastRunId; }

    public LocalDate getForecastDate() { return forecastDate; }
    public void setForecastDate(LocalDate forecastDate) { this.forecastDate = forecastDate; }

    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }

    public Integer getHorizonDays() { return horizonDays; }
    public void setHorizonDays(Integer horizonDays) { this.horizonDays = horizonDays; }

    public Double getPredictedDemand() { return predictedDemand; }
    public void setPredictedDemand(Double predictedDemand) { this.predictedDemand = predictedDemand; }

    public Integer getRoundedDemand() { return roundedDemand; }
    public void setRoundedDemand(Integer roundedDemand) { this.roundedDemand = roundedDemand; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
