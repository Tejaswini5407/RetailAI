package com.retailai.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(
    name = "sales",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_sales_org_store_prod_date",
            columnNames = {"organization_id", "store_id", "product_id", "sale_date"}
        )
    }
)
public class Sale {

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

    @NotNull(message = "Sale date is required")
    @Column(name = "sale_date", nullable = false)
    private LocalDate saleDate;

    @NotNull(message = "Units sold is required")
    @Min(value = 0, message = "Units sold cannot be negative")
    @Column(name = "units_sold", nullable = false)
    private Integer unitsSold;

    @PositiveOrZero(message = "Unit price must be non-negative")
    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public Sale() {}

    public Sale(Organization organization, Store store, Product product, LocalDate saleDate, Integer unitsSold, BigDecimal unitPrice) {
        this.organization = organization;
        this.store = store;
        this.product = product;
        this.saleDate = saleDate;
        this.unitsSold = unitsSold;
        this.unitPrice = unitPrice;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
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

    public LocalDate getSaleDate() { return saleDate; }
    public void setSaleDate(LocalDate saleDate) { this.saleDate = saleDate; }

    public Integer getUnitsSold() { return unitsSold; }
    public void setUnitsSold(Integer unitsSold) { this.unitsSold = unitsSold; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public OffsetDateTime getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Sale sale = (Sale) o;
        return Objects.equals(id, sale.id) ||
               (Objects.equals(organization, sale.organization) &&
                Objects.equals(store, sale.store) &&
                Objects.equals(product, sale.product) &&
                Objects.equals(saleDate, sale.saleDate));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, saleDate);
    }
}
