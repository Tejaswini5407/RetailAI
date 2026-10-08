package com.retailai.dto.importing;

import java.time.OffsetDateTime;

public class ImportSummaryResponseDto {
    private Long importHistoryId;
    private String importType;
    private String fileName;
    private int totalRows;
    private int successfulRows;
    private int failedRows;
    private String status;
    private String errorSummary;

    public ImportSummaryResponseDto() {}

    public ImportSummaryResponseDto(Long importHistoryId, String importType, String fileName, int totalRows, int successfulRows, int failedRows, String status, String errorSummary) {
        this.importHistoryId = importHistoryId;
        this.importType = importType;
        this.fileName = fileName;
        this.totalRows = totalRows;
        this.successfulRows = successfulRows;
        this.failedRows = failedRows;
        this.status = status;
        this.errorSummary = errorSummary;
    }

    public Long getImportHistoryId() { return importHistoryId; }
    public void setImportHistoryId(Long importHistoryId) { this.importHistoryId = importHistoryId; }

    public String getImportType() { return importType; }
    public void setImportType(String importType) { this.importType = importType; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }

    public int getSuccessfulRows() { return successfulRows; }
    public void setSuccessfulRows(int successfulRows) { this.successfulRows = successfulRows; }

    public int getFailedRows() { return failedRows; }
    public void setFailedRows(int failedRows) { this.failedRows = failedRows; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorSummary() { return errorSummary; }
    public void setErrorSummary(String errorSummary) { this.errorSummary = errorSummary; }
}
