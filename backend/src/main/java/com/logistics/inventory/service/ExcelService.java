package com.logistics.inventory.service;

import com.logistics.inventory.entity.*;
import com.logistics.inventory.exception.BadRequestException;
import com.logistics.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExcelService {

    private static final String[] PRODUCT_HEADERS = {
            "SKU", "Name", "Description", "Category", "Supplier",
            "Unit price", "Reorder level", "Active", "Total quantity"};
    private static final String[] STOCK_HEADERS = {"SKU", "Warehouse code", "Quantity"};
    private static final int MAX_IMPORT_ROWS = 5000;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final StockLevelRepository stockLevelRepository;
    private final AuditService auditService;

    public record ImportResult(int created, int updated, List<String> errors) {}

    // ---------- Export ----------

    @Transactional(readOnly = true)
    public byte[] exportInventory() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle headerStyle = headerStyle(workbook);

            Sheet products = workbook.createSheet("Products");
            writeHeader(products, PRODUCT_HEADERS, headerStyle);
            int rowIdx = 1;
            for (Product p : productRepository.findAll(Sort.by("sku"))) {
                Row row = products.createRow(rowIdx++);
                row.createCell(0).setCellValue(p.getSku());
                row.createCell(1).setCellValue(p.getName());
                row.createCell(2).setCellValue(p.getDescription() == null ? "" : p.getDescription());
                row.createCell(3).setCellValue(p.getCategory() == null ? "" : p.getCategory().getName());
                row.createCell(4).setCellValue(p.getSupplier() == null ? "" : p.getSupplier().getName());
                row.createCell(5).setCellValue(p.getUnitPrice().doubleValue());
                row.createCell(6).setCellValue(p.getReorderLevel());
                row.createCell(7).setCellValue(p.isActive() ? "YES" : "NO");
                row.createCell(8).setCellValue(stockLevelRepository.totalQuantityForProduct(p.getId()));
            }
            autosize(products, PRODUCT_HEADERS.length);

            Sheet stock = workbook.createSheet("Stock");
            writeHeader(stock, STOCK_HEADERS, headerStyle);
            rowIdx = 1;
            for (StockLevel level : stockLevelRepository.findAll()) {
                Row row = stock.createRow(rowIdx++);
                row.createCell(0).setCellValue(level.getProduct().getSku());
                row.createCell(1).setCellValue(level.getWarehouse().getCode());
                row.createCell(2).setCellValue(level.getQuantity());
            }
            autosize(stock, STOCK_HEADERS.length);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate Excel export", e);
        }
    }

    // ---------- Import ----------

    /**
     * Upserts products by SKU from the first sheet. Expected columns (header row required):
     * SKU | Name | Description | Category | Supplier | Unit price | Reorder level.
     * Unknown categories/suppliers are created by name. Stock quantities are NOT imported —
     * stock changes must go through movements so the audit trail stays truthful.
     */
    @Transactional
    public ImportResult importProducts(InputStream in) {
        List<String> errors = new ArrayList<>();
        int created = 0;
        int updated = 0;

        try (Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getLastRowNum() < 1) {
                throw new BadRequestException("The file contains no data rows");
            }
            if (sheet.getLastRowNum() > MAX_IMPORT_ROWS) {
                throw new BadRequestException("Import limited to " + MAX_IMPORT_ROWS + " rows per file");
            }
            DataFormatter fmt = new DataFormatter();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String sku = fmt.formatCellValue(row.getCell(0)).trim();
                String name = fmt.formatCellValue(row.getCell(1)).trim();
                if (sku.isEmpty() && name.isEmpty()) continue; // blank row
                int rowNo = i + 1;
                if (sku.isEmpty() || name.isEmpty()) {
                    errors.add("Row " + rowNo + ": SKU and Name are required");
                    continue;
                }
                try {
                    Product existing = productRepository.findBySkuIgnoreCase(sku).orElse(null);
                    boolean isNew = existing == null;
                    Product product = isNew ? new Product() : existing;
                    product.setSku(sku);
                    product.setName(name);
                    product.setDescription(emptyToNull(fmt.formatCellValue(row.getCell(2)).trim()));
                    product.setCategory(resolveCategory(fmt.formatCellValue(row.getCell(3)).trim()));
                    product.setSupplier(resolveSupplier(fmt.formatCellValue(row.getCell(4)).trim()));
                    product.setUnitPrice(parsePrice(fmt.formatCellValue(row.getCell(5)).trim(), rowNo));
                    product.setReorderLevel(parseIntOrZero(fmt.formatCellValue(row.getCell(6)).trim(), rowNo));
                    productRepository.save(product);
                    if (isNew) created++; else updated++;
                } catch (BadRequestException e) {
                    errors.add(e.getMessage());
                }
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Could not read the file — is it a valid .xlsx workbook?");
        }

        auditService.record("PRODUCTS_IMPORTED", "Product", null,
                created + " created, " + updated + " updated, " + errors.size() + " errors");
        return new ImportResult(created, updated, errors);
    }

    // ---------- helpers ----------

    private Category resolveCategory(String name) {
        if (name.isEmpty()) return null;
        return categoryRepository.findAll().stream()
                .filter(c -> c.getName().equalsIgnoreCase(name)).findFirst()
                .orElseGet(() -> categoryRepository.save(Category.builder().name(name).build()));
    }

    private Supplier resolveSupplier(String name) {
        if (name.isEmpty()) return null;
        return supplierRepository.findAll().stream()
                .filter(s -> s.getName().equalsIgnoreCase(name)).findFirst()
                .orElseGet(() -> supplierRepository.save(Supplier.builder().name(name).build()));
    }

    private BigDecimal parsePrice(String raw, int rowNo) {
        if (raw.isEmpty()) return BigDecimal.ZERO;
        try {
            BigDecimal value = new BigDecimal(raw.replace(",", "."));
            if (value.signum() < 0) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException e) {
            throw new BadRequestException("Row " + rowNo + ": invalid unit price '" + raw + "'");
        }
    }

    private int parseIntOrZero(String raw, int rowNo) {
        if (raw.isEmpty()) return 0;
        try {
            int value = (int) Double.parseDouble(raw.replace(",", "."));
            if (value < 0) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException e) {
            throw new BadRequestException("Row " + rowNo + ": invalid reorder level '" + raw + "'");
        }
    }

    private String emptyToNull(String s) {
        return s.isEmpty() ? null : s;
    }

    private CellStyle headerStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private void writeHeader(Sheet sheet, String[] headers, CellStyle style) {
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private void autosize(Sheet sheet, int columns) {
        for (int i = 0; i < columns; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
