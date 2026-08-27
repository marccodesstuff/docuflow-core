package com.docuflow.core.service;

import com.docuflow.core.entity.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class ValidationService {

    @Value("${docuflow.pipeline.validate.auto-approve-enabled:false}")
    private boolean autoApproveEnabled;

    @Value("${docuflow.pipeline.validate.auto-approve-confidence:0.95}")
    private float autoApproveConfidence;

    public ValidationResult validate(ExtractionResult extraction, DocumentType documentType) {
        List<ValidationError> errors = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();

        // 1. Required field validation
        for (FieldDefinition field : documentType.getFields()) {
            if (field.getRequired()) {
                ExtractedField extracted = extraction.getFields().get(field.getKey());
                if (extracted == null || extracted.getValue() == null || extracted.getValue().isBlank()) {
                    errors.add(new ValidationError(field.getKey(), "Required field is missing or empty"));
                }
            }
        }

        // 2. Regex pattern validation
        if (documentType.getValidationRules() != null && 
            documentType.getValidationRules().getFieldRules() != null) {
            for (var entry : documentType.getValidationRules().getFieldRules().entrySet()) {
                String fieldKey = entry.getKey();
                String patternStr = entry.getValue();
                
                ExtractedField extracted = extraction.getFields().get(fieldKey);
                if (extracted != null && extracted.getValue() != null) {
                    try {
                        Pattern pattern = Pattern.compile(patternStr);
                        if (!pattern.matcher(extracted.getValue()).matches()) {
                            errors.add(new ValidationError(fieldKey, 
                                "Value does not match required pattern: " + patternStr));
                        }
                    } catch (Exception e) {
                        warnings.add(new ValidationWarning(fieldKey, 
                            "Invalid validation pattern: " + patternStr));
                    }
                }
            }
        }

        // 3. Cross-field validation
        if (documentType.getValidationRules() != null && 
            documentType.getValidationRules().getCrossFieldRules() != null) {
            for (var rule : documentType.getValidationRules().getCrossFieldRules()) {
                try {
                    // Simple expression evaluation - in production use MVEL or similar
                    boolean result = evaluateExpression(rule.getExpression(), extraction.getFields());
                    if (!result) {
                        errors.add(new ValidationError(
                            String.join(",", rule.getAffectedFields()),
                            rule.getErrorMessage()));
                    }
                } catch (Exception e) {
                    warnings.add(new ValidationWarning(
                        String.join(",", rule.getAffectedFields()),
                        "Cross-field validation error: " + e.getMessage()));
                }
            }
        }

        // 4. Confidence threshold check
        boolean meetsConfidence = extraction.getOverallConfidence() != null && 
            extraction.getOverallConfidence() >= autoApproveConfidence;
        
        boolean autoApprove = autoApproveEnabled && meetsConfidence && errors.isEmpty();

        // 5. Check for extraction issues
        if (extraction.getIssues() != null) {
            // Parse issues JSON and add warnings
            // Simplified for now
        }

        return new ValidationResult(
            errors.isEmpty(),
            autoApprove,
            errors,
            warnings
        );
    }

    private boolean evaluateExpression(String expression, Map<String, ExtractedField> fields) {
        // Very simplified expression evaluator for demo
        // In production: use MVEL, JEXL, or similar expression language
        
        // Example: "total == sum(line_items[*].amount)"
        // For now, just check if all referenced fields exist
        String[] refs = expression.split("[^a-zA-Z0-9_*]+");
        for (String ref : refs) {
            if (!ref.isEmpty() && !ref.matches("\\d+") && !fields.containsKey(ref)) {
                return false;
            }
        }
        return true;
    }

    public static class ValidationResult {
        private final boolean valid;
        private final boolean autoApprove;
        private final List<ValidationError> errors;
        private final List<ValidationWarning> warnings;

        public ValidationResult(boolean valid, boolean autoApprove,
                                 List<ValidationError> errors, List<ValidationWarning> warnings) {
            this.valid = valid;
            this.autoApprove = autoApprove;
            this.errors = errors;
            this.warnings = warnings;
        }

        public boolean isValid() { return valid; }
        public boolean getAutoApprove() { return autoApprove; }
        public List<ValidationError> getErrors() { return errors; }
        public List<ValidationWarning> getWarnings() { return warnings; }
    }

    public static class ValidationError {
        private final String fieldKey;
        private final String message;

        public ValidationError(String fieldKey, String message) {
            this.fieldKey = fieldKey;
            this.message = message;
        }

        public String getFieldKey() { return fieldKey; }
        public String getMessage() { return message; }
    }

    public static class ValidationWarning {
        private final String fieldKey;
        private final String message;

        public ValidationWarning(String fieldKey, String message) {
            this.fieldKey = fieldKey;
            this.message = message;
        }

        public String getFieldKey() { return fieldKey; }
        public String getMessage() { return message; }
    }
}