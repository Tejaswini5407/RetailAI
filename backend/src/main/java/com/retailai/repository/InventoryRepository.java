package com.retailai.repository;

import com.retailai.domain.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    List<Inventory> findByOrganizationId(Long organizationId);
    Page<Inventory> findByOrganizationId(Long organizationId, Pageable pageable);
    Optional<Inventory> findByOrganizationIdAndId(Long organizationId, Long id);
    Optional<Inventory> findByOrganizationIdAndStoreIdAndProductId(
        Long organizationId, Long storeId, Long productId
    );

    @Query("""
        SELECT i FROM Inventory i
        WHERE i.organization.id = :orgId
          AND (:storeId IS NULL OR i.store.id = :storeId)
          AND (:productId IS NULL OR i.product.id = :productId)
    """)
    Page<Inventory> findWithFilters(
        @Param("orgId") Long orgId,
        @Param("storeId") Long storeId,
        @Param("productId") Long productId,
        Pageable pageable
    );
}
