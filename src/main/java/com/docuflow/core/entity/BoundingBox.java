package com.docuflow.core.entity;

import jakarta.persistence.Embeddable;

@Embeddable
public class BoundingBox {

    private Double x;
    private Double y;
    private Double width;
    private Double height;
    private String elementType;
    private Float confidence;

    // Getters and setters
    public Double getX() { return x; }
    public void setX(Double x) { this.x = x; }
    public Double getY() { return y; }
    public void setY(Double y) { this.y = y; }
    public Double getWidth() { return width; }
    public void setWidth(Double width) { this.width = width; }
    public Double getHeight() { return height; }
    public void setHeight(Double height) { this.height = height; }
    public String getElementType() { return elementType; }
    public void setElementType(String elementType) { this.elementType = elementType; }
    public Float getConfidence() { return confidence; }
    public void setConfidence(Float confidence) { this.confidence = confidence; }
}