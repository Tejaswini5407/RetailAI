package com.retailai.controller;

import com.retailai.dto.security.SecurityUserDto;
import com.retailai.security.CustomUserDetails;
import com.retailai.security.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/security")
public class SecurityTestController {

    @GetMapping("/me")
    public ResponseEntity<SecurityUserDto> getCurrentSecurityUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        SecurityUserDto dto = new SecurityUserDto(
            userDetails.getUserId(),
            userDetails.getUsername(),
            userDetails.getUsername(),
            userDetails.getOrganizationId(),
            userDetails.getOrganizationCode(),
            userDetails.getRole()
        );
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/tenant-info")
    public ResponseEntity<SecurityUserDto> getTenantInfoFromContext() {
        SecurityUserDto dto = new SecurityUserDto(
            TenantContext.getCurrentUserId(),
            TenantContext.getCurrentUser().map(CustomUserDetails::getUsername).orElse(null),
            TenantContext.getCurrentUser().map(CustomUserDetails::getUsername).orElse(null),
            TenantContext.getCurrentOrganizationId(),
            TenantContext.getCurrentOrganizationCode(),
            TenantContext.getCurrentRole()
        );
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/admin-only")
    public ResponseEntity<SecurityUserDto> getAdminOnlyInfo(@AuthenticationPrincipal CustomUserDetails userDetails) {
        SecurityUserDto dto = new SecurityUserDto(
            userDetails.getUserId(),
            userDetails.getUsername(),
            userDetails.getUsername(),
            userDetails.getOrganizationId(),
            userDetails.getOrganizationCode(),
            userDetails.getRole()
        );
        return ResponseEntity.ok(dto);
    }
}
