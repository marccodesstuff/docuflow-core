package com.docuflow.core.entity;

import jakarta.persistence.*;
import java.util.HashMap;
import java.util.Map;

@Embeddable
public class RoutingRules {

    @Column(name = "default_assignee_group", length = 100)
    private String defaultAssigneeGroup;

    @ElementCollection
    @CollectionTable(name = "routing_field_based_rules", joinColumns = @JoinColumn(name = "document_type_id"))
    @MapKeyColumn(name = "field_value")
    @Column(name = "assignee_group")
    private Map<String, String> fieldBasedRouting = new HashMap<>();

    @Column(name = "sla_hours")
    private Integer slaHours = 24;

    @Column(name = "auto_approve_threshold_enabled")
    private Boolean autoApproveThresholdEnabled = false;

    @Column(name = "auto_approve_confidence")
    private Float autoApproveConfidence = 0.95f;

    // Getters and setters
    public String getDefaultAssigneeGroup() { return defaultAssigneeGroup; }
    public void setDefaultAssigneeGroup(String defaultAssigneeGroup) { this.defaultAssigneeGroup = defaultAssigneeGroup; }
    public Map<String, String> getFieldBasedRouting() { return fieldBasedRouting; }
    public void setFieldBasedRouting(Map<String, String> fieldBasedRouting) { this.fieldBasedRouting = fieldBasedRouting; }
    public Integer getSlaHours() { return slaHours; }
    public void setSlaHours(Integer slaHours) { this.slaHours = slaHours; }
    public Boolean getAutoApproveThresholdEnabled() { return autoApproveThresholdEnabled; }
    public void setAutoApproveThresholdEnabled(Boolean autoApproveThresholdEnabled) { this.autoApproveThresholdEnabled = autoApproveThresholdEnabled; }
    public Float getAutoApproveConfidence() { return autoApproveConfidence; }
    public void setAutoApproveConfidence(Float autoApproveConfidence) { this.autoApproveConfidence = autoApproveConfidence; }
}