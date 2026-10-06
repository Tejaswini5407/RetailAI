package com.retailai.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(
    name = "inventory",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_inventory_org_store_prod",
            columnNames = {"organization_id", "store_id", "product_id"}
        )
    }
)
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Organization is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @NotNull(message = "Store is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @NotNull(message = "Product is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotNull(message = "Quantity on hand is required")
    @Min(value = 0, message = "Quantity on hand cannot be negative")
    @Column(name = "quantity_on_hand", nullable = false)
    private Integer quantityOnHand;

    @NotNull(message = "Quantity on order is required")
    @Min(value = 0, message = "Quantity on order cannot be negative")
    @Column(name = "quantity_on_order", nullable = false)
    private Integer quantityOnOrder;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Inventory() {}

    public Inventory(Organization organization, Store store, Product product, Integer quantityOnHand, Integer quantityOnOrder) {
        this.organization = organization;
        this.store = store;
        this.product = product;
        this.quantityOnHand = quantityOnHand;
        this.quantityOnOrder = quantityOnOrder;
    }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Organization getOrganization() { return organization; }
    public void setOrganization(Organization organization) { this.organization = organization; }

    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Integer getQuantityOnHand() { return quantityOnHand; }
    public void setQuantityOnHand(Integer quantityOnHand) { this.quantityOnHand = quantityOnHand; }

    public Integer getQuantityOnOrder() { return quantityOnOrder; }
    public void setQuantityOnOrder(Integer quantityOnOrder) { this.quantityOnOrder = quantityOnOrder; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Inventory inventory = (Inventory) o;
        return Objects.equals(id, inventory.id) ||
               (Objects.equals(organization, inventory.organization) &&
                Objects.equals(store, inventory.store) &&
                Objects.equals(product, inventory.product));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
