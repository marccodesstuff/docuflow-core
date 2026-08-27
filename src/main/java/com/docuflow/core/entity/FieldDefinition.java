package com.docuflow.core.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "field_definitions", indexes = {
    @Index(name = "idx_field_definitions_document_type_id", columnList = "document_type_id"),
    @Index(name = "idx_field_definitions_parent_id", columnList = "parent_id")
})
public class FieldDefinition {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_type_id", nullable = false)
    private DocumentType documentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private FieldDefinition parent;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "field_key", nullable = false, length = 100)
    private String key;

    @Column(name = "label", nullable = false, length = 200)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private FieldType type;

    @Column(name = "required", nullable = false)
    private Boolean required = false;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "regex_pattern", length = 500)
    private String regexPattern;

    @Column(name = "enum_values", columnDefinition = "jsonb")
    private String enumValues;  // JSON array

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    private List<FieldDefinition> children = new ArrayList<>();

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public DocumentType getDocumentType() { return documentType; }
    public void setDocumentType(DocumentType documentType) { this.documentType = documentType; }
    public FieldDefinition getParent() { return parent; }
    public void setParent(FieldDefinition parent) { this.parent = parent; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public FieldType getType() { return type; }
    public void setType(FieldType type) { this.type = type; }
    public Boolean getRequired() { return required; }
    public void setRequired(Boolean required) { this.required = required; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRegexPattern() { return regexPattern; }
    public void setRegexPattern(String regexPattern) { this.regexPattern = regexPattern; }
    public String getEnumValues() { return enumValues; }
    public void setEnumValues(String enumValues) { this.enumValues = enumValues; }
    public List<FieldDefinition> getChildren() { return children; }
    public void setChildren(List<FieldDefinition> children) { this.children = children; }
}