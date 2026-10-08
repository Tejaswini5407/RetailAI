package com.retailai.service;

import com.retailai.domain.*;
import com.retailai.dto.importing.*;
import com.retailai.exception.ResourceNotFoundException;
import com.retailai.repository.*;
import com.retailai.security.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class ImportService {

    private static final Logger logger = LoggerFactory.getLogger(ImportService.class);

    private final OrganizationRepository organizationRepository;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final InventoryRepository inventoryRepository;
    private final ImportHistoryRepository importHistoryRepository;

    public ImportService(
        OrganizationRepository organizationRepository,
        StoreRepository storeRepository,
        ProductRepository productRepository,
        SaleRepository saleRepository,
        InventoryRepository inventoryRepository,
        ImportHistoryRepository importHistoryRepository
    ) {
        this.organizationRepository = organizationRepository;
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.saleRepository = saleRepository;
        this.inventoryRepository = inventoryRepository;
        this.importHistoryRepository = importHistoryRepository;
    }

    @Transactional(readOnly = true)
    public ImportPreviewResponseDto previewImport(MultipartFile file, String importType) {
        Long tenantId = TenantContext.getTenantId();
        List<Map<String, String>> rows = parseCsv(file);

        List<ValidationErrorDto> errors = new ArrayList<>();
        List<Map<String, String>> previewData = new ArrayList<>();

        int totalRows = rows.size();
        int validRows = 0;
        int invalidRows = 0;
        int duplicateRows = 0;

        String type = importType != null ? importType.toUpperCase() : "PRODUCTS";

        // Caches for duplicate checks within tenant context
        Set<String> seenKeys = new HashSet<>();

        // Fetch existing tenant entities to validate references
        Map<String, Store> existingStores = new HashMap<>();
        storeRepository.findByOrganizationId(tenantId).forEach(s -> existingStores.put(s.getStoreCode().toUpperCase(), s));

        Map<String, Product> existingProducts = new HashMap<>();
        productRepository.findByOrganizationId(tenantId).forEach(p -> existingProducts.put(p.getSku().toUpperCase(), p));

        for (int i = 0; i < rows.size(); i++) {
            int rowNum = i + 2; // Header is row 1
            Map<String, String> row = rows.get(i);
            boolean rowValid = true;

            switch (type) {
                case "PRODUCTS": {
                    String sku = getVal(row, "sku");
                    String name = getVal(row, "name");
                    String category = getVal(row, "category");
                    String department = getVal(row, "department");

                    if (sku == null || sku.isBlank()) {
                        errors.add(new ValidationErrorDto(rowNum, "sku", "SKU is required"));
                        rowValid = false;
                    } else if (existingProducts.containsKey(sku.toUpperCase()) || seenKeys.contains(sku.toUpperCase())) {
                        duplicateRows++;
                        errors.add(new ValidationErrorDto(rowNum, "sku", "Duplicate SKU '" + sku + "' already exists in organization or file"));
                        rowValid = false;
                    }

                    if (name == null || name.isBlank()) {
                        errors.add(new ValidationErrorDto(rowNum, "name", "Product name is required"));
                        rowValid = false;
                    }
                    if (category == null || category.isBlank()) {
                        errors.add(new ValidationErrorDto(rowNum, "category", "Category is required"));
                        rowValid = false;
                    }
                    if (department == null || department.isBlank()) {
                        errors.add(new ValidationErrorDto(rowNum, "department", "Department is required"));
                        rowValid = false;
                    }

                    if (rowValid) {
                        seenKeys.add(sku.toUpperCase());
                        validRows++;
                        previewData.add(row);
                    } else {
                        invalidRows++;
                    }
                    break;
                }
                case "STORES": {
                    String storeCode = getVal(row, "store_code", "storecode");
                    String name = getVal(row, "name");
                    String state = getVal(row, "state");
                    String city = getVal(row, "city", "location");

                    if (storeCode == null || storeCode.isBlank()) {
                        errors.add(new ValidationErrorDto(rowNum, "store_code", "Store code is required"));
                        rowValid = false;
                    } else if (existingStores.containsKey(storeCode.toUpperCase()) || seenKeys.contains(storeCode.toUpperCase())) {
                        duplicateRows++;
                        errors.add(new ValidationErrorDto(rowNum, "store_code", "Duplicate store code '" + storeCode + "' already exists in organization or file"));
                        rowValid = false;
                    }

                    if (name == null || name.isBlank()) {
                        errors.add(new ValidationErrorDto(rowNum, "name", "Store name is required"));
                        rowValid = false;
                    }
                    if (state == null || state.isBlank()) {
                        errors.add(new ValidationErrorDto(rowNum, "state", "State is required"));
                        rowValid = false;
                    }
                    if (city == null || city.isBlank()) {
                        errors.add(new ValidationErrorDto(rowNum, "city", "City/Location is required"));
                        rowValid = false;
                    }

                    if (rowValid) {
                        seenKeys.add(storeCode.toUpperCase());
                        validRows++;
                        previewData.add(row);
                    } else {
                        invalidRows++;
                    }
                    break;
                }
                case "SALES": {
                    String dateStr = getVal(row, "date", "saledate", "sale_date");
                    String storeCode = getVal(row, "store_code", "storecode");
                    String sku = getVal(row, "sku");
                    String unitsSoldStr = getVal(row, "units_sold", "unitssold");
                    String unitPriceStr = getVal(row, "unit_price", "unitprice");

                    if (dateStr == null || dateStr.isBlank()) {
                        errors.add(new ValidationErrorDto(rowNum, "date", "Sale date is required"));
                        rowValid = false;
                    } else {
                        try {
                            LocalDate.parse(dateStr);
                        } catch (DateTimeParseException e) {
                            errors.add(new ValidationErrorDto(rowNum, "date", "Invalid date format (expected YYYY-MM-DD)"));
                            rowValid = false;
                        }
                    }

                    if (storeCode == null || !existingStores.containsKey(storeCode.toUpperCase())) {
                        errors.add(new ValidationErrorDto(rowNum, "store_code", "Store '" + storeCode + "' does not exist in current organization"));
                        rowValid = false;
                    }

                    if (sku == null || !existingProducts.containsKey(sku.toUpperCase())) {
                        errors.add(new ValidationErrorDto(rowNum, "sku", "SKU '" + sku + "' does not exist in current organization"));
                        rowValid = false;
                    }

                    if (unitsSoldStr != null) {
                        try {
                            int units = Integer.parseInt(unitsSoldStr);
                            if (units < 0) {
                                errors.add(new ValidationErrorDto(rowNum, "units_sold", "units_sold cannot be negative"));
                                rowValid = false;
                            }
                        } catch (NumberFormatException e) {
                            errors.add(new ValidationErrorDto(rowNum, "units_sold", "units_sold must be a valid integer"));
                            rowValid = false;
                        }
                    } else {
                        errors.add(new ValidationErrorDto(rowNum, "units_sold", "units_sold is required"));
                        rowValid = false;
                    }

                    if (unitPriceStr != null && !unitPriceStr.isBlank()) {
                        try {
                            double price = Double.parseDouble(unitPriceStr);
                            if (price < 0) {
                                errors.add(new ValidationErrorDto(rowNum, "unit_price", "unit_price cannot be negative"));
                                rowValid = false;
                            }
                        } catch (NumberFormatException e) {
                            errors.add(new ValidationErrorDto(rowNum, "unit_price", "unit_price must be a valid number"));
                            rowValid = false;
                        }
                    }

                    String compositeKey = storeCode + "|" + sku + "|" + dateStr;
                    if (seenKeys.contains(compositeKey)) {
                        duplicateRows++;
                        errors.add(new ValidationErrorDto(rowNum, "sale", "Duplicate sale record for store " + storeCode + ", SKU " + sku + " on date " + dateStr));
                        rowValid = false;
                    }

                    if (rowValid) {
                        seenKeys.add(compositeKey);
                        validRows++;
                        previewData.add(row);
                    } else {
                        invalidRows++;
                    }
                    break;
                }
                case "INVENTORY": {
                    String storeCode = getVal(row, "store_code", "storecode");
                    String sku = getVal(row, "sku");
                    String qtyHandStr = getVal(row, "quantity_on_hand", "quantityonhand");
                    String qtyOrderStr = getVal(row, "quantity_on_order", "quantityonorder");

                    if (storeCode == null || !existingStores.containsKey(storeCode.toUpperCase())) {
                        errors.add(new ValidationErrorDto(rowNum, "store_code", "Store '" + storeCode + "' does not exist in current organization"));
                        rowValid = false;
                    }

                    if (sku == null || !existingProducts.containsKey(sku.toUpperCase())) {
                        errors.add(new ValidationErrorDto(rowNum, "sku", "SKU '" + sku + "' does not exist in current organization"));
                        rowValid = false;
                    }

                    if (qtyHandStr != null) {
                        try {
                            int qty = Integer.parseInt(qtyHandStr);
                            if (qty < 0) {
                                errors.add(new ValidationErrorDto(rowNum, "quantity_on_hand", "quantity_on_hand cannot be negative"));
                                rowValid = false;
                            }
                        } catch (NumberFormatException e) {
                            errors.add(new ValidationErrorDto(rowNum, "quantity_on_hand", "quantity_on_hand must be an integer"));
                            rowValid = false;
                        }
                    } else {
                        errors.add(new ValidationErrorDto(rowNum, "quantity_on_hand", "quantity_on_hand is required"));
                        rowValid = false;
                    }

                    if (qtyOrderStr != null) {
                        try {
                            int qty = Integer.parseInt(qtyOrderStr);
                            if (qty < 0) {
                                errors.add(new ValidationErrorDto(rowNum, "quantity_on_order", "quantity_on_order cannot be negative"));
                                rowValid = false;
                            }
                        } catch (NumberFormatException e) {
                            errors.add(new ValidationErrorDto(rowNum, "quantity_on_order", "quantity_on_order must be an integer"));
                            rowValid = false;
                        }
                    }

                    String compositeKey = storeCode + "|" + sku;
                    if (seenKeys.contains(compositeKey)) {
                        duplicateRows++;
                        errors.add(new ValidationErrorDto(rowNum, "inventory", "Duplicate inventory row for store " + storeCode + " and SKU " + sku));
                        rowValid = false;
                    }

                    if (rowValid) {
                        seenKeys.add(compositeKey);
                        validRows++;
                        previewData.add(row);
                    } else {
                        invalidRows++;
                    }
                    break;
                }
            }
        }

        int importableRows = validRows;
        return new ImportPreviewResponseDto(totalRows, validRows, invalidRows, duplicateRows, importableRows, errors, previewData);
    }

    @Transactional
    public ImportSummaryResponseDto confirmImport(ImportConfirmRequestDto request) {
        Long tenantId = TenantContext.getTenantId();
        Organization org = organizationRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found for ID: " + tenantId));

        String type = request.getImportType() != null ? request.getImportType().toUpperCase() : "PRODUCTS";
        List<Map<String, String>> rows = request.getRows();

        int totalRows = rows != null ? rows.size() : 0;
        int successful = 0;
        int failed = 0;
        StringBuilder errorMsg = new StringBuilder();

        Map<String, Store> storeMap = new HashMap<>();
        storeRepository.findByOrganizationId(tenantId).forEach(s -> storeMap.put(s.getStoreCode().toUpperCase(), s));

        Map<String, Product> productMap = new HashMap<>();
        productRepository.findByOrganizationId(tenantId).forEach(p -> productMap.put(p.getSku().toUpperCase(), p));

        if (rows != null) {
            for (Map<String, String> row : rows) {
                try {
                    switch (type) {
                        case "PRODUCTS": {
                            String sku = getVal(row, "sku");
                            String name = getVal(row, "name");
                            String category = getVal(row, "category");
                            String department = getVal(row, "department");

                            if (!productMap.containsKey(sku.toUpperCase())) {
                                Product p = new Product(org, sku, name, category, department);
                                p = productRepository.save(p);
                                productMap.put(sku.toUpperCase(), p);
                                successful++;
                            } else {
                                failed++;
                            }
                            break;
                        }
                        case "STORES": {
                            String storeCode = getVal(row, "store_code", "storecode");
                            String name = getVal(row, "name");
                            String state = getVal(row, "state");
                            String city = getVal(row, "city", "location");

                            if (!storeMap.containsKey(storeCode.toUpperCase())) {
                                Store s = new Store(org, storeCode, name, state, city);
                                s = storeRepository.save(s);
                                storeMap.put(storeCode.toUpperCase(), s);
                                successful++;
                            } else {
                                failed++;
                            }
                            break;
                        }
                        case "SALES": {
                            String dateStr = getVal(row, "date", "saledate", "sale_date");
                            String storeCode = getVal(row, "store_code", "storecode");
                            String sku = getVal(row, "sku");
                            int unitsSold = Integer.parseInt(getVal(row, "units_sold", "unitssold"));
                            String priceStr = getVal(row, "unit_price", "unitprice");
                            BigDecimal unitPrice = priceStr != null && !priceStr.isBlank() ? new BigDecimal(priceStr) : null;

                            Store store = storeMap.get(storeCode.toUpperCase());
                            Product product = productMap.get(sku.toUpperCase());

                            if (store != null && product != null) {
                                LocalDate saleDate = LocalDate.parse(dateStr);
                                Optional<Sale> existing = saleRepository.findByOrganizationIdAndStoreIdAndProductIdAndSaleDate(tenantId, store.getId(), product.getId(), saleDate);
                                if (existing.isPresent()) {
                                    Sale s = existing.get();
                                    s.setUnitsSold(unitsSold);
                                    if (unitPrice != null) s.setUnitPrice(unitPrice);
                                    saleRepository.save(s);
                                } else {
                                    Sale s = new Sale(org, store, product, saleDate, unitsSold, unitPrice);
                                    saleRepository.save(s);
                                }
                                successful++;
                            } else {
                                failed++;
                            }
                            break;
                        }
                        case "INVENTORY": {
                            String storeCode = getVal(row, "store_code", "storecode");
                            String sku = getVal(row, "sku");
                            int qtyHand = Integer.parseInt(getVal(row, "quantity_on_hand", "quantityonhand"));
                            int qtyOrder = Integer.parseInt(getVal(row, "quantity_on_order", "quantityonorder", "0"));

                            Store store = storeMap.get(storeCode.toUpperCase());
                            Product product = productMap.get(sku.toUpperCase());

                            if (store != null && product != null) {
                                Optional<Inventory> existing = inventoryRepository.findByOrganizationIdAndStoreIdAndProductId(tenantId, store.getId(), product.getId());
                                if (existing.isPresent()) {
                                    Inventory inv = existing.get();
                                    inv.setQuantityOnHand(qtyHand);
                                    inv.setQuantityOnOrder(qtyOrder);
                                    inventoryRepository.save(inv);
                                } else {
                                    Inventory inv = new Inventory(org, store, product, qtyHand, qtyOrder);
                                    inventoryRepository.save(inv);
                                }
                                successful++;
                            } else {
                                failed++;
                            }
                            break;
                        }
                    }
                } catch (Exception e) {
                    failed++;
                    logger.warn("Import row execution error: {}", e.getMessage());
                }
            }
        }

        String status = failed == 0 ? "COMPLETED" : (successful > 0 ? "COMPLETED_WITH_ERRORS" : "FAILED");
        ImportHistory history = new ImportHistory(
            org,
            type,
            request.getFileName() != null ? request.getFileName() : "import.csv",
            totalRows,
            successful,
            failed,
            status,
            failed > 0 ? failed + " rows failed processing during import" : null
        );
        history = importHistoryRepository.save(history);

        return new ImportSummaryResponseDto(history.getId(), type, history.getFileName(), totalRows, successful, failed, status, history.getErrorSummary());
    }

    @Transactional(readOnly = true)
    public List<ImportHistoryResponseDto> getImportHistory() {
        Long tenantId = TenantContext.getTenantId();
        List<ImportHistory> list = importHistoryRepository.findByOrganizationIdOrderByUploadedAtDesc(tenantId);
        List<ImportHistoryResponseDto> result = new ArrayList<>();
        for (ImportHistory h : list) {
            result.add(new ImportHistoryResponseDto(
                h.getId(),
                h.getImportType(),
                h.getFileName(),
                h.getTotalRows(),
                h.getSuccessfulRows(),
                h.getFailedRows(),
                h.getStatus(),
                h.getErrorSummary(),
                h.getUploadedAt()
            ));
        }
        return result;
    }

    public String getTemplateCsv(String importType) {
        String type = importType != null ? importType.toUpperCase() : "PRODUCTS";
        switch (type) {
            case "PRODUCTS":
                return "sku,name,category,department\nFOODS_1_001,Organic Whole Milk,Dairy,Foods\n";
            case "STORES":
                return "store_code,name,state,city\nSTORE-CA-1,Los Angeles Main Store,CA,Los Angeles\n";
            case "SALES":
                return "date,store_code,sku,units_sold,unit_price\n2026-10-01,STORE-CA-1,FOODS_1_001,45,3.99\n";
            case "INVENTORY":
                return "store_code,sku,quantity_on_hand,quantity_on_order\nSTORE-CA-1,FOODS_1_001,120,50\n";
            default:
                return "sku,name,category,department\n";
        }
    }

    private List<Map<String, String>> parseCsv(MultipartFile file) {
        List<Map<String, String>> list = new ArrayList<>();
        if (file == null || file.isEmpty()) return list;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) return list;

            String[] headers = headerLine.split(",");
            for (int i = 0; i < headers.length; i++) {
                headers[i] = headers[i].trim().replaceAll("^\"|\"$", "").toLowerCase();
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] values = line.split(",", -1);
                Map<String, String> row = new HashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    String val = i < values.length ? values[i].trim().replaceAll("^\"|\"$", "") : "";
                    row.put(headers[i], val);
                }
                list.add(row);
            }
        } catch (Exception e) {
            logger.error("CSV parse failure: {}", e.getMessage(), e);
        }
        return list;
    }

    private String getVal(Map<String, String> row, String... keys) {
        for (String k : keys) {
            if (row.containsKey(k.toLowerCase())) {
                String v = row.get(k.toLowerCase());
                if (v != null && !v.isBlank()) return v;
            }
        }
        return null;
    }
}
