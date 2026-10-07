package com.retailai.service;

import com.retailai.domain.Organization;
import com.retailai.domain.Product;
import com.retailai.domain.Sale;
import com.retailai.domain.Store;
import com.retailai.dto.sale.CreateSaleRequestDto;
import com.retailai.dto.sale.SaleResponseDto;
import com.retailai.exception.DuplicateResourceException;
import com.retailai.exception.OrganizationNotFoundException;
import com.retailai.exception.ResourceNotFoundException;
import com.retailai.repository.OrganizationRepository;
import com.retailai.repository.ProductRepository;
import com.retailai.repository.SaleRepository;
import com.retailai.repository.StoreRepository;
import com.retailai.security.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final OrganizationRepository organizationRepository;

    public SaleService(
        SaleRepository saleRepository,
        StoreRepository storeRepository,
        ProductRepository productRepository,
        OrganizationRepository organizationRepository
    ) {
        this.saleRepository = saleRepository;
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.organizationRepository = organizationRepository;
    }

    @Transactional
    public SaleResponseDto createSale(CreateSaleRequestDto request) {
        Long tenantId = TenantContext.getTenantId();
        Organization organization = organizationRepository.findById(tenantId)
                .orElseThrow(() -> new OrganizationNotFoundException("Organization not found for ID: " + tenantId));

        Store store = storeRepository.findByOrganizationIdAndId(tenantId, request.getStoreId())
                .orElseThrow(() -> new ResourceNotFoundException("Store not found for ID: " + request.getStoreId()));

        Product product = productRepository.findByOrganizationIdAndId(tenantId, request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found for ID: " + request.getProductId()));

        if (saleRepository.findByOrganizationIdAndStoreIdAndProductIdAndSaleDate(tenantId, request.getStoreId(), request.getProductId(), request.getSaleDate()).isPresent()) {
            throw new DuplicateResourceException("Sale record already exists for this store, product, and sale date");
        }

        Sale sale = new Sale(organization, store, product, request.getSaleDate(), request.getUnitsSold(), request.getUnitPrice());
        Sale savedSale = saleRepository.save(sale);

        return mapToDto(savedSale);
    }

    @Transactional(readOnly = true)
    public Page<SaleResponseDto> getSales(Long storeId, Long productId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        return saleRepository.findWithFilters(tenantId, storeId, productId, startDate, endDate, pageable).map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public SaleResponseDto getSaleById(Long id) {
        Long tenantId = TenantContext.getTenantId();
        Sale sale = saleRepository.findByOrganizationIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale record not found with ID: " + id));
        return mapToDto(sale);
    }

    public SaleResponseDto mapToDto(Sale sale) {
        return new SaleResponseDto(
            sale.getId(),
            sale.getStore().getId(),
            sale.getStore().getStoreCode(),
            sale.getProduct().getId(),
            sale.getProduct().getSku(),
            sale.getSaleDate(),
            sale.getUnitsSold(),
            sale.getUnitPrice(),
            sale.getOrganization().getId(),
            sale.getCreatedAt()
        );
    }
}
