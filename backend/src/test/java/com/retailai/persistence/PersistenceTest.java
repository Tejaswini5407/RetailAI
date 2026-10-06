package com.retailai.persistence;

import com.retailai.domain.*;
import com.retailai.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class PersistenceTest {

    @Autowired private OrganizationRepository orgRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private StoreRepository storeRepo;
    @Autowired private ProductRepository productRepo;
    @Autowired private SaleRepository saleRepo;
    @Autowired private InventoryRepository inventoryRepo;

    @BeforeEach
    void setUp() {
        saleRepo.deleteAll();
        inventoryRepo.deleteAll();
        userRepo.deleteAll();
        storeRepo.deleteAll();
        productRepo.deleteAll();
        orgRepo.deleteAll();
    }

    @Test
    @DisplayName("A. Organization can be persisted cleanly")
    void testOrganizationPersistence() {
        Organization org = new Organization("ORG-TEST", "Test Retailer");
        Organization saved = orgRepo.save(org);

        assertNotNull(saved.getId());
        assertEquals("ORG-TEST", saved.getCode());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    @DisplayName("B. User belongs to an organization")
    void testUserBelongsToOrganization() {
        Organization org = orgRepo.save(new Organization("ORG-USER", "User Retailer"));
        User user = new User(org, "admin@userretailer.com", "Admin User", "hashed_pwd", UserRole.ADMIN);
        User savedUser = userRepo.save(user);

        assertNotNull(savedUser.getId());
        assertEquals(org.getId(), savedUser.getOrganization().getId());
        assertEquals(UserRole.ADMIN, savedUser.getRole());
    }

    @Test
    @DisplayName("C. Store belongs to an organization")
    void testStoreBelongsToOrganization() {
        Organization org = orgRepo.save(new Organization("ORG-STORE", "Store Retailer"));
        Store store = new Store(org, "CA_1", "California Main Store", "CA", "Los Angeles");
        Store savedStore = storeRepo.save(store);

        assertNotNull(savedStore.getId());
        assertEquals("CA_1", savedStore.getStoreCode());
        assertEquals(org.getId(), savedStore.getOrganization().getId());
    }

    @Test
    @DisplayName("D. Product belongs to an organization")
    void testProductBelongsToOrganization() {
        Organization org = orgRepo.save(new Organization("ORG-PROD", "Product Retailer"));
        Product product = new Product(org, "SKU-FOODS-001", "Organic Milk", "FOODS", "FOODS_1");
        Product savedProduct = productRepo.save(product);

        assertNotNull(savedProduct.getId());
        assertEquals("SKU-FOODS-001", savedProduct.getSku());
        assertEquals(org.getId(), savedProduct.getOrganization().getId());
    }

    @Test
    @DisplayName("E. Sales record references correct organization, store, and product")
    void testSalesReferencesCorrectEntities() {
        Organization org = orgRepo.save(new Organization("ORG-SALE", "Sale Retailer"));
        Store store = storeRepo.save(new Store(org, "TX_1", "Texas Store", "TX", "Dallas"));
        Product product = productRepo.save(new Product(org, "SKU-HOBBY-1", "Puzzle", "HOBBIES", "HOBBIES_1"));

        Sale sale = new Sale(org, store, product, LocalDate.of(2026, 4, 25), 15, new BigDecimal("12.99"));
        Sale savedSale = saleRepo.save(sale);

        assertNotNull(savedSale.getId());
        assertEquals(org.getId(), savedSale.getOrganization().getId());
        assertEquals(store.getId(), savedSale.getStore().getId());
        assertEquals(product.getId(), savedSale.getProduct().getId());
        assertEquals(15, savedSale.getUnitsSold());
    }

    @Test
    @DisplayName("F. Inventory record references correct organization, store, and product")
    void testInventoryReferencesCorrectEntities() {
        Organization org = orgRepo.save(new Organization("ORG-INV", "Inventory Retailer"));
        Store store = storeRepo.save(new Store(org, "WI_1", "Wisconsin Store", "WI", "Madison"));
        Product product = productRepo.save(new Product(org, "SKU-HOUSE-1", "Detergent", "HOUSEHOLD", "HOUSEHOLD_1"));

        Inventory inv = new Inventory(org, store, product, 50, 20);
        Inventory savedInv = inventoryRepo.save(inv);

        assertNotNull(savedInv.getId());
        assertEquals(org.getId(), savedInv.getOrganization().getId());
        assertEquals(50, savedInv.getQuantityOnHand());
        assertEquals(20, savedInv.getQuantityOnOrder());
    }

    @Test
    @DisplayName("G. Duplicate store code within SAME organization is rejected")
    void testDuplicateStoreCodeSameOrgRejected() {
        Organization org = orgRepo.save(new Organization("ORG-DUP-STORE", "Dup Store Retailer"));
        storeRepo.save(new Store(org, "STORE_1", "Store One", "CA", "LA"));

        assertThrows(DataIntegrityViolationException.class, () -> {
            storeRepo.saveAndFlush(new Store(org, "STORE_1", "Store Duplicate", "CA", "SF"));
        });
    }

    @Test
    @DisplayName("H. Same store code across DIFFERENT organizations is allowed")
    void testSameStoreCodeDifferentOrgsAllowed() {
        Organization orgA = orgRepo.save(new Organization("ORG-A", "Retailer A"));
        Organization orgB = orgRepo.save(new Organization("ORG-B", "Retailer B"));

        Store storeA = storeRepo.save(new Store(orgA, "CA_1", "Store A CA", "CA", "LA"));
        Store storeB = storeRepo.save(new Store(orgB, "CA_1", "Store B CA", "CA", "SF"));

        assertNotNull(storeA.getId());
        assertNotNull(storeB.getId());
        assertNotEquals(storeA.getId(), storeB.getId());
    }

    @Test
    @DisplayName("I. Duplicate SKU within SAME organization is rejected")
    void testDuplicateSkuSameOrgRejected() {
        Organization org = orgRepo.save(new Organization("ORG-DUP-SKU", "Dup SKU Retailer"));
        productRepo.save(new Product(org, "SKU-100", "Item 1", "FOODS", "FOODS_1"));

        assertThrows(DataIntegrityViolationException.class, () -> {
            productRepo.saveAndFlush(new Product(org, "SKU-100", "Item Dup", "FOODS", "FOODS_1"));
        });
    }

    @Test
    @DisplayName("J. Same SKU across DIFFERENT organizations is allowed")
    void testSameSkuDifferentOrgsAllowed() {
        Organization orgA = orgRepo.save(new Organization("ORG-SKU-A", "Retailer A"));
        Organization orgB = orgRepo.save(new Organization("ORG-SKU-B", "Retailer B"));

        Product prodA = productRepo.save(new Product(orgA, "SKU-GLOBAL", "Item A", "FOODS", "FOODS_1"));
        Product prodB = productRepo.save(new Product(orgB, "SKU-GLOBAL", "Item B", "FOODS", "FOODS_1"));

        assertNotNull(prodA.getId());
        assertNotNull(prodB.getId());
        assertNotEquals(prodA.getId(), prodB.getId());
    }

    @Test
    @DisplayName("K. Duplicate daily sales grain is rejected")
    void testDuplicateSalesGrainRejected() {
        Organization org = orgRepo.save(new Organization("ORG-GRAIN", "Grain Retailer"));
        Store store = storeRepo.save(new Store(org, "S1", "Store 1", "CA", "LA"));
        Product prod = productRepo.save(new Product(org, "P1", "Prod 1", "FOODS", "FOODS_1"));
        LocalDate date = LocalDate.of(2026, 4, 25);

        saleRepo.saveAndFlush(new Sale(org, store, prod, date, 10, new BigDecimal("2.50")));

        assertThrows(DataIntegrityViolationException.class, () -> {
            saleRepo.saveAndFlush(new Sale(org, store, prod, date, 15, new BigDecimal("2.50")));
        });
    }

    @Test
    @DisplayName("L. Negative units_sold is rejected by validation / DB check constraint")
    void testNegativeUnitsSoldRejected() {
        Organization org = orgRepo.save(new Organization("ORG-NEG-SALE", "Neg Sale Retailer"));
        Store store = storeRepo.save(new Store(org, "S1", "Store 1", "CA", "LA"));
        Product prod = productRepo.save(new Product(org, "P1", "Prod 1", "FOODS", "FOODS_1"));

        assertThrows(Exception.class, () -> {
            saleRepo.saveAndFlush(new Sale(org, store, prod, LocalDate.now(), -5, new BigDecimal("2.50")));
        });
    }

    @Test
    @DisplayName("M. Negative inventory quantity is rejected by validation / DB check constraint")
    void testNegativeInventoryRejected() {
        Organization org = orgRepo.save(new Organization("ORG-NEG-INV", "Neg Inv Retailer"));
        Store store = storeRepo.save(new Store(org, "S1", "Store 1", "CA", "LA"));
        Product prod = productRepo.save(new Product(org, "P1", "Prod 1", "FOODS", "FOODS_1"));

        assertThrows(Exception.class, () -> {
            inventoryRepo.saveAndFlush(new Inventory(org, store, prod, -10, 0));
        });
    }

    @Test
    @DisplayName("N. Inventory uniqueness per organization, store, and product works")
    void testInventoryUniqueness() {
        Organization org = orgRepo.save(new Organization("ORG-INV-UNIQ", "Inv Uniq Retailer"));
        Store store = storeRepo.save(new Store(org, "S1", "Store 1", "CA", "LA"));
        Product prod = productRepo.save(new Product(org, "P1", "Prod 1", "FOODS", "FOODS_1"));

        inventoryRepo.saveAndFlush(new Inventory(org, store, prod, 100, 50));

        assertThrows(DataIntegrityViolationException.class, () -> {
            inventoryRepo.saveAndFlush(new Inventory(org, store, prod, 200, 10));
        });
    }
}
