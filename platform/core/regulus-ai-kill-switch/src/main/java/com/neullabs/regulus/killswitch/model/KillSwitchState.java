package com.neullabs.regulus.killswitch.model;

import java.time.Instant;

/**
 * Represents the current state of the kill switch.
 */
public record KillSwitchState(
    boolean activated,
    String reason,
    String activatedBy,
    Instant activatedAt,
    Scope scope,
    String correlationId
) {

    public enum Scope {
        GLOBAL,         // All AI operations disabled
        AGENT,          // Specific agent disabled
        MODEL,          // Specific model disabled
        TOOL,           // Specific tool disabled
        ENDPOINT        // Specific endpoint disabled
    }

    public static KillSwitchState inactive() {
        return new KillSwitchState(false, null, null, null, null, null);
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isActive() {
        return activated;
    }

    public static class Builder {
        private boolean activated;
        private String reason;
        private String activatedBy;
        private Instant activatedAt;
        private Scope scope = Scope.GLOBAL;
        private String correlationId;

        public Builder activated(boolean activated) {
            this.activated = activated;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder activatedBy(String activatedBy) {
            this.activatedBy = activatedBy;
            return this;
        }

        public Builder activatedAt(Instant activatedAt) {
            this.activatedAt = activatedAt;
            return this;
        }

        public Builder scope(Scope scope) {
            this.scope = scope;
            return this;
        }

        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public KillSwitchState build() {
            return new KillSwitchState(
                activated,
                reason,
                activatedBy,
                activatedAt != null ? activatedAt : Instant.now(),
                scope,
                correlationId
            );
        }
    }
}
