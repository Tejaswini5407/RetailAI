package com.retailai.dto.forecast;

import java.time.LocalDate;

public class ForecastResponseDto {
    private Long storeId;
    private String storeCode;
    private Long productId;
    private String sku;
    private LocalDate date;
    private Double predictedDemand;
    private Integer roundedDemand;
    private String modelName;
    private String modelVersion;

    public ForecastResponseDto() {}

    public ForecastResponseDto(Long storeId, String storeCode, Long productId, String sku, LocalDate date, Double predictedDemand, Integer roundedDemand, String modelName, String modelVersion) {
        this.storeId = storeId;
        this.storeCode = storeCode;
        this.productId = productId;
        this.sku = sku;
        this.date = date;
        this.predictedDemand = predictedDemand;
        this.roundedDemand = roundedDemand;
        this.modelName = modelName;
        this.modelVersion = modelVersion;
    }

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public String getStoreCode() { return storeCode; }
    public void setStoreCode(String storeCode) { this.storeCode = storeCode; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public Double getPredictedDemand() { return predictedDemand; }
    public void setPredictedDemand(Double predictedDemand) { this.predictedDemand = predictedDemand; }

    public Integer getRoundedDemand() { return roundedDemand; }
    public void setRoundedDemand(Integer roundedDemand) { this.roundedDemand = roundedDemand; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
}
