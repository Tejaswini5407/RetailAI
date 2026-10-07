package com.retailai.dto.inventory;

public class InventoryAssessmentResponseDto {
    private Long storeId;
    private String storeCode;
    private Long productId;
    private String sku;
    private Double totalForecastDemand;
    private Double averageDailyDemand;
    private Integer forecastHorizonDays;
    private Integer leadTimeDays;
    private Double leadTimeDemand;
    private Integer safetyStock;
    private Integer reorderPoint;
    private Integer currentInventory;
    private Integer existingOnOrder;
    private String stockoutRisk; // HIGH, MEDIUM, LOW
    private String inventoryStatus; // OUT_OF_STOCK, BELOW_REORDER_POINT, ADEQUATE
    private Integer recommendedOrderQuantity;

    public InventoryAssessmentResponseDto() {}

    public InventoryAssessmentResponseDto(Long storeId, String storeCode, Long productId, String sku, Double totalForecastDemand, Double averageDailyDemand, Integer forecastHorizonDays, Integer leadTimeDays, Double leadTimeDemand, Integer safetyStock, Integer reorderPoint, Integer currentInventory, Integer existingOnOrder, String stockoutRisk, String inventoryStatus, Integer recommendedOrderQuantity) {
        this.storeId = storeId;
        this.storeCode = storeCode;
        this.productId = productId;
        this.sku = sku;
        this.totalForecastDemand = totalForecastDemand;
        this.averageDailyDemand = averageDailyDemand;
        this.forecastHorizonDays = forecastHorizonDays;
        this.leadTimeDays = leadTimeDays;
        this.leadTimeDemand = leadTimeDemand;
        this.safetyStock = safetyStock;
        this.reorderPoint = reorderPoint;
        this.currentInventory = currentInventory;
        this.existingOnOrder = existingOnOrder;
        this.stockoutRisk = stockoutRisk;
        this.inventoryStatus = inventoryStatus;
        this.recommendedOrderQuantity = recommendedOrderQuantity;
    }

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public String getStoreCode() { return storeCode; }
    public void setStoreCode(String storeCode) { this.storeCode = storeCode; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public Double getTotalForecastDemand() { return totalForecastDemand; }
    public void setTotalForecastDemand(Double totalForecastDemand) { this.totalForecastDemand = totalForecastDemand; }

    public Double getAverageDailyDemand() { return averageDailyDemand; }
    public void setAverageDailyDemand(Double averageDailyDemand) { this.averageDailyDemand = averageDailyDemand; }

    public Integer getForecastHorizonDays() { return forecastHorizonDays; }
    public void setForecastHorizonDays(Integer forecastHorizonDays) { this.forecastHorizonDays = forecastHorizonDays; }

    public Integer getLeadTimeDays() { return leadTimeDays; }
    public void setLeadTimeDays(Integer leadTimeDays) { this.leadTimeDays = leadTimeDays; }

    public Double getLeadTimeDemand() { return leadTimeDemand; }
    public void setLeadTimeDemand(Double leadTimeDemand) { this.leadTimeDemand = leadTimeDemand; }

    public Integer getSafetyStock() { return safetyStock; }
    public void setSafetyStock(Integer safetyStock) { this.safetyStock = safetyStock; }

    public Integer getReorderPoint() { return reorderPoint; }
    public void setReorderPoint(Integer reorderPoint) { this.reorderPoint = reorderPoint; }

    public Integer getCurrentInventory() { return currentInventory; }
    public void setCurrentInventory(Integer currentInventory) { this.currentInventory = currentInventory; }

    public Integer getExistingOnOrder() { return existingOnOrder; }
    public void setExistingOnOrder(Integer existingOnOrder) { this.existingOnOrder = existingOnOrder; }

    public String getStockoutRisk() { return stockoutRisk; }
    public void setStockoutRisk(String stockoutRisk) { this.stockoutRisk = stockoutRisk; }

    public String getInventoryStatus() { return inventoryStatus; }
    public void setInventoryStatus(String inventoryStatus) { this.inventoryStatus = inventoryStatus; }

    public Integer getRecommendedOrderQuantity() { return recommendedOrderQuantity; }
    public void setRecommendedOrderQuantity(Integer recommendedOrderQuantity) { this.recommendedOrderQuantity = recommendedOrderQuantity; }
}
