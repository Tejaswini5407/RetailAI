package com.retailai.dto.user;

import com.retailai.domain.UserRole;
import java.time.OffsetDateTime;

public class UserResponseDto {
    private Long id;
    private String email;
    private String name;
    private Long organizationId;
    private String organizationCode;
    private UserRole role;
    private OffsetDateTime createdAt;

    public UserResponseDto() {}

    public UserResponseDto(Long id, String email, String name, Long organizationId, String organizationCode, UserRole role, OffsetDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.organizationId = organizationId;
        this.organizationCode = organizationCode;
        this.role = role;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
