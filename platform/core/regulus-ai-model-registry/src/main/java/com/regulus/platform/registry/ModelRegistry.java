package com.regulus.platform.registry;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Model Registry interface for SS1/23 compliant model management.
 * Provides a central inventory of all AI/ML models with full lifecycle tracking.
 *
 * <p>This registry supports:
 * <ul>
 *   <li>Model registration and deregistration</li>
 *   <li>Version management</li>
 *   <li>Risk tier classification</li>
 *   <li>Validation tracking</li>
 *   <li>Deployment state management</li>
 *   <li>Audit trail for all changes</li>
 * </ul>
 */
public interface ModelRegistry {

    /**
     * Register a new model in the registry.
     *
     * @param entry the model entry to register
     * @return the registered entry with generated ID if not provided
     */
    ModelRegistryEntry register(ModelRegistryEntry entry);

    /**
     * Update an existing model entry.
     *
     * @param entry the updated model entry
     * @return the updated entry
     * @throws ModelNotFoundException if model not found
     */
    ModelRegistryEntry update(ModelRegistryEntry entry);

    /**
     * Get a model by ID.
     *
     * @param modelId the model ID
     * @return the model entry if found
     */
    Optional<ModelRegistryEntry> getById(String modelId);

    /**
     * Get a model by name and version.
     *
     * @param name the model name
     * @param version the model version
     * @return the model entry if found
     */
    Optional<ModelRegistryEntry> getByNameAndVersion(String name, String version);

    /**
     * Get all versions of a model by name.
     *
     * @param name the model name
     * @return list of all versions
     */
    List<ModelRegistryEntry> getVersionsByName(String name);

    /**
     * Get the latest version of a model.
     *
     * @param name the model name
     * @return the latest version if found
     */
    Optional<ModelRegistryEntry> getLatestVersion(String name);

    /**
     * List all models.
     *
     * @return all registered models
     */
    List<ModelRegistryEntry> listAll();

    /**
     * List models by status.
     *
     * @param status the model status
     * @return models with the given status
     */
    List<ModelRegistryEntry> listByStatus(ModelRegistryEntry.ModelStatus status);

    /**
     * List models by risk tier.
     *
     * @param riskTier the risk tier
     * @return models in the given risk tier
     */
    List<ModelRegistryEntry> listByRiskTier(ModelRegistryEntry.RiskTier riskTier);

    /**
     * List models by type.
     *
     * @param type the model type
     * @return models of the given type
     */
    List<ModelRegistryEntry> listByType(ModelRegistryEntry.ModelType type);

    /**
     * List models matching a filter.
     *
     * @param filter the filter predicate
     * @return matching models
     */
    List<ModelRegistryEntry> listByFilter(Predicate<ModelRegistryEntry> filter);

    /**
     * Update model status.
     *
     * @param modelId the model ID
     * @param status the new status
     * @param updatedBy who made the change
     * @return the updated entry
     */
    ModelRegistryEntry updateStatus(String modelId, ModelRegistryEntry.ModelStatus status, String updatedBy);

    /**
     * Record a validation result for a model.
     *
     * @param modelId the model ID
     * @param validation the validation record
     * @return the updated entry
     */
    ModelRegistryEntry recordValidation(String modelId, ModelRegistryEntry.Validation validation);

    /**
     * Record deployment information for a model.
     *
     * @param modelId the model ID
     * @param deployment the deployment record
     * @return the updated entry
     */
    ModelRegistryEntry recordDeployment(String modelId, ModelRegistryEntry.Deployment deployment);

    /**
     * Record performance metrics for a model.
     *
     * @param modelId the model ID
     * @param performance the performance record
     * @return the updated entry
     */
    ModelRegistryEntry recordPerformance(String modelId, ModelRegistryEntry.Performance performance);

    /**
     * Check if a model is approved for production use.
     *
     * @param modelId the model ID
     * @return true if model is approved and can be deployed
     */
    boolean isApprovedForProduction(String modelId);

    /**
     * Get models requiring validation review.
     * Returns models where next validation date has passed.
     *
     * @return models requiring validation
     */
    List<ModelRegistryEntry> getModelsRequiringValidation();

    /**
     * Get models with degraded performance.
     *
     * @return models with performance issues
     */
    List<ModelRegistryEntry> getModelsWithPerformanceIssues();

    /**
     * Get audit history for a model.
     *
     * @param modelId the model ID
     * @return list of audit entries
     */
    List<ModelAuditEntry> getAuditHistory(String modelId);

    /**
     * Exception thrown when model is not found.
     */
    class ModelNotFoundException extends RuntimeException {
        public ModelNotFoundException(String modelId) {
            super("Model not found: " + modelId);
        }
    }

    /**
     * Audit entry for model changes.
     */
    record ModelAuditEntry(
        String auditId,
        String modelId,
        String action,          // REGISTER, UPDATE, STATUS_CHANGE, VALIDATION, DEPLOYMENT
        String previousState,
        String newState,
        String changedBy,
        java.time.Instant timestamp,
        java.util.Map<String, Object> details
    ) {}
}
