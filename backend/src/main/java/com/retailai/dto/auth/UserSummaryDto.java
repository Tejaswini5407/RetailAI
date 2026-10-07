package com.retailai.dto.auth;

import com.retailai.domain.UserRole;

public class UserSummaryDto {

    private Long id;
    private String email;
    private String name;
    private Long organizationId;
    private String organizationCode;
    private UserRole role;

    public UserSummaryDto() {}

    public UserSummaryDto(Long id, String email, String name, Long organizationId, String organizationCode, UserRole role) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.organizationId = organizationId;
        this.organizationCode = organizationCode;
        this.role = role;
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
}
