package com.retailai.dto.security;

import com.retailai.domain.UserRole;

public class SecurityUserDto {

    private Long userId;
    private String email;
    private String name;
    private Long organizationId;
    private String organizationCode;
    private UserRole role;

    public SecurityUserDto() {}

    public SecurityUserDto(Long userId, String email, String name, Long organizationId, String organizationCode, UserRole role) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.organizationId = organizationId;
        this.organizationCode = organizationCode;
        this.role = role;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }

    public String getOrganizationCode() { return organizationCode; }
    public void setOrganizationCode(String organizationCode) { this.organizationCode = organizationCode; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
}
