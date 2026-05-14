package com.neullabs.regulus.policy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for policy enforcement.
 */
@ConfigurationProperties(prefix = "regulus.ai.policies")
public class PolicyProperties {

    /**
     * Whether policy enforcement is enabled.
     */
    private boolean enabled = true;

    /**
     * List of policy names to enforce by default.
     * Example: ["require.LEI", "require.PurposeCode"]
     */
    private List<String> enforced = new ArrayList<>();

    /**
     * Whether to fail fast on first violation or collect all violations.
     */
    private boolean failFast = false;

    /**
     * Whether to log policy violations to audit log.
     */
    private boolean auditEnabled = true;

    /**
     * LEI-specific configuration.
     */
    private LeiConfig lei = new LeiConfig();

    /**
     * Purpose code configuration.
     */
    private PurposeCodeConfig purposeCode = new PurposeCodeConfig();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getEnforced() {
        return enforced;
    }

    public void setEnforced(List<String> enforced) {
        this.enforced = enforced;
    }

    public boolean isFailFast() {
        return failFast;
    }

    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }

    public boolean isAuditEnabled() {
        return auditEnabled;
    }

    public void setAuditEnabled(boolean auditEnabled) {
        this.auditEnabled = auditEnabled;
    }

    public LeiConfig getLei() {
        return lei;
    }

    public void setLei(LeiConfig lei) {
        this.lei = lei;
    }

    public PurposeCodeConfig getPurposeCode() {
        return purposeCode;
    }

    public void setPurposeCode(PurposeCodeConfig purposeCode) {
        this.purposeCode = purposeCode;
    }

    public static class LeiConfig {
        /**
         * Whether to validate LEI format (ISO 17442).
         */
        private boolean validateFormat = true;

        /**
         * Whether to validate LEI against external registry.
         */
        private boolean validateRegistry = false;

        /**
         * External LEI registry URL for validation.
         */
        private String registryUrl;

        public boolean isValidateFormat() {
            return validateFormat;
        }

        public void setValidateFormat(boolean validateFormat) {
            this.validateFormat = validateFormat;
        }

        public boolean isValidateRegistry() {
            return validateRegistry;
        }

        public void setValidateRegistry(boolean validateRegistry) {
            this.validateRegistry = validateRegistry;
        }

        public String getRegistryUrl() {
            return registryUrl;
        }

        public void setRegistryUrl(String registryUrl) {
            this.registryUrl = registryUrl;
        }
    }

    public static class PurposeCodeConfig {
        /**
         * Additional valid purpose codes beyond standard set.
         */
        private List<String> additionalCodes = new ArrayList<>();

        /**
         * Whether to validate lawful basis alignment.
         */
        private boolean validateLawfulBasis = true;

        public List<String> getAdditionalCodes() {
            return additionalCodes;
        }

        public void setAdditionalCodes(List<String> additionalCodes) {
            this.additionalCodes = additionalCodes;
        }

        public boolean isValidateLawfulBasis() {
            return validateLawfulBasis;
        }

        public void setValidateLawfulBasis(boolean validateLawfulBasis) {
            this.validateLawfulBasis = validateLawfulBasis;
        }
    }
}
