package com.docuflow.core.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "webhook_registrations", indexes = {
    @Index(name = "idx_webhook_registrations_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_webhook_registrations_active", columnList = "active")
})
public class WebhookRegistration {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @ElementCollection
    @CollectionTable(name = "webhook_events", joinColumns = @JoinColumn(name = "webhook_registration_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", length = 50)
    private java.util.List<WebhookEventType> events = new java.util.ArrayList<>();

    @Column(name = "secret", length = 200)
    private String secret;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "consecutive_failures")
    private Integer consecutiveFailures = 0;

    @Column(name = "last_success_at")
    private Instant lastSuccessAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public java.util.List<WebhookEventType> getEvents() { return events; }
    public void setEvents(java.util.List<WebhookEventType> events) { this.events = events; }
    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Integer getConsecutiveFailures() { return consecutiveFailures; }
    public void setConsecutiveFailures(Integer consecutiveFailures) { this.consecutiveFailures = consecutiveFailures; }
    public Instant getLastSuccessAt() { return lastSuccessAt; }
    public void setLastSuccessAt(Instant lastSuccessAt) { this.lastSuccessAt = lastSuccessAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}