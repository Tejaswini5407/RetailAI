package com.retailai.repository;

import com.retailai.domain.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {
    List<Store> findByOrganizationId(Long organizationId);
    Page<Store> findByOrganizationId(Long organizationId, Pageable pageable);
    Optional<Store> findByOrganizationIdAndId(Long organizationId, Long id);
    Optional<Store> findByOrganizationIdAndStoreCode(Long organizationId, String storeCode);
    boolean existsByOrganizationIdAndStoreCode(Long organizationId, String storeCode);
    boolean existsByOrganizationIdAndStoreCodeAndIdNot(Long organizationId, String storeCode, Long id);
}
