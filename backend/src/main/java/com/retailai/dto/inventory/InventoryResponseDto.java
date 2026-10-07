package com.retailai.dto.inventory;

import java.time.OffsetDateTime;

public class InventoryResponseDto {
    private Long id;
    private Long storeId;
    private String storeCode;
    private Long productId;
    private String sku;
    private Integer quantityOnHand;
    private Integer quantityOnOrder;
    private Long organizationId;
    private OffsetDateTime updatedAt;

    public InventoryResponseDto() {}

    public InventoryResponseDto(Long id, Long storeId, String storeCode, Long productId, String sku, Integer quantityOnHand, Integer quantityOnOrder, Long organizationId, OffsetDateTime updatedAt) {
        this.id = id;
        this.storeId = storeId;
        this.storeCode = storeCode;
        this.productId = productId;
        this.sku = sku;
        this.quantityOnHand = quantityOnHand;
        this.quantityOnOrder = quantityOnOrder;
        this.organizationId = organizationId;
        this.updatedAt = updatedAt;
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

    public Integer getQuantityOnHand() { return quantityOnHand; }
    public void setQuantityOnHand(Integer quantityOnHand) { this.quantityOnHand = quantityOnHand; }

    public Integer getQuantityOnOrder() { return quantityOnOrder; }
    public void setQuantityOnOrder(Integer quantityOnOrder) { this.quantityOnOrder = quantityOnOrder; }

    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
