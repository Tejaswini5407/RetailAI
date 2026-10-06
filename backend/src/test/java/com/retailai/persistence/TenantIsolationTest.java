package com.retailai.persistence;

import com.retailai.domain.*;
import com.retailai.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class TenantIsolationTest {

    @Autowired private OrganizationRepository orgRepo;
    @Autowired private StoreRepository storeRepo;
    @Autowired private ProductRepository productRepo;
    @Autowired private SaleRepository saleRepo;
    @Autowired private InventoryRepository inventoryRepo;

    @BeforeEach
    void setUp() {
        saleRepo.deleteAll();
        inventoryRepo.deleteAll();
        storeRepo.deleteAll();
        productRepo.deleteAll();
        orgRepo.deleteAll();
    }

    @Test
    @DisplayName("Tenant Isolation Test: Organization A and B possess identical store codes and SKUs independently")
    void testTenantIsolationAndScopedQueries() {
        // 1. Create Organization A
        Organization orgA = orgRepo.save(new Organization("TENANT_A", "Retailer Alpha"));
        Store storeA = storeRepo.save(new Store(orgA, "CA_1", "Alpha Store CA", "CA", "Los Angeles"));
        Product productA = productRepo.save(new Product(orgA, "SKU-001", "Alpha Cereal", "FOODS", "FOODS_1"));
        Sale saleA = saleRepo.save(new Sale(orgA, storeA, productA, LocalDate.of(2026, 4, 25), 100, new BigDecimal("4.99")));
        Inventory invA = inventoryRepo.save(new Inventory(orgA, storeA, productA, 250, 50));

        // 2. Create Organization B with IDENTICAL store code (CA_1) and SKU (SKU-001)
        Organization orgB = orgRepo.save(new Organization("TENANT_B", "Retailer Beta"));
        Store storeB = storeRepo.save(new Store(orgB, "CA_1", "Beta Store CA", "CA", "San Francisco"));
        Product productB = productRepo.save(new Product(orgB, "SKU-001", "Beta Laundry Detergent", "HOUSEHOLD", "HOUSEHOLD_1"));
        Sale saleB = saleRepo.save(new Sale(orgB, storeB, productB, LocalDate.of(2026, 4, 25), 45, new BigDecimal("14.99")));
        Inventory invB = inventoryRepo.save(new Inventory(orgB, storeB, productB, 80, 0));

        // 3. Verify Tenant-Scoped Store Queries
        List<Store> storesA = storeRepo.findByOrganizationId(orgA.getId());
        List<Store> storesB = storeRepo.findByOrganizationId(orgB.getId());

        assertEquals(1, storesA.size());
        assertEquals("Alpha Store CA", storesA.get(0).getName());
        assertEquals(1, storesB.size());
        assertEquals("Beta Store CA", storesB.get(0).getName());

        // 4. Verify Tenant-Scoped Product Queries
        List<Product> productsA = productRepo.findByOrganizationId(orgA.getId());
        List<Product> productsB = productRepo.findByOrganizationId(orgB.getId());

        assertEquals(1, productsA.size());
        assertEquals("Alpha Cereal", productsA.get(0).getName());
        assertEquals(1, productsB.size());
        assertEquals("Beta Laundry Detergent", productsB.get(0).getName());

        // 5. Verify Tenant-Scoped Sales Queries
        List<Sale> salesA = saleRepo.findByOrganizationId(orgA.getId());
        List<Sale> salesB = saleRepo.findByOrganizationId(orgB.getId());

        assertEquals(1, salesA.size());
        assertEquals(100, salesA.get(0).getUnitsSold());
        assertEquals(1, salesB.size());
        assertEquals(45, salesB.get(0).getUnitsSold());

        // 6. Verify Tenant-Scoped Inventory Queries
        List<Inventory> invsA = inventoryRepo.findByOrganizationId(orgA.getId());
        List<Inventory> invsB = inventoryRepo.findByOrganizationId(orgB.getId());

        assertEquals(1, invsA.size());
        assertEquals(250, invsA.get(0).getQuantityOnHand());
        assertEquals(1, invsB.size());
        assertEquals(80, invsB.get(0).getQuantityOnHand());

        // 7. Verify cross-tenant isolation guarantees zero data leakage
        assertNotEquals(storeA.getId(), storeB.getId());
        assertNotEquals(productA.getId(), productB.getId());
        assertNotEquals(saleA.getId(), saleB.getId());
        assertNotEquals(invA.getId(), invB.getId());
    }
}
