package com.retailai.dto.product;

import java.time.OffsetDateTime;

public class ProductResponseDto {
    private Long id;
    private String sku;
    private String name;
    private String category;
    private String department;
    private Long organizationId;
    private OffsetDateTime createdAt;

    public ProductResponseDto() {}

    public ProductResponseDto(Long id, String sku, String name, String category, String department, Long organizationId, OffsetDateTime createdAt) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.department = department;
        this.organizationId = organizationId;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
