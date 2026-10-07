package com.retailai.dto.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CreateInventoryRequestDto {

    @NotNull(message = "Store ID is required")
    private Long storeId;

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Quantity on hand is required")
    @Min(value = 0, message = "Quantity on hand cannot be negative")
    private Integer quantityOnHand;

    @NotNull(message = "Quantity on order is required")
    @Min(value = 0, message = "Quantity on order cannot be negative")
    private Integer quantityOnOrder;

    public CreateInventoryRequestDto() {}

    public CreateInventoryRequestDto(Long storeId, Long productId, Integer quantityOnHand, Integer quantityOnOrder) {
        this.storeId = storeId;
        this.productId = productId;
        this.quantityOnHand = quantityOnHand;
        this.quantityOnOrder = quantityOnOrder;
    }

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Integer getQuantityOnHand() { return quantityOnHand; }
    public void setQuantityOnHand(Integer quantityOnHand) { this.quantityOnHand = quantityOnHand; }

    public Integer getQuantityOnOrder() { return quantityOnOrder; }
    public void setQuantityOnOrder(Integer quantityOnOrder) { this.quantityOnOrder = quantityOnOrder; }
}
