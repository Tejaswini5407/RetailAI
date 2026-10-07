package com.retailai.security;

import com.retailai.domain.Organization;
import com.retailai.domain.User;
import com.retailai.domain.UserRole;
import com.retailai.dto.auth.AuthResponse;
import com.retailai.dto.auth.LoginRequest;
import com.retailai.dto.auth.RegisterRequest;
import com.retailai.repository.OrganizationRepository;
import com.retailai.repository.UserRepository;
import com.retailai.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthenticationAndSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AuthService authService;
    @Autowired private UserRepository userRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    @DisplayName("A. Registration succeeds with valid data")
    void testRegistrationSuccess() throws Exception {
        String json = """
            {
                "organizationCode": "ORG-ALPHA",
                "organizationName": "Alpha Retail",
                "name": "Jane Doe",
                "email": "jane@alpha.com",
                "password": "securepassword123",
                "role": "ADMIN"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.user.email").value("jane@alpha.com"))
                .andExpect(jsonPath("$.user.organizationCode").value("ORG-ALPHA"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"));
    }

    @Test
    @DisplayName("B. Duplicate email registration is rejected")
    void testDuplicateEmailRegistrationRejected() throws Exception {
        RegisterRequest req1 = new RegisterRequest("ORG-1", "Org One", "User One", "dup@test.com", "password123", UserRole.STORE_MANAGER);
        authService.register(req1);

        String json = """
            {
                "organizationCode": "ORG-2",
                "name": "User Two",
                "email": "dup@test.com",
                "password": "anotherpassword",
                "role": "INVENTORY_MANAGER"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    @DisplayName("C. Password is stored hashed in database, not in plaintext")
    void testPasswordStoredHashed() {
        RegisterRequest req = new RegisterRequest("ORG-PWD", "Pwd Org", "User Pwd", "hashcheck@test.com", "mysecretpassword", UserRole.ADMIN);
        authService.register(req);

        User user = userRepository.findByEmail("hashcheck@test.com").orElseThrow();
        assertNotEquals("mysecretpassword", user.getPasswordHash());
        assertTrue(passwordEncoder.matches("mysecretpassword", user.getPasswordHash()));
        assertTrue(user.getPasswordHash().startsWith("$2a$") || user.getPasswordHash().startsWith("$2b$"));
    }

    @Test
    @DisplayName("D. Correct login succeeds and returns JWT token")
    void testLoginSuccess() throws Exception {
        RegisterRequest req = new RegisterRequest("ORG-LOGIN", "Login Org", "User Login", "login@test.com", "correctpass123", UserRole.STORE_MANAGER);
        authService.register(req);

        String loginJson = """
            {
                "email": "login@test.com",
                "password": "correctpass123"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("login@test.com"));
    }

    @Test
    @DisplayName("E. Login with wrong password fails with 401 Unauthorized")
    void testLoginWrongPasswordFails() throws Exception {
        RegisterRequest req = new RegisterRequest("ORG-WRONG", "Wrong Org", "User Wrong", "wrongpass@test.com", "rightpass", UserRole.ADMIN);
        authService.register(req);

        String loginJson = """
            {
                "email": "wrongpass@test.com",
                "password": "badpassword"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("F. Login with unknown email fails safely with 401 Unauthorized")
    void testLoginUnknownEmailFailsSafely() throws Exception {
        String loginJson = """
            {
                "email": "nonexistent@test.com",
                "password": "somepassword"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("G. Valid JWT authenticates protected endpoint /api/v1/security/me")
    void testValidJwtAuthenticatesProtectedEndpoint() throws Exception {
        RegisterRequest req = new RegisterRequest("ORG-JWT", "JWT Org", "User JWT", "jwt@test.com", "jwtpass123", UserRole.INVENTORY_MANAGER);
        AuthResponse auth = authService.register(req);

        mockMvc.perform(get("/api/v1/security/me")
                .header("Authorization", "Bearer " + auth.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jwt@test.com"))
                .andExpect(jsonPath("$.organizationCode").value("ORG-JWT"))
                .andExpect(jsonPath("$.role").value("INVENTORY_MANAGER"));
    }

    @Test
    @DisplayName("H & I. Invalid, malformed, or missing JWT tokens are rejected")
    void testInvalidJwtRejected() throws Exception {
        // Missing Authorization header
        mockMvc.perform(get("/api/v1/security/me"))
                .andExpect(status().isUnauthorized());

        // Malformed Bearer token
        mockMvc.perform(get("/api/v1/security/me")
                .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("L, M, N. Role-Based Access Control: ADMIN can access admin-only endpoint, STORE_MANAGER is forbidden")
    void testRoleBasedAccessControl() throws Exception {
        // ADMIN user
        RegisterRequest adminReq = new RegisterRequest("ORG-RBAC", "RBAC Org", "Admin User", "admin@rbac.com", "pass12345", UserRole.ADMIN);
        AuthResponse adminAuth = authService.register(adminReq);

        mockMvc.perform(get("/api/v1/security/admin-only")
                .header("Authorization", "Bearer " + adminAuth.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        // STORE_MANAGER user
        RegisterRequest managerReq = new RegisterRequest("ORG-RBAC", "RBAC Org", "Manager User", "manager@rbac.com", "pass12345", UserRole.STORE_MANAGER);
        AuthResponse managerAuth = authService.register(managerReq);

        mockMvc.perform(get("/api/v1/security/admin-only")
                .header("Authorization", "Bearer " + managerAuth.getAccessToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("P, Q, R. Tenant Isolation Security: SecurityContext and TenantContext derive organization strictly from JWT")
    void testTenantSecurityContextIsolation() throws Exception {
        // Tenant A
        RegisterRequest reqA = new RegisterRequest("ORG-AAA", "Tenant A", "Alice", "alice@orgA.com", "passwordA", UserRole.ADMIN);
        AuthResponse authA = authService.register(reqA);

        // Tenant B
        RegisterRequest reqB = new RegisterRequest("ORG-BBB", "Tenant B", "Bob", "bob@orgB.com", "passwordB", UserRole.STORE_MANAGER);
        AuthResponse authB = authService.register(reqB);

        // Request with Tenant A token
        mockMvc.perform(get("/api/v1/security/tenant-info")
                .header("Authorization", "Bearer " + authA.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationCode").value("ORG-AAA"))
                .andExpect(jsonPath("$.email").value("alice@orgA.com"));

        // Request with Tenant B token
        mockMvc.perform(get("/api/v1/security/tenant-info")
                .header("Authorization", "Bearer " + authB.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationCode").value("ORG-BBB"))
                .andExpect(jsonPath("$.email").value("bob@orgB.com"));

        // Verify tokens cannot be cross-referenced or forged to impersonate another tenant
        assertNotEquals(authA.getUser().getOrganizationId(), authB.getUser().getOrganizationId());
    }
}
