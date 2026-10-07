package com.retailai.repository;

import com.retailai.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByOrganizationId(Long organizationId);
    Page<Product> findByOrganizationId(Long organizationId, Pageable pageable);
    Optional<Product> findByOrganizationIdAndId(Long organizationId, Long id);
    Optional<Product> findByOrganizationIdAndSku(Long organizationId, String sku);
    boolean existsByOrganizationIdAndSku(Long organizationId, String sku);
    boolean existsByOrganizationIdAndSkuAndIdNot(Long organizationId, String sku, Long id);
}
