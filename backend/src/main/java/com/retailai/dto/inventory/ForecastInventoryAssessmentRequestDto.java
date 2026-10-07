package com.retailai.dto.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public class ForecastInventoryAssessmentRequestDto {

    @NotNull(message = "Store ID is required")
    private Long storeId;

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotNull(message = "Forecast horizon days is required")
    @Positive(message = "Forecast horizon days must be positive")
    private Integer forecastHorizonDays;

    @Min(value = 1, message = "Lead time days must be positive")
    private Integer leadTimeDays = 7;

    private Integer safetyStock;
    private Integer existingOnOrder;
    private Integer moq;

    // Optional ML features if client wants to provide specific contextual feature values
    private Double itemPrice;

    public ForecastInventoryAssessmentRequestDto() {}

    public ForecastInventoryAssessmentRequestDto(Long storeId, Long productId, LocalDate date, Integer forecastHorizonDays, Integer leadTimeDays, Integer safetyStock, Integer existingOnOrder, Integer moq, Double itemPrice) {
        this.storeId = storeId;
        this.productId = productId;
        this.date = date;
        this.forecastHorizonDays = forecastHorizonDays;
        if (leadTimeDays != null) this.leadTimeDays = leadTimeDays;
        this.safetyStock = safetyStock;
        this.existingOnOrder = existingOnOrder;
        this.moq = moq;
        this.itemPrice = itemPrice;
    }

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

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

    public Double getItemPrice() { return itemPrice; }
    public void setItemPrice(Double itemPrice) { this.itemPrice = itemPrice; }
}
