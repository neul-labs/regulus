package com.regulus.platform.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * In-memory implementation of Model Registry.
 * Suitable for development and testing. Production should use persistence-backed implementation.
 */
public class InMemoryModelRegistry implements ModelRegistry {

    private static final Logger log = LoggerFactory.getLogger(InMemoryModelRegistry.class);

    private final Map<String, ModelRegistryEntry> models = new ConcurrentHashMap<>();
    private final Map<String, List<ModelAuditEntry>> auditLog = new ConcurrentHashMap<>();

    public InMemoryModelRegistry() {
        log.info("In-memory model registry initialized");
    }

    @Override
    public ModelRegistryEntry register(ModelRegistryEntry entry) {
        String modelId = entry.modelId() != null ? entry.modelId() : generateModelId(entry);

        ModelRegistryEntry registered = ModelRegistryEntry.builder()
            .modelId(modelId)
            .name(entry.name())
            .version(entry.version())
            .description(entry.description())
            .type(entry.type())
            .riskTier(entry.riskTier())
            .status(entry.status())
            .ownership(entry.ownership())
            .validation(entry.validation())
            .performance(entry.performance())
            .deployment(entry.deployment())
            .dataLineage(entry.dataLineage())
            .metadata(entry.metadata())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .updatedBy(entry.updatedBy())
            .build();

        models.put(modelId, registered);
        recordAudit(modelId, "REGISTER", null, registered.status().name(), entry.updatedBy(), Map.of(
            "name", entry.name(),
            "version", entry.version(),
            "riskTier", entry.riskTier().name()
        ));

        log.info("Registered model: {} v{} ({})", entry.name(), entry.version(), modelId);
        return registered;
    }

    @Override
    public ModelRegistryEntry update(ModelRegistryEntry entry) {
        if (!models.containsKey(entry.modelId())) {
            throw new ModelNotFoundException(entry.modelId());
        }

        ModelRegistryEntry previous = models.get(entry.modelId());
        ModelRegistryEntry updated = ModelRegistryEntry.builder()
            .modelId(entry.modelId())
            .name(entry.name())
            .version(entry.version())
            .description(entry.description())
            .type(entry.type())
            .riskTier(entry.riskTier())
            .status(entry.status())
            .ownership(entry.ownership())
            .validation(entry.validation())
            .performance(entry.performance())
            .deployment(entry.deployment())
            .dataLineage(entry.dataLineage())
            .metadata(entry.metadata())
            .createdAt(previous.createdAt())
            .updatedAt(Instant.now())
            .updatedBy(entry.updatedBy())
            .build();

        models.put(entry.modelId(), updated);
        recordAudit(entry.modelId(), "UPDATE", previous.status().name(), updated.status().name(),
            entry.updatedBy(), Map.of("changes", "Full update"));

        log.info("Updated model: {}", entry.modelId());
        return updated;
    }

    @Override
    public Optional<ModelRegistryEntry> getById(String modelId) {
        return Optional.ofNullable(models.get(modelId));
    }

    @Override
    public Optional<ModelRegistryEntry> getByNameAndVersion(String name, String version) {
        return models.values().stream()
            .filter(m -> m.name().equals(name) && m.version().equals(version))
            .findFirst();
    }

    @Override
    public List<ModelRegistryEntry> getVersionsByName(String name) {
        return models.values().stream()
            .filter(m -> m.name().equals(name))
            .sorted(Comparator.comparing(ModelRegistryEntry::version).reversed())
            .collect(Collectors.toList());
    }

    @Override
    public Optional<ModelRegistryEntry> getLatestVersion(String name) {
        return getVersionsByName(name).stream().findFirst();
    }

    @Override
    public List<ModelRegistryEntry> listAll() {
        return new ArrayList<>(models.values());
    }

    @Override
    public List<ModelRegistryEntry> listByStatus(ModelRegistryEntry.ModelStatus status) {
        return models.values().stream()
            .filter(m -> m.status() == status)
            .collect(Collectors.toList());
    }

