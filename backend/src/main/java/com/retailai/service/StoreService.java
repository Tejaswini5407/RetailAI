package com.retailai.service;

import com.retailai.domain.Organization;
import com.retailai.domain.Store;
import com.retailai.dto.store.CreateStoreRequestDto;
import com.retailai.dto.store.StoreResponseDto;
import com.retailai.dto.store.UpdateStoreRequestDto;
import com.retailai.exception.DuplicateResourceException;
import com.retailai.exception.OrganizationNotFoundException;
import com.retailai.exception.ResourceNotFoundException;
import com.retailai.repository.OrganizationRepository;
import com.retailai.repository.StoreRepository;
import com.retailai.security.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StoreService {

    private final StoreRepository storeRepository;
    private final OrganizationRepository organizationRepository;

    public StoreService(StoreRepository storeRepository, OrganizationRepository organizationRepository) {
        this.storeRepository = storeRepository;
        this.organizationRepository = organizationRepository;
    }

    @Transactional
    public StoreResponseDto createStore(CreateStoreRequestDto request) {
        Long tenantId = TenantContext.getTenantId();
        Organization organization = organizationRepository.findById(tenantId)
                .orElseThrow(() -> new OrganizationNotFoundException("Organization not found for ID: " + tenantId));

        if (storeRepository.existsByOrganizationIdAndStoreCode(tenantId, request.getStoreCode())) {
            throw new DuplicateResourceException("Store code '" + request.getStoreCode() + "' already exists in this organization");
        }

        Store store = new Store(organization, request.getStoreCode(), request.getName(), request.getState(), request.getCity());
        Store savedStore = storeRepository.save(store);

        return mapToDto(savedStore);
    }

    @Transactional(readOnly = true)
    public Page<StoreResponseDto> getStores(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        return storeRepository.findByOrganizationId(tenantId, pageable).map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public StoreResponseDto getStoreById(Long id) {
        Long tenantId = TenantContext.getTenantId();
        Store store = storeRepository.findByOrganizationIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found with ID: " + id));
        return mapToDto(store);
    }

    @Transactional
    public StoreResponseDto updateStore(Long id, UpdateStoreRequestDto request) {
        Long tenantId = TenantContext.getTenantId();
        Store store = storeRepository.findByOrganizationIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found with ID: " + id));

        if (request.getStoreCode() != null && !request.getStoreCode().isBlank()) {
            if (storeRepository.existsByOrganizationIdAndStoreCodeAndIdNot(tenantId, request.getStoreCode(), id)) {
                throw new DuplicateResourceException("Store code '" + request.getStoreCode() + "' already exists in this organization");
            }
            store.setStoreCode(request.getStoreCode());
        }
        if (request.getName() != null && !request.getName().isBlank()) {
            store.setName(request.getName());
        }
        if (request.getState() != null && !request.getState().isBlank()) {
            store.setState(request.getState());
        }
        if (request.getCity() != null && !request.getCity().isBlank()) {
            store.setCity(request.getCity());
        }

        Store updatedStore = storeRepository.save(store);
        return mapToDto(updatedStore);
    }

    @Transactional
    public void deleteStore(Long id) {
        Long tenantId = TenantContext.getTenantId();
        Store store = storeRepository.findByOrganizationIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found with ID: " + id));
        storeRepository.delete(store);
    }

    public StoreResponseDto mapToDto(Store store) {
        return new StoreResponseDto(
            store.getId(),
            store.getStoreCode(),
            store.getName(),
            store.getState(),
            store.getCity(),
            store.getOrganization().getId(),
            store.getCreatedAt()
        );
    }
}
