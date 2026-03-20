package com.regulus.platform.persistence.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Regulus persistence.
 */
@ConfigurationProperties(prefix = "regulus.ai.persistence")
public class PersistenceProperties {

    private boolean enabled = true;
    private AuditProperties audit = new AuditProperties();
    private KillSwitchProperties killSwitch = new KillSwitchProperties();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public AuditProperties getAudit() {
        return audit;
    }

    public void setAudit(AuditProperties audit) {
        this.audit = audit;
    }

    public KillSwitchProperties getKillSwitch() {
        return killSwitch;
    }

    public void setKillSwitch(KillSwitchProperties killSwitch) {
        this.killSwitch = killSwitch;
    }

    public static class AuditProperties {
        private boolean enabled = true;
        private int retentionDays = 365;
        private int batchSize = 100;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getRetentionDays() {
            return retentionDays;
        }

        public void setRetentionDays(int retentionDays) {
            this.retentionDays = retentionDays;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }
    }

    public static class KillSwitchProperties {
        private boolean enabled = true;
        private boolean requireDualApproval = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isRequireDualApproval() {
            return requireDualApproval;
        }

        public void setRequireDualApproval(boolean requireDualApproval) {
            this.requireDualApproval = requireDualApproval;
        }
    }
}
