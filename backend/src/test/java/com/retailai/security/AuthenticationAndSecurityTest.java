package com.retailai.security;

import com.retailai.domain.Organization;
import com.retailai.domain.User;
import com.retailai.domain.UserRole;
import com.retailai.dto.auth.AuthResponse;
import com.retailai.dto.auth.LoginRequest;
import com.retailai.dto.auth.RegisterRequest;
import com.retailai.dto.auth.UserSummaryDto;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.assertThat;
import com.retailai.config.AbstractIntegrationTest;

@AutoConfigureMockMvc
public class AuthenticationAndSecurityTest extends AbstractIntegrationTest {

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
    @DisplayName("1. Public registration creates NEW organization and forces server-assigned ADMIN role")
    void testRegistrationSuccessNewOrganization() throws Exception {
        String json = """
            {
                "organizationCode": "ORG-ALPHA",
                "organizationName": "Alpha Retail",
                "name": "Jane Founder",
                "email": "jane@alpha.com",
                "password": "securepassword123",
                "role": "STORE_MANAGER"
            }
            """;

        // Even though client requested "STORE_MANAGER", server forces "ADMIN" for new organization founder
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.user.email").value("jane@alpha.com"))
                .andExpect(jsonPath("$.user.organizationCode").value("ORG-ALPHA"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"));

        User user = userRepository.findByEmail("jane@alpha.com").orElseThrow();
        assertEquals(UserRole.ADMIN, user.getRole());
    }

    @Test
    @DisplayName("5.A & 5.B Public registration CANNOT join existing organization or self-assign ADMIN")
    void testPublicRegistrationCannotJoinExistingOrganization() throws Exception {
        // Register initial org
        RegisterRequest initialReq = new RegisterRequest("ORG-TARGET", "Target Corp", "Owner", "owner@target.com", "Password123!", UserRole.ADMIN);
        authService.register(initialReq);

        // Malicious public registration attempt targeting existing organization
        String maliciousJson = """
            {
                "organizationCode": "ORG-TARGET",
                "organizationName": "Target Corp Impostor",
                "name": "Attacker",
                "email": "attacker@target.com",
                "password": "Password123!",
                "role": "ADMIN"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(maliciousJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Organization with code 'ORG-TARGET' already exists. Public registration cannot join an existing organization."));

        // Verify attacker account was NOT created
        assertFalse(userRepository.existsByEmail("attacker@target.com"));
    }

    @Test
    @DisplayName("5.C Newly created organization receives first user with ADMIN role")
    void testNewlyCreatedOrgFirstUserRole() {
        RegisterRequest req = new RegisterRequest("ORG-NEW", "New Org", "First User", "first@new.com", "Password123!", UserRole.INVENTORY_MANAGER);
        AuthResponse response = authService.register(req);

        assertEquals(UserRole.ADMIN, response.getUser().getRole());
        User persistedUser = userRepository.findByEmail("first@new.com").orElseThrow();
        assertEquals(UserRole.ADMIN, persistedUser.getRole());
    }

    @Test
    @DisplayName("5.D, 5.E, 5.F JWT claims match persisted server-side user, orgId, orgCode, and role")
    void testJwtClaimsMatchPersistedData() throws Exception {
        RegisterRequest req = new RegisterRequest("ORG-CLAIMS", "Claims Org", "Claims User", "claims@test.com", "pass12345", UserRole.ADMIN);
        AuthResponse response = authService.register(req);

        User persistedUser = userRepository.findByEmail("claims@test.com").orElseThrow();
        Organization persistedOrg = organizationRepository.findByCode("ORG-CLAIMS").orElseThrow();

        mockMvc.perform(get("/api/v1/security/me")
                .header("Authorization", "Bearer " + response.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(persistedUser.getId().intValue()))
                .andExpect(jsonPath("$.organizationId").value(persistedOrg.getId().intValue()))
                .andExpect(jsonPath("$.organizationCode").value("ORG-CLAIMS"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("5.G & 5.H Client cannot manipulate organizationId via request body or headers")
    void testClientCannotOverrideOrganizationId() throws Exception {
        RegisterRequest req = new RegisterRequest("ORG-STRICT", "Strict Org", "Strict User", "strict@test.com", "pass12345", UserRole.ADMIN);
        AuthResponse response = authService.register(req);

        // Attempt header/body override
        mockMvc.perform(get("/api/v1/security/tenant-info")
                .header("Authorization", "Bearer " + response.getAccessToken())
                .header("X-Tenant-Override", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationCode").value("ORG-STRICT"))
                .andExpect(jsonPath("$.organizationId").value(response.getUser().getOrganizationId()));
    }

    @Test
    @DisplayName("5.I & 5.J Tenant security context and RBAC enforced correctly")
    void testTenantSecurityContextAndRbacEnforced() throws Exception {
        // Create Org A with founder (ADMIN)
        RegisterRequest reqA = new RegisterRequest("ORG-SEC-A", "Org Sec A", "Admin A", "adminA@sec.com", "pass12345", UserRole.ADMIN);
        AuthResponse authA = authService.register(reqA);

        // Create Org B with founder (ADMIN)
        RegisterRequest reqB = new RegisterRequest("ORG-SEC-B", "Org Sec B", "Admin B", "adminB@sec.com", "pass12345", UserRole.ADMIN);
        AuthResponse authB = authService.register(reqB);

        // Admin B adds a STORE_MANAGER to Org B via createUserInTenant
        RegisterRequest staffReq = new RegisterRequest("ORG-SEC-B", "Org Sec B", "Staff B", "staffB@sec.com", "pass12345", UserRole.STORE_MANAGER);
        UserSummaryDto staffB = authService.createUserInTenant(staffReq, authB.getUser().getOrganizationId());
        assertEquals(UserRole.STORE_MANAGER, staffB.getRole());

        // Authenticate staff B via login
        LoginRequest loginReq = new LoginRequest("ORG-SEC-B", "staffB@sec.com", "pass12345");
        AuthResponse staffAuth = authService.login(loginReq);

        // Staff B (STORE_MANAGER) cannot access ADMIN-only endpoint
        mockMvc.perform(get("/api/v1/security/admin-only")
                .header("Authorization", "Bearer " + staffAuth.getAccessToken()))
                .andExpect(status().isForbidden());

        // Admin A (ADMIN) can access ADMIN-only endpoint
        mockMvc.perform(get("/api/v1/security/admin-only")
                .header("Authorization", "Bearer " + authA.getAccessToken()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Duplicate email registration is rejected with 409 Conflict")
    void testDuplicateEmailRegistrationRejected() throws Exception {
        RegisterRequest req1 = new RegisterRequest("ORG-1", "Org One", "User One", "dup@test.com", "password123", UserRole.ADMIN);
        authService.register(req1);

        String json = """
            {
                "organizationCode": "ORG-2",
                "name": "User Two",
                "email": "dup@test.com",
                "password": "anotherpassword",
                "role": "ADMIN"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    @DisplayName("Case-insensitive duplicate org code registration is rejected with 409 Conflict")
    void testCaseInsensitiveOrgCodeDuplicateRegistrationRejected() throws Exception {
        RegisterRequest initialReq = new RegisterRequest("ORG-CASE-CHECK", "Case Org", "Owner", "owner@casecheck.com", "Password123!", UserRole.ADMIN);
        authService.register(initialReq);

        String lowerCaseOrgJson = """
            {
                "organizationCode": "org-case-check",
                "organizationName": "Impostor Org",
                "name": "Attacker",
                "email": "attacker@casecheck.com",
                "password": "Password123!",
                "role": "ADMIN"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(lowerCaseOrgJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    @DisplayName("Password is stored hashed in database using BCrypt")
    void testPasswordStoredHashed() {
        RegisterRequest req = new RegisterRequest("ORG-PWD", "Pwd Org", "User Pwd", "hashcheck@test.com", "mysecretpassword", UserRole.ADMIN);
        authService.register(req);

        User user = userRepository.findByEmail("hashcheck@test.com").orElseThrow();
        assertNotEquals("mysecretpassword", user.getPasswordHash());
        assertTrue(passwordEncoder.matches("mysecretpassword", user.getPasswordHash()));
        assertTrue(user.getPasswordHash().startsWith("$2a$") || user.getPasswordHash().startsWith("$2b$"));
    }

    @Test
    @DisplayName("Login with correct credentials succeeds")
    void testLoginSuccess() throws Exception {
        RegisterRequest req = new RegisterRequest("ORG-LOGIN", "Login Org", "User Login", "login@test.com", "correctpass123", UserRole.ADMIN);
        authService.register(req);

        String loginJson = """
            {
                "organizationCode": "ORG-LOGIN",
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
    @DisplayName("Login with mixed-case organization code and email succeeds")
    void testCaseInsensitiveLoginSuccess() throws Exception {
        RegisterRequest req = new RegisterRequest("ORG-MIXED", "Mixed Org", "User Mixed", "mixed@test.com", "pass12345", UserRole.ADMIN);
        authService.register(req);

        String loginJson = """
            {
                "organizationCode": "org-mixed",
                "email": "MIXED@TEST.COM",
                "password": "pass12345"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.user.email").value("mixed@test.com"));
    }

    @Test
    @DisplayName("Login with unknown organization code returns 404 Not Found with user-friendly registration guidance")
    void testLoginUnknownOrganizationCodeFails() throws Exception {
        String loginJson = """
            {
                "organizationCode": "ORG-NONEXISTENT",
                "email": "user@nonexistent.com",
                "password": "Password123!"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.code").value("ORGANIZATION_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("This organization code is not registered. Please register your organization before logging in."));
    }

    @Test
    @DisplayName("Login with valid organization but incorrect user credentials returns 401 Unauthorized")
    void testLoginValidOrganizationIncorrectCredentialsFails() throws Exception {
        RegisterRequest req = new RegisterRequest("ORG-VALID-USER", "Valid User Org", "Valid User", "user@validuser.com", "pass12345", UserRole.ADMIN);
        authService.register(req);

        String loginJson = """
            {
                "organizationCode": "ORG-VALID-USER",
                "email": "wronguser@validuser.com",
                "password": "pass12345"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid organization code, email, or password"));
    }

    @Test
    @DisplayName("Login with mismatched organization code returns 401 Unauthorized")
    void testLoginMismatchedOrgCodeFails() throws Exception {
        RegisterRequest reqA = new RegisterRequest("ORG-VALID-A", "Org A", "User A", "userA@test.com", "pass12345", UserRole.ADMIN);
        authService.register(reqA);
        RegisterRequest reqB = new RegisterRequest("ORG-VALID-B", "Org B", "User B", "userB@test.com", "pass12345", UserRole.ADMIN);
        authService.register(reqB);

        String loginJson = """
            {
                "organizationCode": "ORG-VALID-B",
                "email": "userA@test.com",
                "password": "pass12345"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid organization code, email, or password"));
    }

    @Test
    @DisplayName("Login with wrong password fails with 401 Unauthorized")
    void testLoginWrongPasswordFails() throws Exception {
        RegisterRequest req = new RegisterRequest("ORG-WRONG", "Wrong Org", "User Wrong", "wrongpass@test.com", "rightpass", UserRole.ADMIN);
        authService.register(req);

        String loginJson = """
            {
                "organizationCode": "ORG-WRONG",
                "email": "wrongpass@test.com",
                "password": "badpassword"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid organization code, email, or password"));
    }

    @Test
    @DisplayName("Invalid or malformed JWT tokens are rejected")
    void testInvalidJwtRejected() throws Exception {
        mockMvc.perform(get("/api/v1/security/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/security/me")
                .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CORS OPTIONS preflight request to /api/v1/auth/register succeeds with 200 OK")
    void testCorsPreflightRequestSuccess() throws Exception {
        mockMvc.perform(options("/api/v1/auth/register")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type,Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS"));
    }

    @Test
    @DisplayName("JwtService throws IllegalStateException when default dev secret is used in prod profile")
    void testProductionProfileWithDefaultJwtSecretThrowsException() {
        String defaultDevSecret = "c3VwZXItc2VjcmV0LWF1dGhlbnRpY2F0aW9uLWtleS1mb3ItcmV0YWlsYWktc2Fhcy1wbGF0Zm9ybS0yNTZiaXQ=";
        assertThrows(IllegalStateException.class, () -> {
            new JwtService(defaultDevSecret, 86400000L, "prod");
        });
    }

    @Test
    @DisplayName("Spring Application Context fails to load in 'prod' profile when default JWT secret is used")
    void testSpringContextFailsInProdProfileWithDefaultJwtSecret() {
        ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                .withPropertyValues(
                    "spring.profiles.active=prod",
                    "jwt.secret=c3VwZXItc2VjcmV0LWF1dGhlbnRpY2F0aW9uLWtleS1mb3ItcmV0YWlsYWktc2Fhcy1wbGF0Zm9ybS0yNTZiaXQ=",
                    "jwt.expiration-ms=86400000"
                )
                .withBean(JwtService.class);

        contextRunner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalStateException.class);
            Throwable rootCause = org.springframework.core.NestedExceptionUtils.getRootCause(context.getStartupFailure());
            assertThat(rootCause).isNotNull().hasMessageContaining("Production profile ('prod') is using the default development JWT secret key");
        });
    }

    @Test
    @DisplayName("Spring Application Context loads successfully in 'prod' profile when a custom strong JWT secret is configured")
    void testSpringContextSucceedsInProdProfileWithCustomJwtSecret() {
        String customStrongSecret = "Y3VzdG9tLXNlY3VyZS1wcm9kdWN0aW9uLWp3dC1zaWduaW5nLWtleS0yNTZiaXQtc3Ryb25n";
        ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                .withPropertyValues(
                    "spring.profiles.active=prod",
                    "jwt.secret=" + customStrongSecret,
                    "jwt.expiration-ms=86400000"
                )
                .withBean(JwtService.class);

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(JwtService.class);
        });
    }
}
