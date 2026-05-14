package com.neullabs.regulus.policy.residency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Data Residency enforcement for UK financial services compliance.
 * Ensures AI processing occurs in approved regions only.
 *
 * <p>Compliance requirements addressed:
 * <ul>
 *   <li>FCA SYSC 13.9 - Outsourcing requirements</li>
 *   <li>PRA SS2/21 - Outsourcing and third party risk</li>
 *   <li>UK GDPR - Data transfer restrictions</li>
 *   <li>EBA Guidelines on outsourcing arrangements</li>
 * </ul>
 */
public class DataResidencyEnforcer {

    private static final Logger log = LoggerFactory.getLogger(DataResidencyEnforcer.class);

    private final DataResidencyConfig config;
    private final Set<String> allowedRegions;
    private final Map<String, DataClassification> dataClassifications;
    private final List<DataResidencyViolation> violations = Collections.synchronizedList(new ArrayList<>());

    public DataResidencyEnforcer(DataResidencyConfig config) {
        this.config = config;
        this.allowedRegions = new HashSet<>(config.getAllowedRegions());
        this.dataClassifications = new HashMap<>(config.getDataClassifications());
        log.info("Data residency enforcer initialized with {} allowed regions", allowedRegions.size());
    }

    /**
     * Check if a request is allowed based on data residency rules.
     *
     * @param request the residency check request
     * @return the result of the check
     */
    public ResidencyCheckResult checkResidency(ResidencyCheckRequest request) {
        // Get data classification
        DataClassification classification = dataClassifications.getOrDefault(
            request.dataType(), DataClassification.STANDARD);

        // Check if target region is allowed for this classification
        Set<String> allowedForClassification = getRegionsForClassification(classification);

        if (allowedForClassification.isEmpty()) {
            allowedForClassification = allowedRegions;
        }

        boolean regionAllowed = allowedForClassification.contains(request.targetRegion());

        if (!regionAllowed) {
            DataResidencyViolation violation = new DataResidencyViolation(
                UUID.randomUUID().toString(),
                request.requestId(),
                request.dataType(),
                classification,
                request.targetRegion(),
                allowedForClassification,
                request.requestedBy(),
                java.time.Instant.now()
            );
            violations.add(violation);

            log.warn("Data residency violation: {} -> {} (allowed: {})",
                request.dataType(), request.targetRegion(), allowedForClassification);

            if (config.isBlockViolations()) {
                return ResidencyCheckResult.blocked(
                    "Data residency violation: region " + request.targetRegion() +
                    " not allowed for " + classification.name() + " data",
                    violation
                );
            }
        }

        // Check transfer restrictions
        if (request.sourceRegion() != null && !request.sourceRegion().equals(request.targetRegion())) {
            TransferRestriction restriction = checkTransferRestriction(
                request.sourceRegion(), request.targetRegion(), classification);

            if (restriction != null && restriction.isBlocked()) {
                return ResidencyCheckResult.blocked(
                    "Cross-border transfer not allowed: " + restriction.reason(),
                    null
                );
            }

            if (restriction != null && restriction.requiresApproval()) {
                return ResidencyCheckResult.requiresApproval(
                    "Cross-border transfer requires approval: " + restriction.reason()
                );
            }
        }

        // Check UK-specific requirements
        if (classification == DataClassification.UK_REGULATED && config.isEnforceUkResidency()) {
            if (!isUkOrEquivalentRegion(request.targetRegion())) {
                return ResidencyCheckResult.blocked(
                    "UK regulated data must remain in UK or equivalent regions",
                    null
                );
            }
        }

        return ResidencyCheckResult.allowed();
    }

    /**
     * Get allowed regions for a model/endpoint.
     *
     * @param modelId the model ID
     * @return set of allowed regions
     */
    public Set<String> getAllowedRegionsForModel(String modelId) {
        // Check if model has specific region restrictions
        Set<String> modelRegions = config.getModelRegionRestrictions().get(modelId);
        if (modelRegions != null && !modelRegions.isEmpty()) {
            return modelRegions;
        }
        return allowedRegions;
    }

