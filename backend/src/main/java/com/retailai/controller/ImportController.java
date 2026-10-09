package com.retailai.controller;

import com.retailai.dto.importing.*;
import com.retailai.service.ImportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/imports")
public class ImportController {

    private final ImportService importService;

    public ImportController(ImportService importService) {
        this.importService = importService;
    }

    @PostMapping("/preview")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'INVENTORY_MANAGER')")
    public ResponseEntity<ImportPreviewResponseDto> previewImport(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "PRODUCTS") String importType
    ) {
        ImportPreviewResponseDto preview = importService.previewImport(file, importType);
        return ResponseEntity.ok(preview);
    }

    @PostMapping("/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'INVENTORY_MANAGER')")
    public ResponseEntity<ImportSummaryResponseDto> confirmImport(
            @RequestBody ImportConfirmRequestDto request
    ) {
        ImportSummaryResponseDto summary = importService.confirmImport(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(summary);
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'INVENTORY_MANAGER')")
    public ResponseEntity<List<ImportHistoryResponseDto>> getImportHistory() {
        List<ImportHistoryResponseDto> history = importService.getImportHistory();
        return ResponseEntity.ok(history);
    }

    @GetMapping("/templates/{type}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'INVENTORY_MANAGER')")
    public ResponseEntity<String> downloadTemplate(@PathVariable("type") String type) {
        String csvContent = importService.getTemplateCsv(type);
        String fileName;
        String t = type != null ? type.toUpperCase() : "PRODUCTS";
        switch (t) {
            case "PRODUCTS":
                fileName = "products_template.csv";
                break;
            case "STORES":
                fileName = "stores_template.csv";
                break;
            case "SALES":
            case "HISTORICAL_SALES":
                fileName = "historical_sales_template.csv";
                break;
            case "INVENTORY":
                fileName = "inventory_template.csv";
                break;
            default:
                fileName = type.toLowerCase() + "_template.csv";
                break;
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvContent);
    }
}
