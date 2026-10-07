package com.retailai.controller;

import com.retailai.dto.auth.UserSummaryDto;
import com.retailai.security.CustomUserDetails;
import com.retailai.security.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CurrentUserController {

    @GetMapping("/me")
    public ResponseEntity<UserSummaryDto> getCurrentUserContext(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UserSummaryDto summary = new UserSummaryDto(
            userDetails.getUserId(),
            userDetails.getUsername(),
            userDetails.getUsername(),
            userDetails.getOrganizationId(),
            userDetails.getOrganizationCode(),
            userDetails.getRole()
        );
        return ResponseEntity.ok(summary);
    }
}
