package com.docuflow.core.service;

import com.docuflow.core.entity.*;
import com.docuflow.shared.proto.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExtractionService {

    private final MLInferenceClient mlInferenceClient;

    public ExtractionService(MLInferenceClient mlInferenceClient) {
        this.mlInferenceClient = mlInferenceClient;
    }

    public ExtractionResult extractDocument(String documentId, String storagePath, String mimeType,
                                             String documentTypeId, List<FieldDefinition> fieldDefinitions) {
        // Convert field definitions to proto format
        Map<String, FieldDefinition> fieldSchema = fieldDefinitions.stream()
            .collect(Collectors.toMap(FieldDefinition::getKey, fd -> fd));

        ExtractFieldsResponse response = mlInferenceClient.extractFields(
            documentId, storagePath, mimeType, documentTypeId, fieldSchema);

        // Convert response to internal format
        ExtractionResult result = new ExtractionResult();
        result.setStatus(convertExtractionStatus(response.getOverallConfidence()));
        result.setOverallConfidence(response.getOverallConfidence());
        result.setModelVersion(response.getModelVersion());
        
        // Convert fields
        Map<String, ExtractedField> fields = new HashMap<>();
        for (var entry : response.getFieldsMap().entrySet()) {
            fields.put(entry.getKey(), convertExtractedField(entry.getValue()));
        }
        result.setFields(fields);
        
        // Convert tables
        List<TableExtraction> tables = response.getTablesList().stream()
            .map(this::convertTableExtraction)
            .collect(Collectors.toList());
        result.setTables(tables);
        
        // Convert issues
        String issuesJson = convertIssues(response.getIssuesList());
        result.setIssues(issuesJson);
        
        return result;
    }

    private ExtractionStatus convertExtractionStatus(float confidence) {
        if (confidence >= 0.9) return ExtractionStatus.SUCCESS;
        if (confidence >= 0.7) return ExtractionStatus.PARTIAL;
        if (confidence >= 0.5) return ExtractionStatus.REVIEW_NEEDED;
        return ExtractionStatus.FAILED;
    }

    private ExtractedField convertExtractedField(com.docuflow.shared.proto.ExtractedField proto) {
        ExtractedField field = new ExtractedField();
        field.setValue(proto.getValue());
        field.setConfidence(proto.getConfidence());
        
        if (proto.hasBbox()) {
            BoundingBox bbox = new BoundingBox();
            bbox.setX(proto.getBbox().getX());
            bbox.setY(proto.getBbox().getY());
            bbox.setWidth(proto.getBbox().getWidth());
            bbox.setHeight(proto.getBbox().getHeight());
            bbox.setElementType(proto.getBbox().getElementType());
            bbox.setConfidence(proto.getBbox().getConfidence());
            field.setBbox(bbox);
        }
        
        field.setValidated(proto.getValidated());
        field.setValidationError(proto.getValidationError());
        field.setAlternatives(new ArrayList<>(proto.getAlternativesList()));
        
        return field;
    }

    private TableExtraction convertTableExtraction(com.docuflow.shared.proto.TableExtraction proto) {
        TableExtraction table = new TableExtraction();
        table.setTableId(proto.getTableId());
        table.setHeaders(new ArrayList<>(proto.getHeadersList()));
        table.setConfidence(proto.getConfidence());
        
        if (proto.hasBbox()) {
            BoundingBox bbox = new BoundingBox();
            bbox.setX(proto.getBbox().getX());
            bbox.setY(proto.getBbox().getY());
            bbox.setWidth(proto.getBbox().getWidth());
            bbox.setHeight(proto.getBbox().getHeight());
            bbox.setElementType(proto.getBbox().getElementType());
            bbox.setConfidence(proto.getBbox().getConfidence());
            table.setBbox(bbox);
        }
        
        List<TableRow> rows = proto.getRowsList().stream()
            .map(this::convertTableRow)
            .collect(Collectors.toList());
        table.setRows(rows);
        
        return table;
    }

    private TableRow convertTableRow(com.docuflow.shared.proto.TableRow proto) {
        TableRow row = new TableRow();
        row.setCells(new ArrayList<>(proto.getCellsList()));
        row.setCellConfidences(new ArrayList<>(proto.getCellConfidencesList()));
        return row;
    }

    private String convertIssues(List<com.docuflow.shared.proto.ExtractionIssue> issues) {
        if (issues.isEmpty()) return "[]";
        
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < issues.size(); i++) {
            var issue = issues.get(i);
            sb.append(String.format("{\"field_key\":\"%s\",\"severity\":\"%s\",\"message\":\"%s\",\"suggested_fix\":\"%s\"}",
                escapeJson(issue.getFieldKey()),
                issue.getSeverity().name(),
                escapeJson(issue.getMessage()),
                escapeJson(issue.getSuggestedFix())));
            if (i < issues.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}