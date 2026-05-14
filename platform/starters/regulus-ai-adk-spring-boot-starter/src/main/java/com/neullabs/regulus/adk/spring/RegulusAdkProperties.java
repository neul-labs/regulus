package com.neullabs.regulus.adk.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring Boot configuration root for Regulus' ADK integration.
 *
 * <pre>{@code
 * regulus:
 *   compliance:
 *     profiles: [eu-ai-act, uk-gdpr, fca-sysc]
 *   adk:
 *     name: my-agent
 *     session-service:
 *       kind: vertex-ai
 *       project-id: my-project
 *       location: europe-west2
 *       cmek-key-name: projects/.../cryptoKeys/regulus-sessions
 *     audit:
 *       sink: kafka
 *       kafka-topic: audit.regulus.v1
 *     kill-switch:
 *       enabled: true
 *       dual-control: true
 *     residency:
 *       allowed-regions: [europe-west2]
 *     model-risk:
 *       tenant-tier: STANDARD
 *     dev-server:
 *       enabled: false
 * }</pre>
 */
@ConfigurationProperties(prefix = "regulus")
public class RegulusAdkProperties {

    private final Compliance compliance = new Compliance();
    private final Governance governance = new Governance();
    private final Grc grc = new Grc();
    private final Adk adk = new Adk();

    public Compliance getCompliance() { return compliance; }
    public Governance getGovernance() { return governance; }
    public Grc getGrc() { return grc; }
    public Adk getAdk() { return adk; }

    public static class Compliance {
        private List<String> profiles = new ArrayList<>();

        public List<String> getProfiles() { return profiles; }
        public void setProfiles(List<String> profiles) { this.profiles = profiles; }
    }

    /**
     * Active AI governance frameworks. Voluntary; complements
     * {@link Compliance#profiles}. Example:
     * <pre>regulus.governance.frameworks: [nist-ai-rmf, nist-ai-rmf-600-1, iso-42001]</pre>
     */
    public static class Governance {
        private List<String> frameworks = new ArrayList<>();

        public List<String> getFrameworks() { return frameworks; }
        public void setFrameworks(List<String> frameworks) { this.frameworks = frameworks; }
    }

    /**
     * GRC tool evidence emitters. Opt-in; no adapter is wired by default.
     */
    public static class Grc {
        private final ServiceNow servicenowIrm = new ServiceNow();
        private final OneTrust onetrustAiGov = new OneTrust();
        private final MetricStream metricstream = new MetricStream();
        private final Webhook webhook = new Webhook();
        private boolean stdout = false;

        public ServiceNow getServicenowIrm() { return servicenowIrm; }
        public OneTrust getOnetrustAiGov() { return onetrustAiGov; }
        public MetricStream getMetricstream() { return metricstream; }
        public Webhook getWebhook() { return webhook; }
        public boolean isStdout() { return stdout; }
        public void setStdout(boolean stdout) { this.stdout = stdout; }

        public static class ServiceNow {
            private boolean enabled = false;
            private String baseUri;
            private String username;
            private String password;
            private String bearerToken;

            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            public String getBaseUri() { return baseUri; }
            public void setBaseUri(String baseUri) { this.baseUri = baseUri; }
            public String getUsername() { return username; }
            public void setUsername(String username) { this.username = username; }
            public String getPassword() { return password; }
            public void setPassword(String password) { this.password = password; }
            public String getBearerToken() { return bearerToken; }
            public void setBearerToken(String bearerToken) { this.bearerToken = bearerToken; }
        }

        public static class OneTrust {
            private boolean enabled = false;
            private String baseUri;
            private String apiKey;

            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            public String getBaseUri() { return baseUri; }
            public void setBaseUri(String baseUri) { this.baseUri = baseUri; }
            public String getApiKey() { return apiKey; }
            public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        }

        public static class MetricStream {
            private boolean enabled = false;
            private String baseUri;
            private String authToken;
            private String intakeAppName = "AIControlEvidence";

            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            public String getBaseUri() { return baseUri; }
            public void setBaseUri(String baseUri) { this.baseUri = baseUri; }
            public String getAuthToken() { return authToken; }
            public void setAuthToken(String authToken) { this.authToken = authToken; }
            public String getIntakeAppName() { return intakeAppName; }
            public void setIntakeAppName(String intakeAppName) { this.intakeAppName = intakeAppName; }
        }

        public static class Webhook {
            private boolean enabled = false;
            private String endpoint;
            /** Hex-encoded HMAC-SHA256 secret. */
            private String hmacKeyHex;

            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            public String getEndpoint() { return endpoint; }
            public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
            public String getHmacKeyHex() { return hmacKeyHex; }
            public void setHmacKeyHex(String hmacKeyHex) { this.hmacKeyHex = hmacKeyHex; }
        }
    }

    public static class Adk {
        private String name = "regulus-agent";
        private final SessionService sessionService = new SessionService();
        private final Audit audit = new Audit();
        private final KillSwitch killSwitch = new KillSwitch();
        private final Residency residency = new Residency();
        private final ModelRisk modelRisk = new ModelRisk();
        private final DevServer devServer = new DevServer();

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public SessionService getSessionService() { return sessionService; }
        public Audit getAudit() { return audit; }
        public KillSwitch getKillSwitch() { return killSwitch; }
        public Residency getResidency() { return residency; }
        public ModelRisk getModelRisk() { return modelRisk; }
        public DevServer getDevServer() { return devServer; }
    }

    public static class SessionService {
        /** {@code in-memory} | {@code vertex-ai} | {@code firestore}. */
        private String kind = "in-memory";
        private String projectId;
        private String location;
        private String cmekKeyName;

        public String getKind() { return kind; }
        public void setKind(String kind) { this.kind = kind; }
        public String getProjectId() { return projectId; }
        public void setProjectId(String projectId) { this.projectId = projectId; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public String getCmekKeyName() { return cmekKeyName; }
        public void setCmekKeyName(String cmekKeyName) { this.cmekKeyName = cmekKeyName; }
    }

    public static class Audit {
        /** {@code stdout} | {@code kafka}. */
        private String sink = "stdout";
        private String kafkaTopic = "audit.regulus.v1";

        public String getSink() { return sink; }
        public void setSink(String sink) { this.sink = sink; }
        public String getKafkaTopic() { return kafkaTopic; }
        public void setKafkaTopic(String topic) { this.kafkaTopic = topic; }
    }

    public static class KillSwitch {
        private boolean enabled = true;
        private boolean dualControl = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public boolean isDualControl() { return dualControl; }
        public void setDualControl(boolean dualControl) { this.dualControl = dualControl; }
    }

    public static class Residency {
        private List<String> allowedRegions = new ArrayList<>();
        private boolean requireCmek = false;

        public List<String> getAllowedRegions() { return allowedRegions; }
        public void setAllowedRegions(List<String> regions) { this.allowedRegions = regions; }
        public boolean isRequireCmek() { return requireCmek; }
        public void setRequireCmek(boolean v) { this.requireCmek = v; }
    }

    public static class ModelRisk {
        /** {@code EXPERIMENTAL} | {@code STANDARD} | {@code REGULATED} | {@code HIGH_RISK}. */
        private String tenantTier = "STANDARD";

        public String getTenantTier() { return tenantTier; }
        public void setTenantTier(String tier) { this.tenantTier = tier; }
    }

    public static class DevServer {
        private boolean enabled = false;
        private int port = 8081;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
    }
}
