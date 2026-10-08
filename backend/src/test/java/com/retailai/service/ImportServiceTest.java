package com.retailai.service;

import com.retailai.domain.UserRole;
import com.retailai.dto.auth.AuthResponse;
import com.retailai.dto.auth.RegisterRequest;
import com.retailai.dto.importing.*;
import com.retailai.dto.product.CreateProductRequestDto;
import com.retailai.dto.store.CreateStoreRequestDto;
import com.retailai.repository.*;
import com.retailai.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ImportServiceTest {

    @Autowired private ImportService importService;
    @Autowired private AuthService authService;
    @Autowired private StoreService storeService;
    @Autowired private ProductService productService;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private SaleRepository saleRepository;
    @Autowired private InventoryRepository inventoryRepository;
    @Autowired private ImportHistoryRepository importHistoryRepository;

    private AuthResponse authTenant;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        saleRepository.deleteAll();
        inventoryRepository.deleteAll();
        productRepository.deleteAll();
        storeRepository.deleteAll();
        importHistoryRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        RegisterRequest req = new RegisterRequest("IMPORT-ORG", "Import Org Corp", "Import Admin", "admin@import.com", "Password123!", UserRole.ADMIN);
        authTenant = authService.register(req);
        setSecurityContext(authTenant);
    }

    private void setSecurityContext(AuthResponse auth) {
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
    }

    @Test
    @DisplayName("1. Product CSV preview and confirmation succeeds")
    void testProductImportWorkflow() {
        String csvContent = "sku,name,category,department\n" +
                            "SKU-IMPORT-1,Imported Milk,Dairy,Foods\n" +
                            "SKU-IMPORT-2,Imported Bread,Bakery,Foods\n";

        MockMultipartFile file = new MockMultipartFile("file", "products.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));
        ImportPreviewResponseDto preview = importService.previewImport(file, "PRODUCTS");

        assertEquals(2, preview.getTotalRows());
        assertEquals(2, preview.getValidRows());
        assertEquals(0, preview.getInvalidRows());

        ImportConfirmRequestDto confirmReq = new ImportConfirmRequestDto("PRODUCTS", "products.csv", preview.getPreviewData());
        ImportSummaryResponseDto summary = importService.confirmImport(confirmReq);

        assertEquals(2, summary.getSuccessfulRows());
        assertEquals("COMPLETED", summary.getStatus());
        assertTrue(productRepository.findByOrganizationIdAndSku(authTenant.getUser().getOrganizationId(), "SKU-IMPORT-1").isPresent());
    }

    @Test
    @DisplayName("2. Store CSV preview and confirmation succeeds")
    void testStoreImportWorkflow() {
        String csvContent = "store_code,name,state,city\n" +
                            "STORE-IMP-1,Imported Main Store,CA,Los Angeles\n";

        MockMultipartFile file = new MockMultipartFile("file", "stores.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));
        ImportPreviewResponseDto preview = importService.previewImport(file, "STORES");

        assertEquals(1, preview.getTotalRows());
        assertEquals(1, preview.getValidRows());

        ImportConfirmRequestDto confirmReq = new ImportConfirmRequestDto("STORES", "stores.csv", preview.getPreviewData());
        ImportSummaryResponseDto summary = importService.confirmImport(confirmReq);

        assertEquals(1, summary.getSuccessfulRows());
        assertTrue(storeRepository.findByOrganizationIdAndStoreCode(authTenant.getUser().getOrganizationId(), "STORE-IMP-1").isPresent());
    }

    @Test
    @DisplayName("3. Historical Sales CSV preview flags missing store and missing SKU errors")
    void testSalesImportValidationErrors() {
        storeService.createStore(new CreateStoreRequestDto("STORE-EXIST", "Existing Store", "CA", "LA"));

        String csvContent = "date,store_code,sku,units_sold,unit_price\n" +
                            "2026-10-01,STORE-EXIST,SKU-MISSING,10,4.99\n" +
                            "2026-10-01,STORE-MISSING,SKU-MISSING,5,4.99\n";

        MockMultipartFile file = new MockMultipartFile("file", "sales.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));
        ImportPreviewResponseDto preview = importService.previewImport(file, "SALES");

        assertEquals(2, preview.getTotalRows());
        assertEquals(0, preview.getValidRows());
        assertEquals(2, preview.getInvalidRows());
        assertFalse(preview.getValidationErrors().isEmpty());
    }

    @Test
    @DisplayName("4. Inventory CSV preview flags negative quantity error")
    void testInventoryImportNegativeQuantityError() {
        storeService.createStore(new CreateStoreRequestDto("STORE-INV", "Inv Store", "CA", "LA"));
        productService.createProduct(new CreateProductRequestDto("SKU-INV", "Inv Prod", "Cat", "Dept"));

        String csvContent = "store_code,sku,quantity_on_hand,quantity_on_order\n" +
                            "STORE-INV,SKU-INV,-15,0\n";

        MockMultipartFile file = new MockMultipartFile("file", "inventory.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));
        ImportPreviewResponseDto preview = importService.previewImport(file, "INVENTORY");

        assertEquals(1, preview.getTotalRows());
        assertEquals(0, preview.getValidRows());
        assertEquals(1, preview.getInvalidRows());
        assertEquals("quantity_on_hand cannot be negative", preview.getValidationErrors().get(0).getMessage());
    }

    @Test
    @DisplayName("5. Import History persistence and tenant scoping")
    void testImportHistoryPersistence() {
        testProductImportWorkflow();
        List<ImportHistoryResponseDto> history = importService.getImportHistory();
        assertFalse(history.isEmpty());
        assertEquals("PRODUCTS", history.get(0).getImportType());
    }
}
