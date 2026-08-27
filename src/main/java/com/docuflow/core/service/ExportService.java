package com.docuflow.core.service;

import com.docuflow.core.entity.*;
import com.docuflow.core.repository.ExtractionResultRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class ExportService {

    private final ExtractionResultRepository extractionResultRepository;
    private final StorageService storageService;
    private final ObjectMapper jsonMapper;
    private final CsvMapper csvMapper;

    public ExportService(ExtractionResultRepository extractionResultRepository,
                          StorageService storageService,
                          ObjectMapper jsonMapper) {
        this.extractionResultRepository = extractionResultRepository;
        this.storageService = storageService;
        this.jsonMapper = jsonMapper;
        this.csvMapper = new CsvMapper();
    }

    public ExportResult export(ExtractionResult extraction, Document document, ExportFormat format) throws IOException {
        byte[] content;
        String filename;
        String contentType;

        switch (format) {
            case JSON -> {
                content = exportJson(extraction, document);
                filename = document.getOriginalFilename().replaceFirst("\\.[^.]+$", "") + ".json";
                contentType = "application/json";
            }
            case CSV -> {
                content = exportCsv(extraction);
                filename = document.getOriginalFilename().replaceFirst("\\.[^.]+$", "") + ".csv";
                contentType = "text/csv";
            }
            case XML -> {
                content = exportXml(extraction, document);
                filename = document.getOriginalFilename().replaceFirst("\\.[^.]+$", "") + ".xml";
                contentType = "application/xml";
            }
            case XLSX -> {
                content = exportXlsx(extraction);
                filename = document.getOriginalFilename().replaceFirst("\\.[^.]+$", "") + ".xlsx";
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            }
            case PDF -> {
                content = exportPdf(extraction, document);
                filename = document.getOriginalFilename().replaceFirst("\\.[^.]+$", "") + ".pdf";
                contentType = "application/pdf";
            }
            default -> throw new IllegalArgumentException("Unsupported export format: " + format);
        }

        // Upload to storage
        String storagePath = "exports/" + document.getTenantId() + "/" + document.getId() + "/" + filename;
        storageService.upload(storagePath, content, contentType);

        // Save export record (could be a separate entity)
        ExportResult result = new ExportResult();
        result.setExtractionResultId(extraction.getId());
        result.setFormat(format);
        result.setStoragePath(storagePath);
        result.setFilename(filename);
        result.setSizeBytes((long) content.length);
        result.setContentType(contentType);

        return result;
    }

    private byte[] exportJson(ExtractionResult extraction, Document document) throws IOException {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("document_id", document.getId().toString());
        output.put("document_type_id", document.getDocumentTypeId().toString());
        output.put("original_filename", document.getOriginalFilename());
        output.put("extracted_at", extraction.getExtractedAt().toString());
        output.put("overall_confidence", extraction.getOverallConfidence());
        output.put("model_version", extraction.getModelVersion());
        
        Map<String, Object> fields = new LinkedHashMap<>();
        for (var entry : extraction.getFields().entrySet()) {
            Map<String, Object> fieldData = new LinkedHashMap<>();
            fieldData.put("value", entry.getValue().getValue());
            fieldData.put("confidence", entry.getValue().getConfidence());
            fieldData.put("validated", entry.getValue().getValidated());
            if (entry.getValue().getValidationError() != null) {
                fieldData.put("validation_error", entry.getValue().getValidationError());
            }
            fields.put(entry.getKey(), fieldData);
        }
        output.put("fields", fields);
        
        if (!extraction.getTables().isEmpty()) {
            List<Map<String, Object>> tables = new ArrayList<>();
            for (TableExtraction table : extraction.getTables()) {
                Map<String, Object> tableData = new LinkedHashMap<>();
                tableData.put("table_id", table.getTableId());
                tableData.put("headers", table.getHeaders());
                tableData.put("confidence", table.getConfidence());
                
                List<List<Object>> rows = new ArrayList<>();
                for (TableRow row : table.getRows()) {
                    rows.add(new ArrayList<>(row.getCells()));
                }
                tableData.put("rows", rows);
                tables.add(tableData);
            }
            output.put("tables", tables);
        }
        
        return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(output);
    }

    private byte[] exportCsv(ExtractionResult extraction) throws IOException {
        // Flatten fields for CSV
        List<Map<String, String>> rows = new ArrayList<>();
        
        Map<String, String> headerRow = new LinkedHashMap<>();
        headerRow.put("field_key", "value");
        rows.add(headerRow);
        
        for (var entry : extraction.getFields().entrySet()) {
            Map<String, String> row = new LinkedHashMap<>();
            row.put("field_key", entry.getValue().getValue() != null ? entry.getValue().getValue() : "");
            rows.add(row);
        }
        
        CsvSchema schema = csvMapper.schemaFor(Map.class).withHeader();
        return csvMapper.writer(schema).writeValueAsBytes(rows);
    }

    private byte[] exportXml(ExtractionResult extraction, Document document) throws IOException {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<extraction>\n");
        xml.append("  <document_id>").append(document.getId()).append("</document_id>\n");
        xml.append("  <document_type_id>").append(document.getDocumentTypeId()).append("</document_type_id>\n");
        xml.append("  <original_filename>").append(escapeXml(document.getOriginalFilename())).append("</original_filename>\n");
        xml.append("  <extracted_at>").append(extraction.getExtractedAt()).append("</extracted_at>\n");
        xml.append("  <overall_confidence>").append(extraction.getOverallConfidence()).append("</overall_confidence>\n");
        xml.append("  <model_version>").append(escapeXml(extraction.getModelVersion())).append("</model_version>\n");
        xml.append("  <fields>\n");
        
        for (var entry : extraction.getFields().entrySet()) {
            xml.append("    <field key=\"").append(escapeXml(entry.getKey())).append("\">\n");
            xml.append("      <value>").append(escapeXml(entry.getValue().getValue())).append("</value>\n");
            xml.append("      <confidence>").append(entry.getValue().getConfidence()).append("</confidence>\n");
            xml.append("      <validated>").append(entry.getValue().getValidated()).append("</validated>\n");
            if (entry.getValue().getValidationError() != null) {
                xml.append("      <validation_error>").append(escapeXml(entry.getValue().getValidationError())).append("</validation_error>\n");
            }
            xml.append("    </field>\n");
        }
        
        xml.append("  </fields>\n");
        
        if (!extraction.getTables().isEmpty()) {
            xml.append("  <tables>\n");
            for (TableExtraction table : extraction.getTables()) {
                xml.append("    <table id=\"").append(escapeXml(table.getTableId())).append("\">\n");
                xml.append("      <headers>");
                for (String header : table.getHeaders()) {
                    xml.append("<header>").append(escapeXml(header)).append("</header>");
                }
                xml.append("</headers>\n");
                xml.append("      <rows>\n");
                for (TableRow row : table.getRows()) {
                    xml.append("        <row>");
                    for (String cell : row.getCells()) {
                        xml.append("<cell>").append(escapeXml(cell)).append("</cell>");
                    }
                    xml.append("</row>\n");
                }
                xml.append("      </rows>\n");
                xml.append("    </table>\n");
            }
            xml.append("  </tables>\n");
        }
        
        xml.append("</extraction>\n");
        return xml.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] exportXlsx(ExtractionResult extraction) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            // Fields sheet
            Sheet fieldsSheet = workbook.createSheet("Fields");
            Row headerRow = fieldsSheet.createRow(0);
            headerRow.createCell(0).setCellValue("Field Key");
            headerRow.createCell(1).setCellValue("Value");
            headerRow.createCell(2).setCellValue("Confidence");
            headerRow.createCell(3).setCellValue("Validated");
            headerRow.createCell(4).setCellValue("Validation Error");
            
            int rowNum = 1;
            for (var entry : extraction.getFields().entrySet()) {
                Row row = fieldsSheet.createRow(rowNum++);
                row.createCell(0).setCellValue(entry.getKey());
                row.createCell(1).setCellValue(entry.getValue().getValue() != null ? entry.getValue().getValue() : "");
                row.createCell(2).setCellValue(entry.getValue().getConfidence() != null ? entry.getValue().getConfidence() : 0);
                row.createCell(3).setCellValue(entry.getValue().getValidated());
                row.createCell(4).setCellValue(entry.getValue().getValidationError() != null ? entry.getValue().getValidationError() : "");
            }
            
            // Auto-size columns
            for (int i = 0; i < 5; i++) {
                fieldsSheet.autoSizeColumn(i);
            }
            
            // Tables sheet(s)
            for (TableExtraction table : extraction.getTables()) {
                Sheet tableSheet = workbook.createSheet(truncateSheetName(table.getTableId()));
                Row tableHeader = tableSheet.createRow(0);
                for (int i = 0; i < table.getHeaders().size(); i++) {
                    tableHeader.createCell(i).setCellValue(table.getHeaders().get(i));
                }
                
                int tableRowNum = 1;
                for (TableRow row : table.getRows()) {
                    Row dataRow = tableSheet.createRow(tableRowNum++);
                    for (int i = 0; i < row.getCells().size(); i++) {
                        dataRow.createCell(i).setCellValue(row.getCells().get(i));
                    }
                }
                
                for (int i = 0; i < table.getHeaders().size(); i++) {
                    tableSheet.autoSizeColumn(i);
                }
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    private byte[] exportPdf(ExtractionResult extraction, Document document) throws IOException {
        // Simplified - would use iText or Apache PDFBox in production
        StringBuilder content = new StringBuilder();
        content.append("DocuFlow Extraction Report\n");
        content.append("==========================\n\n");
        content.append("Document: ").append(document.getOriginalFilename()).append("\n");
        content.append("Document ID: ").append(document.getId()).append("\n");
        content.append("Extracted: ").append(extraction.getExtractedAt()).append("\n");
        content.append("Confidence: ").append(String.format("%.2f%%", extraction.getOverallConfidence() * 100)).append("\n");
        content.append("Model: ").append(extraction.getModelVersion()).append("\n\n");
        content.append("Extracted Fields:\n");
        content.append("-----------------\n");
        
        for (var entry : extraction.getFields().entrySet()) {
            content.append(entry.getKey()).append(": ")
                   .append(entry.getValue().getValue())
                   .append(" (confidence: ")
                   .append(String.format("%.2f%%", entry.getValue().getConfidence() * 100))
                   .append(")\n");
        }
        
        return content.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&")
                .replace("<", "<")
                .replace(">", ">")
                .replace("\"", """)
                .replace("'", "&apos;");
    }

    private String truncateSheetName(String name) {
        if (name == null) return "Table";
        return name.length() > 31 ? name.substring(0, 31) : name;
    }

    public enum ExportFormat { JSON, CSV, XML, XLSX, PDF }

    public static class ExportResult {
        private UUID extractionResultId;
        private ExportFormat format;
        private String storagePath;
        private String filename;
        private Long sizeBytes;
        private String contentType;

        // Getters and setters
        public UUID getExtractionResultId() { return extractionResultId; }
        public void setExtractionResultId(UUID extractionResultId) { this.extractionResultId = extractionResultId; }
        public ExportFormat getFormat() { return format; }
        public void setFormat(ExportFormat format) { this.format = format; }
        public String getStoragePath() { return storagePath; }
        public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }
        public Long getSizeBytes() { return sizeBytes; }
        public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
    }
}