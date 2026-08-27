package com.docuflow.core.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_steps", indexes = {
    @Index(name = "idx_job_steps_job_id", columnList = "job_id"),
    @Index(name = "idx_job_steps_status", columnList = "status")
})
public class JobStep {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private ProcessingJob job;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StepStatus status = StepStatus.PENDING;

    @Column(name = "input", columnDefinition = "jsonb")
    private String input;  // JSON

    @Column(name = "output", columnDefinition = "jsonb")
    private String output;  // JSON

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @CreationTimestamp
    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public ProcessingJob getJob() { return job; }
    public void setJob(ProcessingJob job) { this.job = job; }
    public Integer getStepOrder() { return stepOrder; }
    public void setStepOrder(Integer stepOrder) { this.stepOrder = stepOrder; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public StepStatus getStatus() { return status; }
    public void setStatus(StepStatus status) { this.status = status; }
    public String getInput() { return input; }
    public void setInput(String input) { this.input = input; }
    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}