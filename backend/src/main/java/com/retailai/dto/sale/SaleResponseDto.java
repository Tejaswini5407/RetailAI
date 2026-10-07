package com.retailai.dto.sale;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public class SaleResponseDto {
    private Long id;
    private Long storeId;
    private String storeCode;
    private Long productId;
    private String sku;
    private LocalDate saleDate;
    private Integer unitsSold;
    private BigDecimal unitPrice;
    private Long organizationId;
    private OffsetDateTime createdAt;

    public SaleResponseDto() {}

    public SaleResponseDto(Long id, Long storeId, String storeCode, Long productId, String sku, LocalDate saleDate, Integer unitsSold, BigDecimal unitPrice, Long organizationId, OffsetDateTime createdAt) {
        this.id = id;
        this.storeId = storeId;
        this.storeCode = storeCode;
        this.productId = productId;
        this.sku = sku;
        this.saleDate = saleDate;
        this.unitsSold = unitsSold;
        this.unitPrice = unitPrice;
        this.organizationId = organizationId;
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

    public LocalDate getSaleDate() { return saleDate; }
    public void setSaleDate(LocalDate saleDate) { this.saleDate = saleDate; }

    public Integer getUnitsSold() { return unitsSold; }
    public void setUnitsSold(Integer unitsSold) { this.unitsSold = unitsSold; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
