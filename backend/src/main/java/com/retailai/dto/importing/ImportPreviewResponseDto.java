package com.retailai.dto.importing;

import java.util.List;
import java.util.Map;

public class ImportPreviewResponseDto {
    private int totalRows;
    private int validRows;
    private int invalidRows;
    private int duplicateRows;
    private int importableRows;
    private List<ValidationErrorDto> validationErrors;
    private List<Map<String, String>> previewData;

    public ImportPreviewResponseDto() {}

    public ImportPreviewResponseDto(int totalRows, int validRows, int invalidRows, int duplicateRows, int importableRows, List<ValidationErrorDto> validationErrors, List<Map<String, String>> previewData) {
        this.totalRows = totalRows;
        this.validRows = validRows;
        this.invalidRows = invalidRows;
        this.duplicateRows = duplicateRows;
        this.importableRows = importableRows;
        this.validationErrors = validationErrors;
        this.previewData = previewData;
    }

    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }

    public int getValidRows() { return validRows; }
    public void setValidRows(int validRows) { this.validRows = validRows; }

    public int getInvalidRows() { return invalidRows; }
    public void setInvalidRows(int invalidRows) { this.invalidRows = invalidRows; }

    public int getDuplicateRows() { return duplicateRows; }
    public void setDuplicateRows(int duplicateRows) { this.duplicateRows = duplicateRows; }

    public int getImportableRows() { return importableRows; }
    public void setImportableRows(int importableRows) { this.importableRows = importableRows; }

    public List<ValidationErrorDto> getValidationErrors() { return validationErrors; }
    public void setValidationErrors(List<ValidationErrorDto> validationErrors) { this.validationErrors = validationErrors; }

    public List<Map<String, String>> getPreviewData() { return previewData; }
    public void setPreviewData(List<Map<String, String>> previewData) { this.previewData = previewData; }
}
