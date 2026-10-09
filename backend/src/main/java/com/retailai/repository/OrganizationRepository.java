package com.retailai.repository;

import com.retailai.domain.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Optional<Organization> findByCode(String code);
    Optional<Organization> findByCodeIgnoreCase(String code);
    boolean existsByCode(String code);
    boolean existsByCodeIgnoreCase(String code);
}
