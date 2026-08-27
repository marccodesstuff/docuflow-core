package com.docuflow.core.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "document_types", indexes = {
    @Index(name = "idx_document_types_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_document_types_name", columnList = "name")
})
public class DocumentType {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @OneToMany(mappedBy = "documentType", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    private List<FieldDefinition> fields = new ArrayList<>();

    @Embedded
    private ValidationRules validationRules;

    @Embedded
    private RoutingRules routingRules;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<FieldDefinition> getFields() { return fields; }
    public void setFields(List<FieldDefinition> fields) { this.fields = fields; }
    public ValidationRules getValidationRules() { return validationRules; }
    public void setValidationRules(ValidationRules validationRules) { this.validationRules = validationRules; }
    public RoutingRules getRoutingRules() { return routingRules; }
    public void setRoutingRules(RoutingRules routingRules) { this.routingRules = routingRules; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}