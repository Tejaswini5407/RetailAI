package com.retailai.dto.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class InventoryAssessmentRequestDto {

    @NotNull(message = "Store ID is required")
    private Long storeId;

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Forecast demand is required")
    @Min(value = 0, message = "Forecast demand cannot be negative")
    private Double forecastDemand;

    @NotNull(message = "Forecast horizon days is required")
    @Positive(message = "Forecast horizon days must be positive")
    private Integer forecastHorizonDays;

    @Min(value = 1, message = "Lead time days must be positive")
    private Integer leadTimeDays = 7;

    @Min(value = 0, message = "Safety stock cannot be negative")
    private Integer safetyStock;

    @Min(value = 0, message = "Existing on order cannot be negative")
    private Integer existingOnOrder;

    @Min(value = 0, message = "MOQ cannot be negative")
    private Integer moq;

    public InventoryAssessmentRequestDto() {}

    public InventoryAssessmentRequestDto(Long storeId, Long productId, Double forecastDemand, Integer forecastHorizonDays, Integer leadTimeDays, Integer safetyStock, Integer existingOnOrder, Integer moq) {
        this.storeId = storeId;
        this.productId = productId;
        this.forecastDemand = forecastDemand;
        this.forecastHorizonDays = forecastHorizonDays;
        if (leadTimeDays != null) this.leadTimeDays = leadTimeDays;
        this.safetyStock = safetyStock;
        this.existingOnOrder = existingOnOrder;
        this.moq = moq;
    }

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Double getForecastDemand() { return forecastDemand; }
    public void setForecastDemand(Double forecastDemand) { this.forecastDemand = forecastDemand; }

    public Integer getForecastHorizonDays() { return forecastHorizonDays; }
    public void setForecastHorizonDays(Integer forecastHorizonDays) { this.forecastHorizonDays = forecastHorizonDays; }

    public Integer getLeadTimeDays() { return leadTimeDays; }
    public void setLeadTimeDays(Integer leadTimeDays) { this.leadTimeDays = leadTimeDays; }

    public Integer getSafetyStock() { return safetyStock; }
    public void setSafetyStock(Integer safetyStock) { this.safetyStock = safetyStock; }

    public Integer getExistingOnOrder() { return existingOnOrder; }
    public void setExistingOnOrder(Integer existingOnOrder) { this.existingOnOrder = existingOnOrder; }

    public Integer getMoq() { return moq; }
    public void setMoq(Integer moq) { this.moq = moq; }
}
