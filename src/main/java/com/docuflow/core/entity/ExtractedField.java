package com.docuflow.core.entity;

import jakarta.persistence.*;

@Embeddable
public class ExtractedField {

    @Column(name = "value", length = 5000)
    private String value;

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

    @Column(name = "validated")
    private Boolean validated = false;

    @Column(name = "validation_error", length = 500)
    private String validationError;

    @ElementCollection
    @CollectionTable(name = "extracted_field_alternatives", joinColumns = @JoinColumn(name = "extracted_field_id"))
    @Column(name = "alternative")
    private java.util.List<String> alternatives = new java.util.ArrayList<>();

    // Getters and setters
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public Float getConfidence() { return confidence; }
    public void setConfidence(Float confidence) { this.confidence = confidence; }
    public BoundingBox getBbox() { return bbox; }
    public void setBbox(BoundingBox bbox) { this.bbox = bbox; }
    public Boolean getValidated() { return validated; }
    public void setValidated(Boolean validated) { this.validated = validated; }
    public String getValidationError() { return validationError; }
    public void setValidationError(String validationError) { this.validationError = validationError; }
    public java.util.List<String> getAlternatives() { return alternatives; }
    public void setAlternatives(java.util.List<String> alternatives) { this.alternatives = alternatives; }
}