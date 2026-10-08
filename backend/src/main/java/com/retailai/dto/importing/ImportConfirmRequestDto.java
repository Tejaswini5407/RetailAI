package com.retailai.dto.importing;

import java.util.List;
import java.util.Map;

public class ImportConfirmRequestDto {
    private String importType;
    private String fileName;
    private List<Map<String, String>> rows;

    public ImportConfirmRequestDto() {}

    public ImportConfirmRequestDto(String importType, String fileName, List<Map<String, String>> rows) {
        this.importType = importType;
        this.fileName = fileName;
        this.rows = rows;
    }

    public String getImportType() { return importType; }
    public void setImportType(String importType) { this.importType = importType; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public List<Map<String, String>> getRows() { return rows; }
    public void setRows(List<Map<String, String>> rows) { this.rows = rows; }
}
