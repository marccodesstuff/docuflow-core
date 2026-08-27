package com.docuflow.core.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "table_extractions", indexes = {
    @Index(name = "idx_table_extractions_extraction_result_id", columnList = "extraction_result_id")
})
public class TableExtraction {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "extraction_result_id", nullable = false)
    private ExtractionResult extractionResult;

    @Column(name = "table_id", length = 100)
    private String tableId;

    @ElementCollection
    @CollectionTable(name = "table_extraction_headers", joinColumns = @JoinColumn(name = "table_extraction_id"))
    @Column(name = "header")
    @OrderColumn(name = "header_order")
    private List<String> headers = new ArrayList<>();

    @OneToMany(mappedBy = "tableExtraction", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("rowOrder ASC")
    private List<TableRow> rows = new ArrayList<>();

    @Column(name = "confidence")
    private Float confidence;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "x", column = @Column(name = "bbox_x")),
        @AttributeOverride(name = "y", column = @Column(name = "bbox_y")),
        @AttributeOverride(name = "width", column = @Column(name = "bbox_width")),
        @AttributeOverride(name = "height", column = @Column(name = "bbox_height")),
        @AttributeOverride(name = "elementType", column = @Column(name = "bbox_element_type")),
        @AttributeOverride(name = "confidence", column = @Column(name = "bbox_confidence"))
    })
    private BoundingBox bbox;

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public ExtractionResult getExtractionResult() { return extractionResult; }
    public void setExtractionResult(ExtractionResult extractionResult) { this.extractionResult = extractionResult; }
    public String getTableId() { return tableId; }
    public void setTableId(String tableId) { this.tableId = tableId; }
    public List<String> getHeaders() { return headers; }
    public void setHeaders(List<String> headers) { this.headers = headers; }
    public List<TableRow> getRows() { return rows; }
    public void setRows(List<TableRow> rows) { this.rows = rows; }
    public Float getConfidence() { return confidence; }
    public void setConfidence(Float confidence) { this.confidence = confidence; }
    public BoundingBox getBbox() { return bbox; }
    public void setBbox(BoundingBox bbox) { this.bbox = bbox; }
}