    /**
     * Validate that an endpoint is in an allowed region.
     *
     * @param endpoint the endpoint URL
     * @return true if endpoint is in an allowed region
     */
    public boolean isEndpointAllowed(String endpoint) {
        String region = extractRegionFromEndpoint(endpoint);
        if (region == null) {
            // Cannot determine region, apply default policy
            return config.isAllowUnknownRegions();
        }
        return allowedRegions.contains(region);
    }

    /**
     * Get all violations.
     */
    public List<DataResidencyViolation> getViolations() {
        return new ArrayList<>(violations);
    }

    /**
     * Clear violations (for testing).
     */
    public void clearViolations() {
        violations.clear();
    }

    private Set<String> getRegionsForClassification(DataClassification classification) {
        return config.getClassificationRegions().getOrDefault(classification, Set.of());
    }

    private TransferRestriction checkTransferRestriction(String sourceRegion, String targetRegion,
                                                          DataClassification classification) {
        // Check explicit restrictions
        String key = sourceRegion + "->" + targetRegion;
        if (config.getTransferRestrictions().containsKey(key)) {
            return config.getTransferRestrictions().get(key);
        }

        // Check classification-based restrictions
        if (classification == DataClassification.PII || classification == DataClassification.SENSITIVE) {
            // PII transfers outside UK/EEA need approval
            if (isUkOrEeaRegion(sourceRegion) && !isUkOrEeaRegion(targetRegion)) {
                return new TransferRestriction(false, true,
                    "Transfer of " + classification + " data outside UK/EEA requires approval");
            }
        }

        if (classification == DataClassification.UK_REGULATED) {
            // UK regulated data generally must stay in UK
            if (!isUkOrEquivalentRegion(targetRegion)) {
                return new TransferRestriction(true, false,
                    "UK regulated data cannot leave UK/equivalent regions");
            }
        }

        return null;
    }

    private boolean isUkOrEeaRegion(String region) {
        return region != null && (
            region.startsWith("europe-") ||
            region.equals("uk") ||
            region.equals("eu") ||
            region.contains("london") ||
            region.contains("frankfurt") ||
            region.contains("amsterdam") ||
            region.contains("ireland")
        );
    }

    private boolean isUkOrEquivalentRegion(String region) {
        // UK and adequacy-approved regions
        return region != null && (
            region.contains("london") ||
            region.contains("europe-west2") ||  // GCP London
            region.equals("uk") ||
            region.equals("uksouth") ||          // Azure UK South
            region.equals("ukwest") ||           // Azure UK West
            region.equals("eu-west-2")           // AWS London
        );
    }

    private String extractRegionFromEndpoint(String endpoint) {
        if (endpoint == null) return null;

        // GCP patterns
        if (endpoint.contains("europe-west2")) return "europe-west2";
        if (endpoint.contains("europe-west1")) return "europe-west1";
        if (endpoint.contains("us-central1")) return "us-central1";

        // AWS patterns
        if (endpoint.contains("eu-west-2")) return "eu-west-2";
        if (endpoint.contains("us-east-1")) return "us-east-1";

        // Azure patterns
        if (endpoint.contains("uksouth")) return "uksouth";
        if (endpoint.contains("ukwest")) return "ukwest";

        return null;
    }

    /**
     * Data classification levels.
     */
    public enum DataClassification {
        PUBLIC,          // No restrictions
        STANDARD,        // Standard business data
        INTERNAL,        // Internal only
        CONFIDENTIAL,    // Confidential business data
        PII,            // Personally Identifiable Information
        SENSITIVE,       // Sensitive personal data (special categories)
        UK_REGULATED,    // UK FCA/PRA regulated data
        CRITICAL         // Critical/systemically important
    }

