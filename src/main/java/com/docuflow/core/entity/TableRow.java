package com.docuflow.core.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "table_rows", indexes = {
    @Index(name = "idx_table_rows_table_extraction_id", columnList = "table_extraction_id")
})
public class TableRow {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "table_extraction_id", nullable = false)
    private TableExtraction tableExtraction;

    @Column(name = "row_order", nullable = false)
    private Integer rowOrder;

    @ElementCollection
    @CollectionTable(name = "table_row_cells", joinColumns = @JoinColumn(name = "table_row_id"))
    @Column(name = "cell")
    @OrderColumn(name = "cell_order")
    private java.util.List<String> cells = new java.util.ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "table_row_cell_confidences", joinColumns = @JoinColumn(name = "table_row_id"))
    @Column(name = "confidence")
    @OrderColumn(name = "confidence_order")
    private java.util.List<Float> cellConfidences = new java.util.ArrayList<>();

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public TableExtraction getTableExtraction() { return tableExtraction; }
    public void setTableExtraction(TableExtraction tableExtraction) { this.tableExtraction = tableExtraction; }
    public Integer getRowOrder() { return rowOrder; }
    public void setRowOrder(Integer rowOrder) { this.rowOrder = rowOrder; }
    public java.util.List<String> getCells() { return cells; }
    public void setCells(java.util.List<String> cells) { this.cells = cells; }
    public java.util.List<Float> getCellConfidences() { return cellConfidences; }
    public void setCellConfidences(java.util.List<Float> cellConfidences) { this.cellConfidences = cellConfidences; }
}