package com.neullabs.regulus.killswitch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration properties for kill switch functionality.
 */
@ConfigurationProperties(prefix = "regulus.ai.kill-switch")
public class KillSwitchProperties {

    /**
     * Whether kill switch functionality is enabled.
     */
    private boolean enabled = true;

    /**
     * How often to refresh state from external provider.
     */
    private Duration refreshInterval = Duration.ofSeconds(30);

    /**
     * External state provider configuration.
     */
    private ProviderConfig provider = new ProviderConfig();

    /**
     * Alerting configuration.
     */
    private AlertConfig alerts = new AlertConfig();

    /**
     * Dual-control configuration for activation.
     */
    private DualControlConfig dualControl = new DualControlConfig();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getRefreshInterval() {
        return refreshInterval;
    }

    public void setRefreshInterval(Duration refreshInterval) {
        this.refreshInterval = refreshInterval;
    }

    public ProviderConfig getProvider() {
        return provider;
    }

    public void setProvider(ProviderConfig provider) {
        this.provider = provider;
    }

    public AlertConfig getAlerts() {
        return alerts;
    }

    public void setAlerts(AlertConfig alerts) {
        this.alerts = alerts;
    }

    public DualControlConfig getDualControl() {
        return dualControl;
    }

    public void setDualControl(DualControlConfig dualControl) {
        this.dualControl = dualControl;
    }

    public static class ProviderConfig {
        /**
         * Type of state provider: memory, vault, configHub
         */
        private String type = "memory";

        /**
         * Vault path for kill switch state (if using Vault).
         */
        private String vaultPath = "secret/regulus/kill-switch";

        /**
         * ConfigHub URL (if using ConfigHub).
         */
        private String configHubUrl;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getVaultPath() {
            return vaultPath;
        }

        public void setVaultPath(String vaultPath) {
            this.vaultPath = vaultPath;
        }

        public String getConfigHubUrl() {
            return configHubUrl;
        }

        public void setConfigHubUrl(String configHubUrl) {
            this.configHubUrl = configHubUrl;
        }
    }

    public static class AlertConfig {
        /**
         * Whether to send alerts on kill switch activation.
         */
        private boolean enabled = true;

        /**
         * Alert channels: slack, pagerduty, email
         */
        private String[] channels = {"slack"};

        /**
         * Alert severity level.
         */
        private String severity = "critical";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String[] getChannels() {
            return channels;
        }

        public void setChannels(String[] channels) {
            this.channels = channels;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }
    }

    public static class DualControlConfig {
        /**
         * Whether dual-control is required for activation.
         */
        private boolean required = false;

        /**
         * Minimum approvers needed for dual-control.
         */
        private int minApprovers = 2;

        /**
         * Approval timeout duration.
         */
        private Duration approvalTimeout = Duration.ofMinutes(15);

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }

        public int getMinApprovers() {
            return minApprovers;
        }

        public void setMinApprovers(int minApprovers) {
            this.minApprovers = minApprovers;
        }

        public Duration getApprovalTimeout() {
            return approvalTimeout;
        }

        public void setApprovalTimeout(Duration approvalTimeout) {
            this.approvalTimeout = approvalTimeout;
        }
    }
}
