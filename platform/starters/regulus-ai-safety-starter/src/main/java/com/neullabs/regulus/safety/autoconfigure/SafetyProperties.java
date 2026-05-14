package com.neullabs.regulus.safety.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration properties for safety features.
 */
@ConfigurationProperties(prefix = "regulus.ai.safety")
public class SafetyProperties {

    private boolean enabled = true;
    private KillSwitchProperties killSwitch = new KillSwitchProperties();
    private PrivacyProperties privacy = new PrivacyProperties();
    private PromptInjectionProperties promptInjection = new PromptInjectionProperties();
    private DataResidencyProperties dataResidency = new DataResidencyProperties();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public KillSwitchProperties getKillSwitch() {
        return killSwitch;
    }

    public void setKillSwitch(KillSwitchProperties killSwitch) {
        this.killSwitch = killSwitch;
    }

    public PrivacyProperties getPrivacy() {
        return privacy;
    }

    public void setPrivacy(PrivacyProperties privacy) {
        this.privacy = privacy;
    }

    public PromptInjectionProperties getPromptInjection() {
        return promptInjection;
    }

    public void setPromptInjection(PromptInjectionProperties promptInjection) {
        this.promptInjection = promptInjection;
    }

    public DataResidencyProperties getDataResidency() {
        return dataResidency;
    }

    public void setDataResidency(DataResidencyProperties dataResidency) {
        this.dataResidency = dataResidency;
    }

    public static class KillSwitchProperties {
        private boolean enabled = true;
        private String provider = "in-memory";
        private DualControlProperties dualControl = new DualControlProperties();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public DualControlProperties getDualControl() {
            return dualControl;
        }

        public void setDualControl(DualControlProperties dualControl) {
            this.dualControl = dualControl;
        }
    }

    public static class DualControlProperties {
        private boolean enabled = false;
        private int requiredApprovers = 2;
        private boolean allowEmergencyBypass = true;
        private boolean allowSelfApproval = false;
        private List<String> authorizedApprovers = List.of();

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getRequiredApprovers() { return requiredApprovers; }
        public void setRequiredApprovers(int requiredApprovers) { this.requiredApprovers = requiredApprovers; }
        public boolean isAllowEmergencyBypass() { return allowEmergencyBypass; }
        public void setAllowEmergencyBypass(boolean allowEmergencyBypass) { this.allowEmergencyBypass = allowEmergencyBypass; }
        public boolean isAllowSelfApproval() { return allowSelfApproval; }
        public void setAllowSelfApproval(boolean allowSelfApproval) { this.allowSelfApproval = allowSelfApproval; }
        public List<String> getAuthorizedApprovers() { return authorizedApprovers; }
        public void setAuthorizedApprovers(List<String> authorizedApprovers) { this.authorizedApprovers = authorizedApprovers; }
    }

    public static class DataResidencyProperties {
        private boolean enabled = false;
        private List<String> allowedRegions = List.of("europe-west2", "europe-west1"); // UK and EU default
        private boolean blockViolations = true;
        private boolean enforceUkResidency = true;
        private boolean allowUnknownRegions = false;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public List<String> getAllowedRegions() { return allowedRegions; }
        public void setAllowedRegions(List<String> allowedRegions) { this.allowedRegions = allowedRegions; }
        public boolean isBlockViolations() { return blockViolations; }
        public void setBlockViolations(boolean blockViolations) { this.blockViolations = blockViolations; }
        public boolean isEnforceUkResidency() { return enforceUkResidency; }
        public void setEnforceUkResidency(boolean enforceUkResidency) { this.enforceUkResidency = enforceUkResidency; }
        public boolean isAllowUnknownRegions() { return allowUnknownRegions; }
        public void setAllowUnknownRegions(boolean allowUnknownRegions) { this.allowUnknownRegions = allowUnknownRegions; }
    }

    public static class PrivacyProperties {
        private PiiPatternProperties piiPattern = new PiiPatternProperties();
        private JsonPathProperties jsonPath = new JsonPathProperties();

        public PiiPatternProperties getPiiPattern() {
            return piiPattern;
        }

        public void setPiiPattern(PiiPatternProperties piiPattern) {
            this.piiPattern = piiPattern;
        }

        public JsonPathProperties getJsonPath() {
            return jsonPath;
        }

        public void setJsonPath(JsonPathProperties jsonPath) {
            this.jsonPath = jsonPath;
        }
    }

    public static class PiiPatternProperties {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class JsonPathProperties {
        private boolean enabled = true;
        private List<String> paths = List.of(
            "$.password",
            "$.secret",
            "$.apiKey",
            "$.creditCard",
            "$.ssn"
        );

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getPaths() {
            return paths;
        }

        public void setPaths(List<String> paths) {
            this.paths = paths;
        }
    }

    public static class PromptInjectionProperties {
        private boolean enabled = true;
        private boolean blockOnDetection = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isBlockOnDetection() {
            return blockOnDetection;
        }

        public void setBlockOnDetection(boolean blockOnDetection) {
            this.blockOnDetection = blockOnDetection;
        }
    }
}
