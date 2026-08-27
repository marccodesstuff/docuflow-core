package com.docuflow.core.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "document_pages", indexes = {
    @Index(name = "idx_document_pages_document_id", columnList = "document_id"),
    @Index(name = "idx_document_pages_page_number", columnList = "page_number")
})
public class DocumentPage {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "page_number", nullable = false)
    private Integer pageNumber;

    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    @Column(name = "width_px")
    private Integer widthPx;

    @Column(name = "height_px")
    private Integer heightPx;

    @Column(name = "elements", columnDefinition = "jsonb")
    private String elements;  // JSON array of BoundingBox

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Document getDocument() { return document; }
    public void setDocument(Document document) { this.document = document; }
    public Integer getPageNumber() { return pageNumber; }
    public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }
    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
    public Integer getWidthPx() { return widthPx; }
    public void setWidthPx(Integer widthPx) { this.widthPx = widthPx; }
    public Integer getHeightPx() { return heightPx; }
    public void setHeightPx(Integer heightPx) { this.heightPx = heightPx; }
    public String getElements() { return elements; }
    public void setElements(String elements) { this.elements = elements; }
}