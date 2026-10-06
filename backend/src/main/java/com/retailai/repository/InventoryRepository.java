package com.retailai.repository;

import com.retailai.domain.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    List<Inventory> findByOrganizationId(Long organizationId);
    Optional<Inventory> findByOrganizationIdAndStoreIdAndProductId(
        Long organizationId, Long storeId, Long productId
    );
}
