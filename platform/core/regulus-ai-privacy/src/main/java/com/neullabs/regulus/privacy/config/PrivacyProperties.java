package com.neullabs.regulus.privacy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for privacy filtering.
 */
@ConfigurationProperties(prefix = "regulus.ai.privacy")
public class PrivacyProperties {

    /**
     * Whether privacy filtering is enabled.
     */
    private boolean enabled = true;

    /**
     * JSONPath expressions for fields to redact.
     */
    private List<RedactionPath> redact = new ArrayList<>();

    /**
     * Whether to enable PII pattern detection.
     */
    private boolean piiDetectionEnabled = true;

    /**
     * Custom PII patterns to add to default set.
     */
    private List<PiiPatternConfig> customPatterns = new ArrayList<>();

    /**
     * Default replacement strategy.
     */
    private String defaultReplacement = "MASK";

    /**
     * Whether to log redaction details for audit.
     */
    private boolean auditLogging = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<RedactionPath> getRedact() {
        return redact;
    }

    public void setRedact(List<RedactionPath> redact) {
        this.redact = redact;
    }

    public boolean isPiiDetectionEnabled() {
        return piiDetectionEnabled;
    }

    public void setPiiDetectionEnabled(boolean piiDetectionEnabled) {
        this.piiDetectionEnabled = piiDetectionEnabled;
    }

    public List<PiiPatternConfig> getCustomPatterns() {
        return customPatterns;
    }

    public void setCustomPatterns(List<PiiPatternConfig> customPatterns) {
        this.customPatterns = customPatterns;
    }

    public String getDefaultReplacement() {
        return defaultReplacement;
    }

    public void setDefaultReplacement(String defaultReplacement) {
        this.defaultReplacement = defaultReplacement;
    }

    public boolean isAuditLogging() {
        return auditLogging;
    }

    public void setAuditLogging(boolean auditLogging) {
        this.auditLogging = auditLogging;
    }

    public static class RedactionPath {
        private String path;
        private String fieldType = "SENSITIVE";
        private String replacement = "MASK";

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getFieldType() {
            return fieldType;
        }

        public void setFieldType(String fieldType) {
            this.fieldType = fieldType;
        }

        public String getReplacement() {
            return replacement;
        }

        public void setReplacement(String replacement) {
            this.replacement = replacement;
        }
    }

    public static class PiiPatternConfig {
        private String name;
        private String fieldType;
        private String pattern;
        private String replacement;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getFieldType() {
            return fieldType;
        }

        public void setFieldType(String fieldType) {
            this.fieldType = fieldType;
        }

        public String getPattern() {
            return pattern;
        }

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }

        public String getReplacement() {
            return replacement;
        }

        public void setReplacement(String replacement) {
            this.replacement = replacement;
        }
    }
}
