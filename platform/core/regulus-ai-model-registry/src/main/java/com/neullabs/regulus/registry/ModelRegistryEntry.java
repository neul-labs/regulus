package com.neullabs.regulus.registry;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Model Registry entry representing a registered AI/ML model.
 * Implements SS1/23 (PRA) Model Risk Management requirements for UK financial services.
 *
 * <p>Key SS1/23 Requirements addressed:
 * <ul>
 *   <li>Model inventory and classification</li>
 *   <li>Model ownership and accountability</li>
 *   <li>Documentation and change tracking</li>
 *   <li>Performance monitoring baseline</li>
 *   <li>Risk tier classification</li>
 * </ul>
 */
public record ModelRegistryEntry(
    String modelId,
    String name,
    String version,
    String description,
    ModelType type,
    RiskTier riskTier,
    ModelStatus status,
    Ownership ownership,
    Validation validation,
    Performance performance,
    Deployment deployment,
    List<String> dataLineage,
    Map<String, Object> metadata,
    Instant createdAt,
    Instant updatedAt,
    String updatedBy
) {
    /**
     * Model type classification.
     */
    public enum ModelType {
        LLM,                    // Large Language Model
        CLASSIFICATION,         // Classification model
        REGRESSION,            // Regression model
        RECOMMENDATION,        // Recommendation model
        ANOMALY_DETECTION,     // Anomaly detection
        NLP,                   // Natural Language Processing
        COMPUTER_VISION,       // Computer Vision
        REINFORCEMENT_LEARNING,// RL model
        ENSEMBLE,              // Ensemble of models
        RULE_BASED,            // Rule-based system
        AGENT                  // AI Agent
    }

    /**
     * Risk tier classification per SS1/23.
     * Tier 1 = highest risk, requires most oversight.
     */
    public enum RiskTier {
        TIER_1(1, "High materiality - direct customer impact, financial decisions"),
        TIER_2(2, "Medium materiality - indirect customer impact"),
        TIER_3(3, "Low materiality - internal operations, limited impact"),
        TIER_4(4, "Minimal risk - monitoring, reporting only");

        private final int level;
        private final String description;

        RiskTier(int level, String description) {
            this.level = level;
            this.description = description;
        }

        public int getLevel() { return level; }
        public String getDescription() { return description; }
    }

    /**
     * Model lifecycle status.
     */
    public enum ModelStatus {
        DEVELOPMENT,           // In development
        VALIDATION,            // Under validation
        PENDING_APPROVAL,      // Awaiting approval
        APPROVED,              // Approved for use
        DEPLOYED,              // Currently deployed
        DEPRECATED,            // Deprecated, should not be used
        RETIRED,               // Retired, not in use
        SUSPENDED              // Temporarily suspended (e.g., due to issues)
    }

    /**
     * Model ownership information required by SS1/23.
     */
    public record Ownership(
        String modelOwner,           // Accountable executive
        String modelDeveloper,       // Technical owner
        String businessUnit,         // Business unit
        String validationTeam,       // Independent validation team
        String riskReviewDate,       // Date of last risk review
        List<String> stakeholders    // Additional stakeholders
    ) {}

    /**
     * Model validation information.
     */
    public record Validation(
        Instant lastValidationDate,
        Instant nextValidationDate,
        String validationStatus,     // PASSED, FAILED, PENDING
        String validatorId,          // ID of validator
        List<ValidationCheck> checks,
        Map<String, Object> results
    ) {}

    /**
     * Individual validation check.
     */
    public record ValidationCheck(
        String checkId,
        String checkName,
        String checkType,            // ACCURACY, BIAS, FAIRNESS, ROBUSTNESS, etc.
        String status,               // PASSED, FAILED, WARNING
        String message,
        Map<String, Object> metrics
    ) {}

    /**
     * Model performance metrics.
     */
    public record Performance(
        Map<String, Double> metrics,      // accuracy, precision, recall, etc.
        Map<String, Double> thresholds,   // Threshold values for alerts
        Instant lastMeasured,
        PerformanceStatus status
    ) {
        public enum PerformanceStatus {
            NOMINAL,       // Within expected range
            DEGRADED,      // Below threshold but acceptable
            CRITICAL,      // Requires immediate attention
            UNKNOWN        // Not yet measured
        }
    }

    /**
     * Deployment information.
     */
    public record Deployment(
        String environment,          // DEV, UAT, PROD
        String deploymentTarget,     // GCP, Azure, On-prem
        String region,               // Data residency region
        Instant deployedAt,
        String deployedBy,
        String configVersion,
        Map<String, String> endpoints
    ) {}

    /**
     * Builder for ModelRegistryEntry.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String modelId;
        private String name;
        private String version;
        private String description;
        private ModelType type;
        private RiskTier riskTier = RiskTier.TIER_3;
        private ModelStatus status = ModelStatus.DEVELOPMENT;
        private Ownership ownership;
        private Validation validation;
        private Performance performance;
        private Deployment deployment;
        private List<String> dataLineage = List.of();
        private Map<String, Object> metadata = Map.of();
        private Instant createdAt = Instant.now();
        private Instant updatedAt = Instant.now();
        private String updatedBy;

        public Builder modelId(String modelId) { this.modelId = modelId; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder version(String version) { this.version = version; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder type(ModelType type) { this.type = type; return this; }
        public Builder riskTier(RiskTier riskTier) { this.riskTier = riskTier; return this; }
        public Builder status(ModelStatus status) { this.status = status; return this; }
        public Builder ownership(Ownership ownership) { this.ownership = ownership; return this; }
        public Builder validation(Validation validation) { this.validation = validation; return this; }
        public Builder performance(Performance performance) { this.performance = performance; return this; }
        public Builder deployment(Deployment deployment) { this.deployment = deployment; return this; }
        public Builder dataLineage(List<String> dataLineage) { this.dataLineage = dataLineage; return this; }
        public Builder metadata(Map<String, Object> metadata) { this.metadata = metadata; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder updatedBy(String updatedBy) { this.updatedBy = updatedBy; return this; }

        public ModelRegistryEntry build() {
            return new ModelRegistryEntry(
                modelId, name, version, description, type, riskTier, status,
                ownership, validation, performance, deployment, dataLineage,
                metadata, createdAt, updatedAt, updatedBy
            );
        }
    }
}