    @Override
    public List<ModelRegistryEntry> listByRiskTier(ModelRegistryEntry.RiskTier riskTier) {
        return models.values().stream()
            .filter(m -> m.riskTier() == riskTier)
            .collect(Collectors.toList());
    }

    @Override
    public List<ModelRegistryEntry> listByType(ModelRegistryEntry.ModelType type) {
        return models.values().stream()
            .filter(m -> m.type() == type)
            .collect(Collectors.toList());
    }

    @Override
    public List<ModelRegistryEntry> listByFilter(Predicate<ModelRegistryEntry> filter) {
        return models.values().stream()
            .filter(filter)
            .collect(Collectors.toList());
    }

    @Override
    public ModelRegistryEntry updateStatus(String modelId, ModelRegistryEntry.ModelStatus status, String updatedBy) {
        ModelRegistryEntry existing = models.get(modelId);
        if (existing == null) {
            throw new ModelNotFoundException(modelId);
        }

        ModelRegistryEntry updated = ModelRegistryEntry.builder()
            .modelId(existing.modelId())
            .name(existing.name())
            .version(existing.version())
            .description(existing.description())
            .type(existing.type())
            .riskTier(existing.riskTier())
            .status(status)
            .ownership(existing.ownership())
            .validation(existing.validation())
            .performance(existing.performance())
            .deployment(existing.deployment())
            .dataLineage(existing.dataLineage())
            .metadata(existing.metadata())
            .createdAt(existing.createdAt())
            .updatedAt(Instant.now())
            .updatedBy(updatedBy)
            .build();

        models.put(modelId, updated);
        recordAudit(modelId, "STATUS_CHANGE", existing.status().name(), status.name(), updatedBy, Map.of());

        log.info("Updated model status: {} -> {}", existing.status(), status);
        return updated;
    }

    @Override
    public ModelRegistryEntry recordValidation(String modelId, ModelRegistryEntry.Validation validation) {
        ModelRegistryEntry existing = models.get(modelId);
        if (existing == null) {
            throw new ModelNotFoundException(modelId);
        }

        ModelRegistryEntry updated = ModelRegistryEntry.builder()
            .modelId(existing.modelId())
            .name(existing.name())
            .version(existing.version())
            .description(existing.description())
            .type(existing.type())
            .riskTier(existing.riskTier())
            .status(existing.status())
            .ownership(existing.ownership())
            .validation(validation)
            .performance(existing.performance())
            .deployment(existing.deployment())
            .dataLineage(existing.dataLineage())
            .metadata(existing.metadata())
            .createdAt(existing.createdAt())
            .updatedAt(Instant.now())
            .updatedBy(validation.validatorId())
            .build();

        models.put(modelId, updated);
        recordAudit(modelId, "VALIDATION", null, validation.validationStatus(),
            validation.validatorId(), Map.of("checks", validation.checks().size()));

        log.info("Recorded validation for model: {} - {}", modelId, validation.validationStatus());
        return updated;
    }

    @Override
    public ModelRegistryEntry recordDeployment(String modelId, ModelRegistryEntry.Deployment deployment) {
        ModelRegistryEntry existing = models.get(modelId);
        if (existing == null) {
            throw new ModelNotFoundException(modelId);
        }

        ModelRegistryEntry updated = ModelRegistryEntry.builder()
            .modelId(existing.modelId())
            .name(existing.name())
            .version(existing.version())
            .description(existing.description())
            .type(existing.type())
            .riskTier(existing.riskTier())
            .status(ModelRegistryEntry.ModelStatus.DEPLOYED)
            .ownership(existing.ownership())
            .validation(existing.validation())
            .performance(existing.performance())
            .deployment(deployment)
            .dataLineage(existing.dataLineage())
            .metadata(existing.metadata())
            .createdAt(existing.createdAt())
            .updatedAt(Instant.now())
            .updatedBy(deployment.deployedBy())
            .build();

        models.put(modelId, updated);
        recordAudit(modelId, "DEPLOYMENT", existing.status().name(), "DEPLOYED",
            deployment.deployedBy(), Map.of(
                "environment", deployment.environment(),
                "region", deployment.region()
            ));

        log.info("Recorded deployment for model: {} to {}", modelId, deployment.environment());
        return updated;
    }

