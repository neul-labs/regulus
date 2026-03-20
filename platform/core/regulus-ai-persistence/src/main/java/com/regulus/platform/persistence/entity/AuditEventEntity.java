package com.regulus.platform.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Persistent audit event for compliance and regulatory reporting.
 * Immutable after creation - audit logs should never be modified.
 */
@Entity
@Table(name = "audit_events", indexes = {
    @Index(name = "idx_audit_correlation_id", columnList = "correlationId"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_event_type", columnList = "eventType"),
    @Index(name = "idx_audit_user_id", columnList = "userId"),
    @Index(name = "idx_audit_agent_id", columnList = "agentId")
})
public class AuditEventEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 36)
    private String correlationId;

    @Column(nullable = false, length = 50)
    private String eventType;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(length = 100)
    private String userId;

    @Column(length = 100)
    private String agentId;

    @Column(length = 100)
    private String modelId;

    @Column(length = 100)
    private String toolName;

    @Column(columnDefinition = "TEXT")
    private String inputSummary;

    @Column(columnDefinition = "TEXT")
    private String outputSummary;

    @Column(length = 20)
    private String outcome;

    @Column
    private Long durationMs;

    @Column
    private Integer tokenCount;

    @Column
    private Double estimatedCost;

    @Column(length = 100)
    private String policyDecision;

    @Column(columnDefinition = "TEXT")
    private String policyViolations;

    @Column(length = 100)
    private String lei;

    @Column(length = 50)
    private String purposeCode;

    @Column
    private Boolean consentGranted;

    @Column
    private Boolean killSwitchActive;

    @Column(columnDefinition = "TEXT")
    private String metadataJson;

    @Column(length = 50)
    private String sourceIp;

    @Column(length = 200)
    private String userAgent;

    @Column(nullable = false)
    private Instant createdAt;

    // No setters - entity is immutable after creation
    protected AuditEventEntity() {
        // JPA requires default constructor
    }

    private AuditEventEntity(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID().toString();
        this.correlationId = builder.correlationId;
        this.eventType = builder.eventType;
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
        this.userId = builder.userId;
        this.agentId = builder.agentId;
        this.modelId = builder.modelId;
        this.toolName = builder.toolName;
        this.inputSummary = builder.inputSummary;
        this.outputSummary = builder.outputSummary;
        this.outcome = builder.outcome;
        this.durationMs = builder.durationMs;
        this.tokenCount = builder.tokenCount;
        this.estimatedCost = builder.estimatedCost;
        this.policyDecision = builder.policyDecision;
        this.policyViolations = builder.policyViolations;
        this.lei = builder.lei;
        this.purposeCode = builder.purposeCode;
        this.consentGranted = builder.consentGranted;
        this.killSwitchActive = builder.killSwitchActive;
        this.metadataJson = builder.metadataJson;
        this.sourceIp = builder.sourceIp;
        this.userAgent = builder.userAgent;
        this.createdAt = Instant.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters only
    public String getId() { return id; }
    public String getCorrelationId() { return correlationId; }
    public String getEventType() { return eventType; }
    public Instant getTimestamp() { return timestamp; }
    public String getUserId() { return userId; }
    public String getAgentId() { return agentId; }
    public String getModelId() { return modelId; }
    public String getToolName() { return toolName; }
    public String getInputSummary() { return inputSummary; }
    public String getOutputSummary() { return outputSummary; }
    public String getOutcome() { return outcome; }
    public Long getDurationMs() { return durationMs; }
    public Integer getTokenCount() { return tokenCount; }
    public Double getEstimatedCost() { return estimatedCost; }
    public String getPolicyDecision() { return policyDecision; }
    public String getPolicyViolations() { return policyViolations; }
    public String getLei() { return lei; }
    public String getPurposeCode() { return purposeCode; }
    public Boolean getConsentGranted() { return consentGranted; }
    public Boolean getKillSwitchActive() { return killSwitchActive; }
    public String getMetadataJson() { return metadataJson; }
    public String getSourceIp() { return sourceIp; }
    public String getUserAgent() { return userAgent; }
    public Instant getCreatedAt() { return createdAt; }

    public static class Builder {
        private String id;
        private String correlationId;
        private String eventType;
        private Instant timestamp;
        private String userId;
        private String agentId;
        private String modelId;
        private String toolName;
        private String inputSummary;
        private String outputSummary;
        private String outcome;
        private Long durationMs;
        private Integer tokenCount;
        private Double estimatedCost;
        private String policyDecision;
        private String policyViolations;
        private String lei;
        private String purposeCode;
        private Boolean consentGranted;
        private Boolean killSwitchActive;
        private String metadataJson;
        private String sourceIp;
        private String userAgent;

        public Builder id(String id) { this.id = id; return this; }
        public Builder correlationId(String correlationId) { this.correlationId = correlationId; return this; }
        public Builder eventType(String eventType) { this.eventType = eventType; return this; }
        public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public Builder userId(String userId) { this.userId = userId; return this; }
        public Builder agentId(String agentId) { this.agentId = agentId; return this; }
        public Builder modelId(String modelId) { this.modelId = modelId; return this; }
        public Builder toolName(String toolName) { this.toolName = toolName; return this; }
        public Builder inputSummary(String inputSummary) { this.inputSummary = inputSummary; return this; }
        public Builder outputSummary(String outputSummary) { this.outputSummary = outputSummary; return this; }
        public Builder outcome(String outcome) { this.outcome = outcome; return this; }
        public Builder durationMs(Long durationMs) { this.durationMs = durationMs; return this; }
        public Builder tokenCount(Integer tokenCount) { this.tokenCount = tokenCount; return this; }
        public Builder estimatedCost(Double estimatedCost) { this.estimatedCost = estimatedCost; return this; }
        public Builder policyDecision(String policyDecision) { this.policyDecision = policyDecision; return this; }
        public Builder policyViolations(String policyViolations) { this.policyViolations = policyViolations; return this; }
        public Builder lei(String lei) { this.lei = lei; return this; }
        public Builder purposeCode(String purposeCode) { this.purposeCode = purposeCode; return this; }
        public Builder consentGranted(Boolean consentGranted) { this.consentGranted = consentGranted; return this; }
        public Builder killSwitchActive(Boolean killSwitchActive) { this.killSwitchActive = killSwitchActive; return this; }
        public Builder metadataJson(String metadataJson) { this.metadataJson = metadataJson; return this; }
        public Builder sourceIp(String sourceIp) { this.sourceIp = sourceIp; return this; }
        public Builder userAgent(String userAgent) { this.userAgent = userAgent; return this; }

        public AuditEventEntity build() {
            return new AuditEventEntity(this);
        }
    }
}
