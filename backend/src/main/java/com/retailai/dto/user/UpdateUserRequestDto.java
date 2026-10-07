package com.retailai.dto.user;

import com.retailai.domain.UserRole;
import jakarta.validation.constraints.Size;

public class UpdateUserRequestDto {

    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;

    private UserRole role;

    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    public UpdateUserRequestDto() {}

    public UpdateUserRequestDto(String name, UserRole role, String password) {
        this.name = name;
        this.role = role;
        this.password = password;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
