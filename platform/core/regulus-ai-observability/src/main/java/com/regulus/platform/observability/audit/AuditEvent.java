package com.regulus.platform.observability.audit;

import java.time.Instant;
import java.util.Map;

/**
 * Structured audit event for compliance logging.
 * Captures who/what/when for regulatory requirements.
 */
public record AuditEvent(
    String eventId,
    EventType type,
    Instant timestamp,
    String correlationId,
    String userId,
    String agentId,
    String operation,
    String resource,
    Outcome outcome,
    String message,
    Map<String, Object> details,
    Map<String, String> metadata
) {

    public enum EventType {
        LLM_CALL,
        AGENT_INVOCATION,
        TOOL_EXECUTION,
        POLICY_EVALUATION,
        POLICY_VIOLATION,
        PRIVACY_REDACTION,
        KILL_SWITCH_ACTIVATED,
        KILL_SWITCH_DEACTIVATED,
        MCP_CALL,
        A2A_CALL,
        DATA_ACCESS,
        CONSENT_CHECK,
        ERROR
    }

    public enum Outcome {
        SUCCESS,
        FAILURE,
        BLOCKED,
        PARTIAL
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String eventId;
        private EventType type;
        private Instant timestamp = Instant.now();
        private String correlationId;
        private String userId;
        private String agentId;
        private String operation;
        private String resource;
        private Outcome outcome = Outcome.SUCCESS;
        private String message;
        private Map<String, Object> details = Map.of();
        private Map<String, String> metadata = Map.of();

        public Builder eventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public Builder type(EventType type) {
            this.type = type;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder agentId(String agentId) {
            this.agentId = agentId;
            return this;
        }

        public Builder operation(String operation) {
            this.operation = operation;
            return this;
        }

        public Builder resource(String resource) {
            this.resource = resource;
            return this;
        }

        public Builder outcome(Outcome outcome) {
            this.outcome = outcome;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder details(Map<String, Object> details) {
            this.details = details;
            return this;
        }

        public Builder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        public AuditEvent build() {
            if (eventId == null) {
                eventId = java.util.UUID.randomUUID().toString();
            }
            return new AuditEvent(
                eventId, type, timestamp, correlationId, userId, agentId,
                operation, resource, outcome, message, details, metadata
            );
        }
    }
}
