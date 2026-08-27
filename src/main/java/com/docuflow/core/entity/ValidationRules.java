package com.docuflow.core.entity;

import jakarta.persistence.*;
import java.util.HashMap;
import java.util.Map;

@Embeddable
public class ValidationRules {

    @ElementCollection
    @CollectionTable(name = "validation_field_rules", joinColumns = @JoinColumn(name = "document_type_id"))
    @MapKeyColumn(name = "field_key")
    @Column(name = "rule")
    private Map<String, String> fieldRules = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "validation_cross_field_rules", joinColumns = @JoinColumn(name = "document_type_id"))
    private java.util.List<CrossFieldRule> crossFieldRules = new java.util.ArrayList<>();

    // Getters and setters
    public Map<String, String> getFieldRules() { return fieldRules; }
    public void setFieldRules(Map<String, String> fieldRules) { this.fieldRules = fieldRules; }
    public java.util.List<CrossFieldRule> getCrossFieldRules() { return crossFieldRules; }
    public void setCrossFieldRules(java.util.List<CrossFieldRule> crossFieldRules) { this.crossFieldRules = crossFieldRules; }

    @Embeddable
    public static class CrossFieldRule {
        @Column(name = "expression", length = 1000)
        private String expression;

        @Column(name = "error_message", length = 500)
        private String errorMessage;

        @ElementCollection
        @CollectionTable(name = "cross_field_rule_affected_fields", joinColumns = @JoinColumn(name = "cross_field_rule_id"))
        @Column(name = "field_key")
        private java.util.List<String> affectedFields = new java.util.ArrayList<>();

        public String getExpression() { return expression; }
        public void setExpression(String expression) { this.expression = expression; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public java.util.List<String> getAffectedFields() { return affectedFields; }
        public void setAffectedFields(java.util.List<String> affectedFields) { this.affectedFields = affectedFields; }
    }
}