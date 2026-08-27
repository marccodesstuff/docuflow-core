package com.docuflow.core.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "extraction_results", indexes = {
    @Index(name = "idx_extraction_results_job_id", columnList = "job_id", unique = true),
    @Index(name = "idx_extraction_results_document_id", columnList = "document_id"),
    @Index(name = "idx_extraction_results_status", columnList = "status")
})
public class ExtractionResult {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "job_id", nullable = false, unique = true)
    private UUID jobId;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ExtractionStatus status;

    @ElementCollection
    @CollectionTable(name = "extracted_fields", joinColumns = @JoinColumn(name = "extraction_result_id"))
    @MapKeyColumn(name = "field_key")
    @Column(name = "field_value", columnDefinition = "jsonb")
    private Map<String, ExtractedField> fields = new java.util.HashMap<>();

    @OneToMany(mappedBy = "extractionResult", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TableExtraction> tables = new ArrayList<>();

    @Column(name = "overall_confidence")
    private Float overallConfidence;

    @Column(name = "model_version", length = 100)
    private String modelVersion;

    @Column(name = "issues", columnDefinition = "jsonb")
    private String issues;  // JSON array of ExtractionIssue

    @CreationTimestamp
    @Column(name = "extracted_at", nullable = false, updatable = false)
    private Instant extractedAt;

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getJobId() { return jobId; }
    public void setJobId(UUID jobId) { this.jobId = jobId; }
    public UUID getDocumentId() { return documentId; }
    public void setDocumentId(UUID documentId) { this.documentId = documentId; }
    public ExtractionStatus getStatus() { return status; }
    public void setStatus(ExtractionStatus status) { this.status = status; }
    public Map<String, ExtractedField> getFields() { return fields; }
    public void setFields(Map<String, ExtractedField> fields) { this.fields = fields; }
    public List<TableExtraction> getTables() { return tables; }
    public void setTables(List<TableExtraction> tables) { this.tables = tables; }
    public Float getOverallConfidence() { return overallConfidence; }
    public void setOverallConfidence(Float overallConfidence) { this.overallConfidence = overallConfidence; }
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    public String getIssues() { return issues; }
    public void setIssues(String issues) { this.issues = issues; }
    public Instant getExtractedAt() { return extractedAt; }
    public void setExtractedAt(Instant extractedAt) { this.extractedAt = extractedAt; }
}