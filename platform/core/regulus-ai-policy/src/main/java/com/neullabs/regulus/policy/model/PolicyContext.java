package com.neullabs.regulus.policy.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Context object carrying policy-relevant metadata through the request lifecycle.
 * Captures LEI, purpose codes, consent status, and other governance attributes.
 */
public class PolicyContext {

    private String legalEntityIdentifier;
    private String purposeCode;
    private String lawfulBasis;
    private boolean consentGranted;
    private String userId;
    private String correlationId;
    private Instant timestamp;
    private Map<String, Object> attributes;

    public PolicyContext() {
        this.timestamp = Instant.now();
        this.attributes = new HashMap<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Optional<String> getLegalEntityIdentifier() {
        return Optional.ofNullable(legalEntityIdentifier);
    }

    public void setLegalEntityIdentifier(String legalEntityIdentifier) {
        this.legalEntityIdentifier = legalEntityIdentifier;
    }

    public Optional<String> getPurposeCode() {
        return Optional.ofNullable(purposeCode);
    }

    public void setPurposeCode(String purposeCode) {
        this.purposeCode = purposeCode;
    }

    public Optional<String> getLawfulBasis() {
        return Optional.ofNullable(lawfulBasis);
    }

    public void setLawfulBasis(String lawfulBasis) {
        this.lawfulBasis = lawfulBasis;
    }

    public boolean isConsentGranted() {
        return consentGranted;
    }

    public void setConsentGranted(boolean consentGranted) {
        this.consentGranted = consentGranted;
    }

    public Optional<String> getUserId() {
        return Optional.ofNullable(userId);
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Optional<String> getCorrelationId() {
        return Optional.ofNullable(correlationId);
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttribute(String key, Object value) {
        this.attributes.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getAttribute(String key, Class<T> type) {
        Object value = attributes.get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of((T) value);
        }
        return Optional.empty();
    }

    public static class Builder {
        private final PolicyContext context = new PolicyContext();

        public Builder legalEntityIdentifier(String lei) {
            context.setLegalEntityIdentifier(lei);
            return this;
        }

        public Builder purposeCode(String purposeCode) {
            context.setPurposeCode(purposeCode);
            return this;
        }

        public Builder lawfulBasis(String lawfulBasis) {
            context.setLawfulBasis(lawfulBasis);
            return this;
        }

        public Builder consentGranted(boolean consentGranted) {
            context.setConsentGranted(consentGranted);
            return this;
        }

        public Builder userId(String userId) {
            context.setUserId(userId);
            return this;
        }

        public Builder correlationId(String correlationId) {
            context.setCorrelationId(correlationId);
            return this;
        }

        public Builder attribute(String key, Object value) {
            context.setAttribute(key, value);
            return this;
        }

        public PolicyContext build() {
            return context;
        }
    }
}
