package com.retailai.repository;

import com.retailai.domain.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {
    List<Store> findByOrganizationId(Long organizationId);
    Optional<Store> findByOrganizationIdAndStoreCode(Long organizationId, String storeCode);
    boolean existsByOrganizationIdAndStoreCode(Long organizationId, String storeCode);
}
