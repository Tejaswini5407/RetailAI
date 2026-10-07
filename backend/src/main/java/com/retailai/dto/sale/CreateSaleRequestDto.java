package com.retailai.dto.sale;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

public class CreateSaleRequestDto {

    @NotNull(message = "Store ID is required")
    private Long storeId;

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Sale date is required")
    private LocalDate saleDate;

    @NotNull(message = "Units sold is required")
    @Min(value = 0, message = "Units sold cannot be negative")
    private Integer unitsSold;

    @PositiveOrZero(message = "Unit price must be non-negative")
    private BigDecimal unitPrice;

    public CreateSaleRequestDto() {}

    public CreateSaleRequestDto(Long storeId, Long productId, LocalDate saleDate, Integer unitsSold, BigDecimal unitPrice) {
        this.storeId = storeId;
        this.productId = productId;
        this.saleDate = saleDate;
        this.unitsSold = unitsSold;
        this.unitPrice = unitPrice;
    }

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public LocalDate getSaleDate() { return saleDate; }
    public void setSaleDate(LocalDate saleDate) { this.saleDate = saleDate; }

    public Integer getUnitsSold() { return unitsSold; }
    public void setUnitsSold(Integer unitsSold) { this.unitsSold = unitsSold; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
}
