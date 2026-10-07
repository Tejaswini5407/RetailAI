package com.retailai.dto.store;

import jakarta.validation.constraints.Size;

public class UpdateStoreRequestDto {

    @Size(max = 50, message = "Store code cannot exceed 50 characters")
    private String storeCode;

    @Size(max = 100, message = "Store name cannot exceed 100 characters")
    private String name;

    @Size(max = 10, message = "State cannot exceed 10 characters")
    private String state;

    @Size(max = 50, message = "City cannot exceed 50 characters")
    private String city;

    public UpdateStoreRequestDto() {}

    public UpdateStoreRequestDto(String storeCode, String name, String state, String city) {
        this.storeCode = storeCode;
        this.name = name;
        this.state = state;
        this.city = city;
    }

    public String getStoreCode() { return storeCode; }
    public void setStoreCode(String storeCode) { this.storeCode = storeCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
}