    /**
     * Request for residency check.
     */
    public record ResidencyCheckRequest(
        String requestId,
        String dataType,
        String sourceRegion,
        String targetRegion,
        String requestedBy,
        Map<String, Object> metadata
    ) {
        public static ResidencyCheckRequest of(String dataType, String targetRegion) {
            return new ResidencyCheckRequest(
                UUID.randomUUID().toString(),
                dataType,
                null,
                targetRegion,
                null,
                Map.of()
            );
        }
    }

    /**
     * Result of residency check.
     */
    public record ResidencyCheckResult(
        boolean isAllowed,
        boolean requiresApproval,
        String message,
        DataResidencyViolation violation
    ) {
        public static ResidencyCheckResult allowed() {
            return new ResidencyCheckResult(true, false, null, null);
        }

        public static ResidencyCheckResult blocked(String message, DataResidencyViolation violation) {
            return new ResidencyCheckResult(false, false, message, violation);
        }

        public static ResidencyCheckResult requiresApproval(String message) {
            return new ResidencyCheckResult(false, true, message, null);
        }
    }

    /**
     * Data residency violation record.
     */
    public record DataResidencyViolation(
        String violationId,
        String requestId,
        String dataType,
        DataClassification classification,
        String attemptedRegion,
        Set<String> allowedRegions,
        String requestedBy,
        java.time.Instant timestamp
    ) {}

    /**
     * Transfer restriction rule.
     */
    public record TransferRestriction(
        boolean blocked,
        boolean requiresApproval,
        String reason
    ) {
        public boolean isBlocked() { return blocked; }
        public boolean requiresApproval() { return requiresApproval; }
    }

    /**
     * Configuration for data residency enforcement.
     */
    public static class DataResidencyConfig {
        private List<String> allowedRegions = List.of("europe-west2", "europe-west1"); // Default to UK/EU
        private boolean blockViolations = true;
        private boolean allowUnknownRegions = false;
        private boolean enforceUkResidency = true;
        private Map<String, DataClassification> dataClassifications = new HashMap<>();
        private Map<DataClassification, Set<String>> classificationRegions = new HashMap<>();
        private Map<String, Set<String>> modelRegionRestrictions = new HashMap<>();
        private Map<String, TransferRestriction> transferRestrictions = new HashMap<>();

        public List<String> getAllowedRegions() { return allowedRegions; }
        public void setAllowedRegions(List<String> allowedRegions) { this.allowedRegions = allowedRegions; }
        public boolean isBlockViolations() { return blockViolations; }
        public void setBlockViolations(boolean blockViolations) { this.blockViolations = blockViolations; }
        public boolean isAllowUnknownRegions() { return allowUnknownRegions; }
        public void setAllowUnknownRegions(boolean allowUnknownRegions) { this.allowUnknownRegions = allowUnknownRegions; }
        public boolean isEnforceUkResidency() { return enforceUkResidency; }
        public void setEnforceUkResidency(boolean enforceUkResidency) { this.enforceUkResidency = enforceUkResidency; }
        public Map<String, DataClassification> getDataClassifications() { return dataClassifications; }
        public void setDataClassifications(Map<String, DataClassification> dataClassifications) { this.dataClassifications = dataClassifications; }
        public Map<DataClassification, Set<String>> getClassificationRegions() { return classificationRegions; }
        public void setClassificationRegions(Map<DataClassification, Set<String>> classificationRegions) { this.classificationRegions = classificationRegions; }
        public Map<String, Set<String>> getModelRegionRestrictions() { return modelRegionRestrictions; }
        public void setModelRegionRestrictions(Map<String, Set<String>> modelRegionRestrictions) { this.modelRegionRestrictions = modelRegionRestrictions; }
        public Map<String, TransferRestriction> getTransferRestrictions() { return transferRestrictions; }
        public void setTransferRestrictions(Map<String, TransferRestriction> transferRestrictions) { this.transferRestrictions = transferRestrictions; }
    }
}
