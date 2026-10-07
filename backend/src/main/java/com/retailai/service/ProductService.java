package com.retailai.service;

import com.retailai.domain.Organization;
import com.retailai.domain.Product;
import com.retailai.dto.product.CreateProductRequestDto;
import com.retailai.dto.product.ProductResponseDto;
import com.retailai.dto.product.UpdateProductRequestDto;
import com.retailai.exception.DuplicateResourceException;
import com.retailai.exception.OrganizationNotFoundException;
import com.retailai.exception.ResourceNotFoundException;
import com.retailai.repository.OrganizationRepository;
import com.retailai.repository.ProductRepository;
import com.retailai.security.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final OrganizationRepository organizationRepository;

    public ProductService(ProductRepository productRepository, OrganizationRepository organizationRepository) {
        this.productRepository = productRepository;
        this.organizationRepository = organizationRepository;
    }

    @Transactional
    public ProductResponseDto createProduct(CreateProductRequestDto request) {
        Long tenantId = TenantContext.getTenantId();
        Organization organization = organizationRepository.findById(tenantId)
                .orElseThrow(() -> new OrganizationNotFoundException("Organization not found for ID: " + tenantId));

        if (productRepository.existsByOrganizationIdAndSku(tenantId, request.getSku())) {
            throw new DuplicateResourceException("Product SKU '" + request.getSku() + "' already exists in this organization");
        }

        Product product = new Product(organization, request.getSku(), request.getName(), request.getCategory(), request.getDepartment());
        Product savedProduct = productRepository.save(product);

        return mapToDto(savedProduct);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponseDto> getProducts(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        return productRepository.findByOrganizationId(tenantId, pageable).map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public ProductResponseDto getProductById(Long id) {
        Long tenantId = TenantContext.getTenantId();
        Product product = productRepository.findByOrganizationIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
        return mapToDto(product);
    }

    @Transactional
    public ProductResponseDto updateProduct(Long id, UpdateProductRequestDto request) {
        Long tenantId = TenantContext.getTenantId();
        Product product = productRepository.findByOrganizationIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        if (request.getSku() != null && !request.getSku().isBlank()) {
            if (productRepository.existsByOrganizationIdAndSkuAndIdNot(tenantId, request.getSku(), id)) {
                throw new DuplicateResourceException("Product SKU '" + request.getSku() + "' already exists in this organization");
            }
            product.setSku(request.getSku());
        }
        if (request.getName() != null && !request.getName().isBlank()) {
            product.setName(request.getName());
        }
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            product.setCategory(request.getCategory());
        }
        if (request.getDepartment() != null && !request.getDepartment().isBlank()) {
            product.setDepartment(request.getDepartment());
        }

        Product updatedProduct = productRepository.save(product);
        return mapToDto(updatedProduct);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Long tenantId = TenantContext.getTenantId();
        Product product = productRepository.findByOrganizationIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
        productRepository.delete(product);
    }

    public ProductResponseDto mapToDto(Product product) {
        return new ProductResponseDto(
            product.getId(),
            product.getSku(),
            product.getName(),
            product.getCategory(),
            product.getDepartment(),
            product.getOrganization().getId(),
            product.getCreatedAt()
        );
    }
}
