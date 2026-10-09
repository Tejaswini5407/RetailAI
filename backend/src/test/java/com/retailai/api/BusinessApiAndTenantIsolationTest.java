package com.retailai.api;

import com.retailai.domain.UserRole;
import com.retailai.dto.auth.AuthResponse;
import com.retailai.dto.auth.RegisterRequest;
import com.retailai.dto.inventory.CreateInventoryRequestDto;
import com.retailai.dto.product.CreateProductRequestDto;
import com.retailai.dto.product.ProductResponseDto;
import com.retailai.dto.store.CreateStoreRequestDto;
import com.retailai.dto.store.StoreResponseDto;
import com.retailai.repository.*;
import com.retailai.security.CustomUserDetails;
import com.retailai.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.mockito.Mockito;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import java.util.List;
import java.util.Map;

import com.retailai.config.AbstractIntegrationTest;

@AutoConfigureMockMvc
public class BusinessApiAndTenantIsolationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private RestTemplate restTemplate;
    @Autowired private AuthService authService;
    @Autowired private StoreService storeService;
    @Autowired private ProductService productService;
    @Autowired private SaleService saleService;
    @Autowired private InventoryService inventoryService;
    @Autowired private UserRepository userRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private SaleRepository saleRepository;
    @Autowired private InventoryRepository inventoryRepository;

    private AuthResponse authTenantA;
    private AuthResponse authTenantB;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        saleRepository.deleteAll();
        inventoryRepository.deleteAll();
        productRepository.deleteAll();
        storeRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        // Register Tenant A
        RegisterRequest reqA = new RegisterRequest("TENANT-A-CODE", "Tenant A Corp", "Alice Admin", "alice@tenantA.com", "Password123!", UserRole.ADMIN);
        authTenantA = authService.register(reqA);

        // Register Tenant B
        RegisterRequest reqB = new RegisterRequest("TENANT-B-CODE", "Tenant B Corp", "Bob Admin", "bob@tenantB.com", "Password123!", UserRole.ADMIN);
        authTenantB = authService.register(reqB);

        SecurityContextHolder.clearContext();
    }

    private void runAs(AuthResponse auth, Runnable action) {
        try {
            CustomUserDetails userDetails = new CustomUserDetails(
                auth.getUser().getId(),
                auth.getUser().getEmail(),
                "passwordHash",
                auth.getUser().getOrganizationId(),
                auth.getUser().getOrganizationCode(),
                auth.getUser().getRole()
            );
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            action.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("1. GET /api/v1/me returns authenticated user & organization context")
    void testGetCurrentUserContext() throws Exception {
        mockMvc.perform(get("/api/v1/me")
                .header("Authorization", "Bearer " + authTenantA.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@tenantA.com"))
                .andExpect(jsonPath("$.organizationCode").value("TENANT-A-CODE"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("2. Store CRUD & Tenant Isolation: Tenant A cannot access or update Tenant B's store")
    void testStoreCrudAndTenantIsolation() throws Exception {
        // Create Store in Tenant A
        String storeAJson = """
            {
                "storeCode": "STORE-CA-1",
                "name": "CA Store 1",
                "state": "CA",
                "city": "Los Angeles"
            }
            """;

        mockMvc.perform(post("/api/v1/stores")
                .header("Authorization", "Bearer " + authTenantA.getAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(storeAJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.storeCode").value("STORE-CA-1"));

        // Create Store in Tenant B with same storeCode (should succeed because storeCode uniqueness is per-tenant)
        String storeBJson = """
            {
                "storeCode": "STORE-CA-1",
                "name": "CA Store 1 (Tenant B)",
                "state": "CA",
                "city": "San Francisco"
            }
            """;

        mockMvc.perform(post("/api/v1/stores")
                .header("Authorization", "Bearer " + authTenantB.getAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(storeBJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.storeCode").value("STORE-CA-1"));

        // Tenant A listing stores should only see Tenant A's store
        mockMvc.perform(get("/api/v1/stores")
                .header("Authorization", "Bearer " + authTenantA.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("CA Store 1"));

        // Get Tenant B's store ID
        Long storeBId = storeRepository.findByOrganizationId(authTenantB.getUser().getOrganizationId()).get(0).getId();

        // Tenant A attempting to GET Tenant B's store by ID fails with 404 Not Found
        mockMvc.perform(get("/api/v1/stores/" + storeBId)
                .header("Authorization", "Bearer " + authTenantA.getAccessToken()))
                .andExpect(status().isNotFound());

        // Tenant A attempting to PUT Tenant B's store fails with 404 Not Found
        String updateJson = """
            {
                "name": "Hacked Store"
            }
            """;
        mockMvc.perform(put("/api/v1/stores/" + storeBId)
                .header("Authorization", "Bearer " + authTenantA.getAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("3. Product CRUD & Duplicate SKU: Tenant A cannot access Tenant B's product")
    void testProductCrudAndTenantIsolation() throws Exception {
        // Create Product in Tenant A
        String prodAJson = """
            {
                "sku": "FOODS_1_001",
                "name": "Organic Milk",
                "category": "Dairy",
                "department": "Foods"
            }
            """;

        mockMvc.perform(post("/api/v1/products")
                .header("Authorization", "Bearer " + authTenantA.getAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(prodAJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("FOODS_1_001"));

        // Duplicate SKU in same tenant fails with 409 Conflict
        mockMvc.perform(post("/api/v1/products")
                .header("Authorization", "Bearer " + authTenantA.getAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(prodAJson))
                .andExpect(status().isConflict());

        // Create Product in Tenant B with same SKU succeeds
        mockMvc.perform(post("/api/v1/products")
                .header("Authorization", "Bearer " + authTenantB.getAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(prodAJson))
                .andExpect(status().isCreated());

        Long prodBId = productRepository.findByOrganizationId(authTenantB.getUser().getOrganizationId()).get(0).getId();

        // Tenant A GET Tenant B product -> 404
        mockMvc.perform(get("/api/v1/products/" + prodBId)
                .header("Authorization", "Bearer " + authTenantA.getAccessToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("4. Sales API & Cross-Tenant Reference Attack Prevention")
    void testSalesCrossTenantAttackPrevented() throws Exception {
        StoreResponseDto[] storeHolderA = new StoreResponseDto[1];
        ProductResponseDto[] prodHolderA = new ProductResponseDto[1];
        StoreResponseDto[] storeHolderB = new StoreResponseDto[1];
        ProductResponseDto[] prodHolderB = new ProductResponseDto[1];

        // Create Store and Product in Tenant A
        runAs(authTenantA, () -> {
            storeHolderA[0] = storeService.createStore(new CreateStoreRequestDto("STORE-A", "Store A", "CA", "LA"));
            prodHolderA[0] = productService.createProduct(new CreateProductRequestDto("SKU-A", "Prod A", "Cat", "Dept"));
        });

        // Create Store and Product in Tenant B
        runAs(authTenantB, () -> {
            storeHolderB[0] = storeService.createStore(new CreateStoreRequestDto("STORE-B", "Store B", "NY", "NYC"));
            prodHolderB[0] = productService.createProduct(new CreateProductRequestDto("SKU-B", "Prod B", "Cat", "Dept"));
        });

        StoreResponseDto storeA = storeHolderA[0];
        ProductResponseDto prodA = prodHolderA[0];
        StoreResponseDto storeB = storeHolderB[0];

        // Tenant A attempts to record a sale referencing Tenant B's storeId -> fails with 404
        String badSaleJson = String.format("""
            {
                "storeId": %d,
                "productId": %d,
                "saleDate": "2026-04-25",
                "unitsSold": 10,
                "unitPrice": 4.99
            }
            """, storeB.getId(), prodA.getId());

        mockMvc.perform(post("/api/v1/sales")
                .header("Authorization", "Bearer " + authTenantA.getAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(badSaleJson))
                .andExpect(status().isNotFound());

        // Valid sale in Tenant A succeeds
        String validSaleJson = String.format("""
            {
                "storeId": %d,
                "productId": %d,
                "saleDate": "2026-04-25",
                "unitsSold": 10,
                "unitPrice": 4.99
            }
            """, storeA.getId(), prodA.getId());

        mockMvc.perform(post("/api/v1/sales")
                .header("Authorization", "Bearer " + authTenantA.getAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validSaleJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.unitsSold").value(10));

        // Duplicate daily sale grain fails with 409 Conflict
        mockMvc.perform(post("/api/v1/sales")
                .header("Authorization", "Bearer " + authTenantA.getAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validSaleJson))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("5. Inventory API & Phase 5 Inventory Assessment Engine")
    void testInventoryAssessmentEngine() throws Exception {
        StoreResponseDto[] storeHolderA = new StoreResponseDto[1];
        ProductResponseDto[] prodHolderA = new ProductResponseDto[1];

        runAs(authTenantA, () -> {
            storeHolderA[0] = storeService.createStore(new CreateStoreRequestDto("STORE-INV", "Store Inv", "CA", "LA"));
            prodHolderA[0] = productService.createProduct(new CreateProductRequestDto("SKU-INV", "Prod Inv", "Cat", "Dept"));
            inventoryService.createInventory(new CreateInventoryRequestDto(storeHolderA[0].getId(), prodHolderA[0].getId(), 5, 10));
        });

        StoreResponseDto storeA = storeHolderA[0];
        ProductResponseDto prodA = prodHolderA[0];

        // Inventory assessment request (forecastDemand = 140, forecastHorizonDays = 14, leadTimeDays = 7)
        // avgDailyDemand = 10.0, leadTimeDemand = 70.0
        // currentInventory = 5 < leadTimeDemand (70.0) -> Stockout risk HIGH!
        String assessJson = String.format("""
            {
                "storeId": %d,
                "productId": %d,
                "forecastDemand": 140.0,
                "forecastHorizonDays": 14,
                "leadTimeDays": 7,
                "safetyStock": 20,
                "moq": 50
            }
            """, storeA.getId(), prodA.getId());

        mockMvc.perform(post("/api/v1/inventory/assess")
                .header("Authorization", "Bearer " + authTenantA.getAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(assessJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageDailyDemand").value(10.0))
                .andExpect(jsonPath("$.stockoutRisk").value("HIGH"))
                .andExpect(jsonPath("$.inventoryStatus").value("CRITICAL"))
                .andExpect(jsonPath("$.recommendedOrderQuantity").value(145)); // rawOrder = max(0, 140 + 20 - 5 - 10) = 145 >= moq(50)
    }

    @Test
    @DisplayName("6. Forecast Endpoint Integration - Successful Model Call")
    void testForecastEndpointIntegration() throws Exception {
        Map<String, Object> mockResponse = Map.of(
            "model_name", "Random Forest",
            "model_version", "1.0.0",
            "predictions", List.of(Map.of("predicted_demand", 18.5, "rounded_demand", 19))
        );
        Mockito.when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(mockResponse));

        StoreResponseDto[] storeHolderA = new StoreResponseDto[1];
        ProductResponseDto[] prodHolderA = new ProductResponseDto[1];

        runAs(authTenantA, () -> {
            storeHolderA[0] = storeService.createStore(new CreateStoreRequestDto("STORE-FC", "Store FC", "CA", "LA"));
            prodHolderA[0] = productService.createProduct(new CreateProductRequestDto("SKU-FC", "Prod FC", "Cat", "Dept"));
        });

        StoreResponseDto storeA = storeHolderA[0];
        ProductResponseDto prodA = prodHolderA[0];

        String forecastReqJson = String.format("""
            {
                "storeId": %d,
                "productId": %d,
                "date": "2026-04-25"
            }
            """, storeA.getId(), prodA.getId());

        mockMvc.perform(post("/api/v1/forecasts")
                .header("Authorization", "Bearer " + authTenantA.getAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(forecastReqJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeCode").value("STORE-FC"))
                .andExpect(jsonPath("$.sku").value("SKU-FC"))
                .andExpect(jsonPath("$.predictedDemand").value(18.5))
                .andExpect(jsonPath("$.modelName").value("Random Forest"));
    }

    @Test
    @DisplayName("7. Forecast Endpoint Integration - FastAPI Service Unavailable (Returns HTTP 503)")
    void testForecastEndpointWhenMLServiceUnavailable() throws Exception {
        Mockito.when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenThrow(new RuntimeException("Connection refused to ML microservice"));

        StoreResponseDto[] storeHolderA = new StoreResponseDto[1];
        ProductResponseDto[] prodHolderA = new ProductResponseDto[1];

        runAs(authTenantA, () -> {
            storeHolderA[0] = storeService.createStore(new CreateStoreRequestDto("STORE-ERR", "Store ERR", "CA", "LA"));
            prodHolderA[0] = productService.createProduct(new CreateProductRequestDto("SKU-ERR", "Prod ERR", "Cat", "Dept"));
        });

        StoreResponseDto storeA = storeHolderA[0];
        ProductResponseDto prodA = prodHolderA[0];

        String forecastReqJson = String.format("""
            {
                "storeId": %d,
                "productId": %d,
                "date": "2026-04-25"
            }
            """, storeA.getId(), prodA.getId());

        mockMvc.perform(post("/api/v1/forecasts")
                .header("Authorization", "Bearer " + authTenantA.getAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(forecastReqJson))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("Service Unavailable"))
                .andExpect(jsonPath("$.message").value("Forecast service is temporarily unavailable. Please try again later."));
    }
}
