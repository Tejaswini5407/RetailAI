package com.retailai.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(
    name = "stores",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_stores_org_store_code", columnNames = {"organization_id", "store_code"}),
        @UniqueConstraint(name = "uk_stores_org_id", columnNames = {"organization_id", "id"})
    }
)
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Organization is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @NotBlank(message = "Store code is required")
    @Size(max = 50, message = "Store code cannot exceed 50 characters")
    @Column(name = "store_code", nullable = false, length = 50)
    private String storeCode;

    @NotBlank(message = "Store name is required")
    @Size(max = 100, message = "Store name cannot exceed 100 characters")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @NotBlank(message = "State is required")
    @Size(max = 10, message = "State cannot exceed 10 characters")
    @Column(name = "state", nullable = false, length = 10)
    private String state;

    @NotBlank(message = "City is required")
    @Size(max = 50, message = "City cannot exceed 50 characters")
    @Column(name = "city", nullable = false, length = 50)
    private String city;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Store() {}

    public Store(Organization organization, String storeCode, String name, String state, String city) {
        this.organization = organization;
        this.storeCode = storeCode;
        this.name = name;
        this.state = state;
        this.city = city;
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Organization getOrganization() { return organization; }
    public void setOrganization(Organization organization) { this.organization = organization; }

    public String getStoreCode() { return storeCode; }
    public void setStoreCode(String storeCode) { this.storeCode = storeCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Store store = (Store) o;
        return Objects.equals(id, store.id) ||
               (Objects.equals(organization, store.organization) && Objects.equals(storeCode, store.storeCode));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, storeCode);
    }
}
