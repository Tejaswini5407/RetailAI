package com.retailai.dto.product;

import jakarta.validation.constraints.Size;

public class UpdateProductRequestDto {

    @Size(max = 50, message = "SKU cannot exceed 50 characters")
    private String sku;

    @Size(max = 100, message = "Product name cannot exceed 100 characters")
    private String name;

    @Size(max = 50, message = "Category cannot exceed 50 characters")
    private String category;

    @Size(max = 50, message = "Department cannot exceed 50 characters")
    private String department;

    public UpdateProductRequestDto() {}

    public UpdateProductRequestDto(String sku, String name, String category, String department) {
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.department = department;
    }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
}
