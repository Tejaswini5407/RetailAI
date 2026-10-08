package com.retailai.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "forecast_history")
public class ForecastHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotBlank
    @Column(name = "forecast_run_id", nullable = false, length = 50)
    private String forecastRunId;

    @NotNull
    @Column(name = "forecast_date", nullable = false)
    private LocalDate forecastDate;

    @NotNull
    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(name = "horizon_days", nullable = false)
    private Integer horizonDays = 1;

    @NotNull
    @Column(name = "predicted_demand", nullable = false, columnDefinition = "NUMERIC(10, 4)")
    private Double predictedDemand;

    @NotNull
    @Column(name = "rounded_demand", nullable = false)
    private Integer roundedDemand;

    @NotBlank
    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public ForecastHistory() {}

    public ForecastHistory(Organization organization, Store store, Product product, String forecastRunId, LocalDate forecastDate, LocalDate targetDate, Integer horizonDays, Double predictedDemand, Integer roundedDemand, String modelName, String modelVersion) {
        this.organization = organization;
        this.store = store;
        this.product = product;
        this.forecastRunId = forecastRunId;
        this.forecastDate = forecastDate;
        this.targetDate = targetDate;
        this.horizonDays = horizonDays != null ? horizonDays : 1;
        this.predictedDemand = predictedDemand;
        this.roundedDemand = roundedDemand;
        this.modelName = modelName;
        this.modelVersion = modelVersion;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Organization getOrganization() { return organization; }
    public void setOrganization(Organization organization) { this.organization = organization; }

    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public String getForecastRunId() { return forecastRunId; }
    public void setForecastRunId(String forecastRunId) { this.forecastRunId = forecastRunId; }

    public LocalDate getForecastDate() { return forecastDate; }
    public void setForecastDate(LocalDate forecastDate) { this.forecastDate = forecastDate; }

    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }

    public Integer getHorizonDays() { return horizonDays; }
    public void setHorizonDays(Integer horizonDays) { this.horizonDays = horizonDays; }

    public Double getPredictedDemand() { return predictedDemand; }
    public void setPredictedDemand(Double predictedDemand) { this.predictedDemand = predictedDemand; }

    public Integer getRoundedDemand() { return roundedDemand; }
    public void setRoundedDemand(Integer roundedDemand) { this.roundedDemand = roundedDemand; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
