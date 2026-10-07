package com.retailai.service;

import com.retailai.domain.Inventory;
import com.retailai.domain.Organization;
import com.retailai.domain.Product;
import com.retailai.domain.Store;
import com.retailai.dto.inventory.*;
import com.retailai.exception.DuplicateResourceException;
import com.retailai.exception.OrganizationNotFoundException;
import com.retailai.exception.ResourceNotFoundException;
import com.retailai.repository.InventoryRepository;
import com.retailai.repository.OrganizationRepository;
import com.retailai.repository.ProductRepository;
import com.retailai.repository.StoreRepository;
import com.retailai.security.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final OrganizationRepository organizationRepository;

    public InventoryService(
        InventoryRepository inventoryRepository,
        StoreRepository storeRepository,
        ProductRepository productRepository,
        OrganizationRepository organizationRepository
    ) {
        this.inventoryRepository = inventoryRepository;
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.organizationRepository = organizationRepository;
    }

    @Transactional
    public InventoryResponseDto createInventory(CreateInventoryRequestDto request) {
        Long tenantId = TenantContext.getTenantId();
        Organization organization = organizationRepository.findById(tenantId)
                .orElseThrow(() -> new OrganizationNotFoundException("Organization not found for ID: " + tenantId));

        Store store = storeRepository.findByOrganizationIdAndId(tenantId, request.getStoreId())
                .orElseThrow(() -> new ResourceNotFoundException("Store not found for ID: " + request.getStoreId()));

        Product product = productRepository.findByOrganizationIdAndId(tenantId, request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found for ID: " + request.getProductId()));

        if (inventoryRepository.findByOrganizationIdAndStoreIdAndProductId(tenantId, request.getStoreId(), request.getProductId()).isPresent()) {
            throw new DuplicateResourceException("Inventory position already exists for this store and product");
        }

        Inventory inventory = new Inventory(organization, store, product, request.getQuantityOnHand(), request.getQuantityOnOrder());
        Inventory savedInventory = inventoryRepository.save(inventory);

        return mapToDto(savedInventory);
    }

    @Transactional(readOnly = true)
    public Page<InventoryResponseDto> getInventory(Long storeId, Long productId, Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        return inventoryRepository.findWithFilters(tenantId, storeId, productId, pageable).map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public InventoryResponseDto getInventoryById(Long id) {
        Long tenantId = TenantContext.getTenantId();
        Inventory inventory = inventoryRepository.findByOrganizationIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory position not found with ID: " + id));
        return mapToDto(inventory);
    }

    @Transactional
    public InventoryResponseDto updateInventory(Long id, UpdateInventoryRequestDto request) {
        Long tenantId = TenantContext.getTenantId();
        Inventory inventory = inventoryRepository.findByOrganizationIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory position not found with ID: " + id));

        if (request.getQuantityOnHand() != null) {
            inventory.setQuantityOnHand(request.getQuantityOnHand());
        }
        if (request.getQuantityOnOrder() != null) {
            inventory.setQuantityOnOrder(request.getQuantityOnOrder());
        }

        Inventory updatedInventory = inventoryRepository.save(inventory);
        return mapToDto(updatedInventory);
    }

    @Transactional(readOnly = true)
    public InventoryAssessmentResponseDto assessInventory(InventoryAssessmentRequestDto request) {
        Long tenantId = TenantContext.getTenantId();

        Store store = storeRepository.findByOrganizationIdAndId(tenantId, request.getStoreId())
                .orElseThrow(() -> new ResourceNotFoundException("Store not found for ID: " + request.getStoreId()));

        Product product = productRepository.findByOrganizationIdAndId(tenantId, request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found for ID: " + request.getProductId()));

        Optional<Inventory> optInventory = inventoryRepository.findByOrganizationIdAndStoreIdAndProductId(
            tenantId, request.getStoreId(), request.getProductId()
        );

        int currentInventory = optInventory.map(Inventory::getQuantityOnHand).orElse(0);
        int existingOnOrder = (request.getExistingOnOrder() != null)
                ? request.getExistingOnOrder()
                : optInventory.map(Inventory::getQuantityOnOrder).orElse(0);

        double totalForecastDemand = request.getForecastDemand();
        int forecastHorizonDays = request.getForecastHorizonDays();
        double avgDailyDemand = totalForecastDemand / forecastHorizonDays;
        int leadTimeDays = (request.getLeadTimeDays() != null) ? request.getLeadTimeDays() : 7;
        double leadTimeDemand = avgDailyDemand * leadTimeDays;

        int safetyStock = (request.getSafetyStock() != null)
                ? request.getSafetyStock()
                : (int) Math.round(1.65 * Math.sqrt(leadTimeDays) * avgDailyDemand);

        int reorderPoint = (int) Math.round(leadTimeDemand) + safetyStock;

        // Stockout risk classification (Phase 5 exact rules)
        String stockoutRisk;
        if (currentInventory < leadTimeDemand) {
            stockoutRisk = "HIGH";
        } else if (currentInventory < reorderPoint) {
            stockoutRisk = "MEDIUM";
        } else {
            stockoutRisk = "LOW";
        }

        // Inventory status classification
        String inventoryStatus;
        if (currentInventory == 0) {
            inventoryStatus = "OUT_OF_STOCK";
        } else if (currentInventory < reorderPoint) {
            inventoryStatus = "BELOW_REORDER_POINT";
        } else {
            inventoryStatus = "ADEQUATE";
        }

        // Order recommendation (Phase 5 MOQ and net demand deduction rules)
        int rawOrder = (int) Math.max(0, Math.ceil(totalForecastDemand + safetyStock - currentInventory - existingOnOrder));
        int recommendedOrder;
        if (rawOrder > 0 && request.getMoq() != null && request.getMoq() > 0) {
            recommendedOrder = Math.max(rawOrder, request.getMoq());
        } else {
            recommendedOrder = rawOrder;
        }

        return new InventoryAssessmentResponseDto(
            store.getId(),
            store.getStoreCode(),
            product.getId(),
            product.getSku(),
            totalForecastDemand,
            avgDailyDemand,
            forecastHorizonDays,
            leadTimeDays,
            leadTimeDemand,
            safetyStock,
            reorderPoint,
            currentInventory,
            existingOnOrder,
            stockoutRisk,
            inventoryStatus,
            recommendedOrder
        );
    }

    public InventoryResponseDto mapToDto(Inventory inventory) {
        return new InventoryResponseDto(
            inventory.getId(),
            inventory.getStore().getId(),
            inventory.getStore().getStoreCode(),
            inventory.getProduct().getId(),
            inventory.getProduct().getSku(),
            inventory.getQuantityOnHand(),
            inventory.getQuantityOnOrder(),
            inventory.getOrganization().getId(),
            inventory.getUpdatedAt()
        );
    }
}
