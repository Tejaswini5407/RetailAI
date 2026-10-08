package com.retailai.repository;

import com.retailai.domain.ForecastHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForecastHistoryRepository extends JpaRepository<ForecastHistory, Long> {
    Page<ForecastHistory> findByOrganizationId(Long organizationId, Pageable pageable);
    List<ForecastHistory> findByOrganizationIdOrderByCreatedAtDesc(Long organizationId);
    List<ForecastHistory> findByOrganizationIdAndStoreIdAndProductId(Long organizationId, Long storeId, Long productId);
}