    @Override
    public ModelRegistryEntry recordPerformance(String modelId, ModelRegistryEntry.Performance performance) {
        ModelRegistryEntry existing = models.get(modelId);
        if (existing == null) {
            throw new ModelNotFoundException(modelId);
        }

        ModelRegistryEntry updated = ModelRegistryEntry.builder()
            .modelId(existing.modelId())
            .name(existing.name())
            .version(existing.version())
            .description(existing.description())
            .type(existing.type())
            .riskTier(existing.riskTier())
            .status(existing.status())
            .ownership(existing.ownership())
            .validation(existing.validation())
            .performance(performance)
            .deployment(existing.deployment())
            .dataLineage(existing.dataLineage())
            .metadata(existing.metadata())
            .createdAt(existing.createdAt())
            .updatedAt(Instant.now())
            .updatedBy(existing.updatedBy())
            .build();

        models.put(modelId, updated);

        if (performance.status() != ModelRegistryEntry.Performance.PerformanceStatus.NOMINAL) {
            recordAudit(modelId, "PERFORMANCE_ALERT", "NOMINAL", performance.status().name(),
                "system", Map.of("metrics", performance.metrics()));
        }

        log.debug("Recorded performance for model: {} - {}", modelId, performance.status());
        return updated;
    }

    @Override
    public boolean isApprovedForProduction(String modelId) {
        return getById(modelId)
            .map(m -> m.status() == ModelRegistryEntry.ModelStatus.APPROVED ||
                      m.status() == ModelRegistryEntry.ModelStatus.DEPLOYED)
            .orElse(false);
    }

    @Override
    public List<ModelRegistryEntry> getModelsRequiringValidation() {
        Instant now = Instant.now();
        return models.values().stream()
            .filter(m -> m.validation() != null &&
                        m.validation().nextValidationDate() != null &&
                        m.validation().nextValidationDate().isBefore(now))
            .collect(Collectors.toList());
    }

    @Override
    public List<ModelRegistryEntry> getModelsWithPerformanceIssues() {
        return models.values().stream()
            .filter(m -> m.performance() != null &&
                        m.performance().status() != ModelRegistryEntry.Performance.PerformanceStatus.NOMINAL)
            .collect(Collectors.toList());
    }

    @Override
    public List<ModelAuditEntry> getAuditHistory(String modelId) {
        return auditLog.getOrDefault(modelId, List.of());
    }

    private String generateModelId(ModelRegistryEntry entry) {
        return String.format("%s-%s-%s",
            entry.name().toLowerCase().replaceAll("[^a-z0-9]", "-"),
            entry.version().replaceAll("[^a-zA-Z0-9]", "-"),
            UUID.randomUUID().toString().substring(0, 8)
        );
    }

    private void recordAudit(String modelId, String action, String previousState,
                            String newState, String changedBy, Map<String, Object> details) {
        ModelAuditEntry audit = new ModelAuditEntry(
            UUID.randomUUID().toString(),
            modelId,
            action,
            previousState,
            newState,
            changedBy,
            Instant.now(),
            details
        );

        auditLog.computeIfAbsent(modelId, k -> new CopyOnWriteArrayList<>()).add(audit);
    }

    /**
     * Get total number of registered models.
     */
    public int getModelCount() {
        return models.size();
    }

    /**
     * Get count by risk tier.
     */
    public Map<ModelRegistryEntry.RiskTier, Long> getCountByRiskTier() {
        return models.values().stream()
            .collect(Collectors.groupingBy(ModelRegistryEntry::riskTier, Collectors.counting()));
    }

    /**
     * Get count by status.
     */
    public Map<ModelRegistryEntry.ModelStatus, Long> getCountByStatus() {
        return models.values().stream()
            .collect(Collectors.groupingBy(ModelRegistryEntry::status, Collectors.counting()));
    }
}
