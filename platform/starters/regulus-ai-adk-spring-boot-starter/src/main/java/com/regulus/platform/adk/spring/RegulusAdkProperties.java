package com.regulus.platform.adk.spring;

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
    private final Adk adk = new Adk();

    public Compliance getCompliance() { return compliance; }
    public Adk getAdk() { return adk; }

    public static class Compliance {
        private List<String> profiles = new ArrayList<>();

        public List<String> getProfiles() { return profiles; }
        public void setProfiles(List<String> profiles) { this.profiles = profiles; }
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
