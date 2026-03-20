package com.regulus.platform.privacy.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Metadata attached to data for privacy tracking and GDPR compliance.
 */
public class PrivacyMetadata {

    private String purposeCode;
    private String lawfulBasis;
    private String dataSubjectId;
    private Instant collectionTimestamp;
    private Instant retentionExpiry;
    private String dataCategory;
    private boolean specialCategory;
    private String sourceSystem;
    private Map<String, Object> additionalAttributes;

    public PrivacyMetadata() {
        this.collectionTimestamp = Instant.now();
        this.additionalAttributes = new HashMap<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters and setters

    public String getPurposeCode() {
        return purposeCode;
    }

    public void setPurposeCode(String purposeCode) {
        this.purposeCode = purposeCode;
    }

    public String getLawfulBasis() {
        return lawfulBasis;
    }

    public void setLawfulBasis(String lawfulBasis) {
        this.lawfulBasis = lawfulBasis;
    }

    public String getDataSubjectId() {
        return dataSubjectId;
    }

    public void setDataSubjectId(String dataSubjectId) {
        this.dataSubjectId = dataSubjectId;
    }

    public Instant getCollectionTimestamp() {
        return collectionTimestamp;
    }

    public void setCollectionTimestamp(Instant collectionTimestamp) {
        this.collectionTimestamp = collectionTimestamp;
    }

    public Instant getRetentionExpiry() {
        return retentionExpiry;
    }

    public void setRetentionExpiry(Instant retentionExpiry) {
        this.retentionExpiry = retentionExpiry;
    }

    public String getDataCategory() {
        return dataCategory;
    }

    public void setDataCategory(String dataCategory) {
        this.dataCategory = dataCategory;
    }

    public boolean isSpecialCategory() {
        return specialCategory;
    }

    public void setSpecialCategory(boolean specialCategory) {
        this.specialCategory = specialCategory;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public Map<String, Object> getAdditionalAttributes() {
        return additionalAttributes;
    }

    public void setAttribute(String key, Object value) {
        this.additionalAttributes.put(key, value);
    }

    public static class Builder {
        private final PrivacyMetadata metadata = new PrivacyMetadata();

        public Builder purposeCode(String purposeCode) {
            metadata.setPurposeCode(purposeCode);
            return this;
        }

        public Builder lawfulBasis(String lawfulBasis) {
            metadata.setLawfulBasis(lawfulBasis);
            return this;
        }

        public Builder dataSubjectId(String dataSubjectId) {
            metadata.setDataSubjectId(dataSubjectId);
            return this;
        }

        public Builder retentionExpiry(Instant retentionExpiry) {
            metadata.setRetentionExpiry(retentionExpiry);
            return this;
        }

        public Builder dataCategory(String dataCategory) {
            metadata.setDataCategory(dataCategory);
            return this;
        }

        public Builder specialCategory(boolean specialCategory) {
            metadata.setSpecialCategory(specialCategory);
            return this;
        }

        public Builder sourceSystem(String sourceSystem) {
            metadata.setSourceSystem(sourceSystem);
            return this;
        }

        public Builder attribute(String key, Object value) {
            metadata.setAttribute(key, value);
            return this;
        }

        public PrivacyMetadata build() {
            return metadata;
        }
    }
}
