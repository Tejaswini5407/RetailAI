package com.retailai.security;

import com.retailai.domain.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class TenantContext {

    private TenantContext() {}

    public static Optional<CustomUserDetails> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            return Optional.of(userDetails);
        }
        return Optional.empty();
    }

    public static Long getCurrentOrganizationId() {
        return getCurrentUser()
                .map(CustomUserDetails::getOrganizationId)
                .orElseThrow(() -> new IllegalStateException("No authenticated tenant found in security context"));
    }

    public static String getCurrentOrganizationCode() {
        return getCurrentUser()
                .map(CustomUserDetails::getOrganizationCode)
                .orElseThrow(() -> new IllegalStateException("No authenticated tenant found in security context"));
    }

    public static Long getCurrentUserId() {
        return getCurrentUser()
                .map(CustomUserDetails::getUserId)
                .orElseThrow(() -> new IllegalStateException("No authenticated user found in security context"));
    }

    public static UserRole getCurrentRole() {
        return getCurrentUser()
                .map(CustomUserDetails::getRole)
                .orElseThrow(() -> new IllegalStateException("No authenticated role found in security context"));
    }
}
