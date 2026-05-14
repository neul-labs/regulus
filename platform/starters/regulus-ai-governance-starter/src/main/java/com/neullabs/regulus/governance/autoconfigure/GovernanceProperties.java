package com.neullabs.regulus.governance.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

/**
 * Configuration properties for governance features.
 */
@ConfigurationProperties(prefix = "regulus.ai.governance")
public class GovernanceProperties {

    private boolean enabled = true;
    private boolean enforceMode = true;
    private Guards guards = new Guards();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnforceMode() {
        return enforceMode;
    }

    public void setEnforceMode(boolean enforceMode) {
        this.enforceMode = enforceMode;
    }

    public Guards getGuards() {
        return guards;
    }

    public void setGuards(Guards guards) {
        this.guards = guards;
    }

    public static class Guards {
        private LeiGuard lei = new LeiGuard();
        private PurposeCodeGuard purposeCode = new PurposeCodeGuard();
        private ConsentGuard consent = new ConsentGuard();

        public LeiGuard getLei() {
            return lei;
        }

        public void setLei(LeiGuard lei) {
            this.lei = lei;
        }

        public PurposeCodeGuard getPurposeCode() {
            return purposeCode;
        }

        public void setPurposeCode(PurposeCodeGuard purposeCode) {
            this.purposeCode = purposeCode;
        }

        public ConsentGuard getConsent() {
            return consent;
        }

        public void setConsent(ConsentGuard consent) {
            this.consent = consent;
        }
    }

    public static class LeiGuard {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class PurposeCodeGuard {
        private boolean enabled = true;
        private Set<String> allowedCodes = Set.of(
            "ACCOUNT_MANAGEMENT",
            "SERVICE_DELIVERY",
            "FRAUD_PREVENTION",
            "REGULATORY_COMPLIANCE"
        );

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Set<String> getAllowedCodes() {
            return allowedCodes;
        }

        public void setAllowedCodes(Set<String> allowedCodes) {
            this.allowedCodes = allowedCodes;
        }
    }

    public static class ConsentGuard {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
