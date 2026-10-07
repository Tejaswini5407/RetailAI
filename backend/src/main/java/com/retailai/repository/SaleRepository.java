package com.retailai.repository;

import com.retailai.domain.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {
    List<Sale> findByOrganizationId(Long organizationId);
    Page<Sale> findByOrganizationId(Long organizationId, Pageable pageable);
    Optional<Sale> findByOrganizationIdAndId(Long organizationId, Long id);
    List<Sale> findByOrganizationIdAndStoreIdAndProductId(Long organizationId, Long storeId, Long productId);
    Optional<Sale> findByOrganizationIdAndStoreIdAndProductIdAndSaleDate(
        Long organizationId, Long storeId, Long productId, LocalDate saleDate
    );

    @Query("""
        SELECT s FROM Sale s
        WHERE s.organization.id = :orgId
          AND (:storeId IS NULL OR s.store.id = :storeId)
          AND (:productId IS NULL OR s.product.id = :productId)
          AND (:startDate IS NULL OR s.saleDate >= :startDate)
          AND (:endDate IS NULL OR s.saleDate <= :endDate)
    """)
    Page<Sale> findWithFilters(
        @Param("orgId") Long orgId,
        @Param("storeId") Long storeId,
        @Param("productId") Long productId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        Pageable pageable
    );
}
