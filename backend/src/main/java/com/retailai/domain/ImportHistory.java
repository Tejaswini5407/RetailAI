package com.retailai.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

@Entity
@Table(name = "import_history")
public class ImportHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @NotBlank
    @Column(name = "import_type", nullable = false, length = 50)
    private String importType;

    @NotBlank
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "total_rows", nullable = false)
    private Integer totalRows = 0;

    @Column(name = "successful_rows", nullable = false)
    private Integer successfulRows = 0;

    @Column(name = "failed_rows", nullable = false)
    private Integer failedRows = 0;

    @NotBlank
    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "error_summary", columnDefinition = "TEXT")
    private String errorSummary;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private OffsetDateTime uploadedAt;

    public ImportHistory() {}

    public ImportHistory(Organization organization, String importType, String fileName, Integer totalRows, Integer successfulRows, Integer failedRows, String status, String errorSummary) {
        this.organization = organization;
        this.importType = importType;
        this.fileName = fileName;
        this.totalRows = totalRows;
        this.successfulRows = successfulRows;
        this.failedRows = failedRows;
        this.status = status;
        this.errorSummary = errorSummary;
    }

    @PrePersist
    protected void onCreate() {
        this.uploadedAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Organization getOrganization() { return organization; }
    public void setOrganization(Organization organization) { this.organization = organization; }

    public String getImportType() { return importType; }
    public void setImportType(String importType) { this.importType = importType; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public Integer getTotalRows() { return totalRows; }
    public void setTotalRows(Integer totalRows) { this.totalRows = totalRows; }

    public Integer getSuccessfulRows() { return successfulRows; }
    public void setSuccessfulRows(Integer successfulRows) { this.successfulRows = successfulRows; }

    public Integer getFailedRows() { return failedRows; }
    public void setFailedRows(Integer failedRows) { this.failedRows = failedRows; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorSummary() { return errorSummary; }
    public void setErrorSummary(String errorSummary) { this.errorSummary = errorSummary; }

    public OffsetDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(OffsetDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}
