package com.retailai.dto.store;

import java.time.OffsetDateTime;

public class StoreResponseDto {
    private Long id;
    private String storeCode;
    private String name;
    private String state;
    private String city;
    private Long organizationId;
    private OffsetDateTime createdAt;

    public StoreResponseDto() {}

    public StoreResponseDto(Long id, String storeCode, String name, String state, String city, Long organizationId, OffsetDateTime createdAt) {
        this.id = id;
        this.storeCode = storeCode;
        this.name = name;
        this.state = state;
        this.city = city;
        this.organizationId = organizationId;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStoreCode() { return storeCode; }
    public void setStoreCode(String storeCode) { this.storeCode = storeCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
