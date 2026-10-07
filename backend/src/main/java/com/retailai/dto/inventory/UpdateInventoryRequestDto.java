package com.retailai.dto.inventory;

import jakarta.validation.constraints.Min;

public class UpdateInventoryRequestDto {

    @Min(value = 0, message = "Quantity on hand cannot be negative")
    private Integer quantityOnHand;

    @Min(value = 0, message = "Quantity on order cannot be negative")
    private Integer quantityOnOrder;

    public UpdateInventoryRequestDto() {}

    public UpdateInventoryRequestDto(Integer quantityOnHand, Integer quantityOnOrder) {
        this.quantityOnHand = quantityOnHand;
        this.quantityOnOrder = quantityOnOrder;
    }

    public Integer getQuantityOnHand() { return quantityOnHand; }
    public void setQuantityOnHand(Integer quantityOnHand) { this.quantityOnHand = quantityOnHand; }

    public Integer getQuantityOnOrder() { return quantityOnOrder; }
    public void setQuantityOnOrder(Integer quantityOnOrder) { this.quantityOnOrder = quantityOnOrder; }
}
