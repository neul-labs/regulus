package com.regulus.platform.observability.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for observability features.
 */
@ConfigurationProperties(prefix = "regulus.ai.observability")
public class ObservabilityProperties {

    /**
     * Whether observability features are enabled.
     */
    private boolean enabled = true;

    /**
     * Metrics configuration.
     */
    private MetricsConfig metrics = new MetricsConfig();

    /**
     * Tracing configuration.
     */
    private TracingConfig tracing = new TracingConfig();

    /**
     * Audit logging configuration.
     */
    private AuditConfig audit = new AuditConfig();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public MetricsConfig getMetrics() {
        return metrics;
    }

    public void setMetrics(MetricsConfig metrics) {
        this.metrics = metrics;
    }

    public TracingConfig getTracing() {
        return tracing;
    }

    public void setTracing(TracingConfig tracing) {
        this.tracing = tracing;
    }

    public AuditConfig getAudit() {
        return audit;
    }

    public void setAudit(AuditConfig audit) {
        this.audit = audit;
    }

    public static class MetricsConfig {
        private boolean enabled = true;
        private boolean includeTokenCounts = true;
        private boolean includeDurations = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isIncludeTokenCounts() {
            return includeTokenCounts;
        }

        public void setIncludeTokenCounts(boolean includeTokenCounts) {
            this.includeTokenCounts = includeTokenCounts;
        }

        public boolean isIncludeDurations() {
            return includeDurations;
        }

        public void setIncludeDurations(boolean includeDurations) {
            this.includeDurations = includeDurations;
        }
    }

    public static class TracingConfig {
        private boolean enabled = true;
        private double samplingRate = 1.0;
        private boolean propagateContext = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public double getSamplingRate() {
            return samplingRate;
        }

        public void setSamplingRate(double samplingRate) {
            this.samplingRate = samplingRate;
        }

        public boolean isPropagateContext() {
            return propagateContext;
        }

        public void setPropagateContext(boolean propagateContext) {
            this.propagateContext = propagateContext;
        }
    }

    public static class AuditConfig {
        private boolean enabled = true;
        private String sink = "log"; // log, kafka, both
        private String kafkaTopic = "regulus-audit-events";
        private boolean includeRequestPayload = false;
        private boolean includeResponsePayload = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getSink() {
            return sink;
        }

        public void setSink(String sink) {
            this.sink = sink;
        }

        public String getKafkaTopic() {
            return kafkaTopic;
        }

        public void setKafkaTopic(String kafkaTopic) {
            this.kafkaTopic = kafkaTopic;
        }

        public boolean isIncludeRequestPayload() {
            return includeRequestPayload;
        }

        public void setIncludeRequestPayload(boolean includeRequestPayload) {
            this.includeRequestPayload = includeRequestPayload;
        }

        public boolean isIncludeResponsePayload() {
            return includeResponsePayload;
        }

        public void setIncludeResponsePayload(boolean includeResponsePayload) {
            this.includeResponsePayload = includeResponsePayload;
        }
    }
}
