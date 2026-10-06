package com.retailai.repository;

import com.retailai.domain.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {
    List<Sale> findByOrganizationId(Long organizationId);
    List<Sale> findByOrganizationIdAndStoreIdAndProductId(Long organizationId, Long storeId, Long productId);
    Optional<Sale> findByOrganizationIdAndStoreIdAndProductIdAndSaleDate(
        Long organizationId, Long storeId, Long productId, LocalDate saleDate
    );
}
