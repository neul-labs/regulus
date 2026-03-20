package com.regulus.platform.policy.model;

import java.time.Instant;

/**
 * Represents a policy violation detected during request processing.
 */
public record PolicyViolation(
    String policyName,
    String violationType,
    String message,
    Severity severity,
    Instant timestamp,
    String correlationId
) {

    public enum Severity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String policyName;
        private String violationType;
        private String message;
        private Severity severity = Severity.HIGH;
        private Instant timestamp = Instant.now();
        private String correlationId;

        public Builder policyName(String policyName) {
            this.policyName = policyName;
            return this;
        }

        public Builder violationType(String violationType) {
            this.violationType = violationType;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder severity(Severity severity) {
            this.severity = severity;
            return this;
        }

        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public PolicyViolation build() {
            return new PolicyViolation(
                policyName,
                violationType,
                message,
                severity,
                timestamp,
                correlationId
            );
        }
    }
}
