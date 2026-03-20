# SS1/23 Model Registry

Model risk management compliance for PRA SS1/23.

## Overview

PRA SS1/23 requires firms to maintain a comprehensive inventory of all models, including AI/ML models. Regulus provides built-in model registry support to help meet these requirements.

## Requirements Summary

Key SS1/23 requirements addressed:

| Requirement | Regulus Feature |
|------------|-----------------|
| Model inventory | Model Registry |
| Risk classification | Risk Tiering |
| Documentation | Model Cards |
| Validation | Validation Reports |
| Review cadence | Scheduled Reviews |
| Approval workflows | Integration with GRC |

## Configuration

### Basic Setup

```yaml title="application.yml"
regulus:
  model-registry:
    enabled: true
    model-id: customer-support-agent-v1
    risk-tier: MEDIUM
    owner: ai-platform-team
    review-cadence: QUARTERLY
```

### Full Configuration

```yaml title="application.yml"
regulus:
  model-registry:
    enabled: true

    # Model identification
    model-id: customer-support-agent-v1
    model-name: Customer Support Agent
    model-version: "1.0.0"
    model-type: LLM_AGENT

    # Risk classification
    risk-tier: MEDIUM  # LOW, MEDIUM, HIGH, CRITICAL
    materiality: SIGNIFICANT

    # Ownership
    owner: ai-platform-team
    business-owner: customer-service-director
    technical-owner: ai-engineering-lead

    # Review schedule
    review-cadence: QUARTERLY  # MONTHLY, QUARTERLY, SEMI_ANNUAL, ANNUAL
    last-review: "2024-01-15"
    next-review: "2024-04-15"

    # Approval
    approval-required: true
    approvers:
      - model-risk-team
      - compliance-team

    # External integration
    external-registry:
      enabled: true
      type: servicenow  # servicenow, archer, or custom
      sync-interval: 1h
```

## Risk Tiering

### Risk Tier Definitions

| Tier | Criteria | Review Cadence |
|------|----------|----------------|
| **LOW** | Limited business impact, no customer-facing decisions | Annual |
| **MEDIUM** | Moderate impact, supports but doesn't make decisions | Quarterly |
| **HIGH** | Significant impact, influences customer outcomes | Monthly |
| **CRITICAL** | Material impact, direct customer decisions | Monthly + continuous monitoring |

### Determining Risk Tier

```java
public RiskTier assessRiskTier(ModelMetadata model) {
    int score = 0;

    // Customer impact
    if (model.isCustomerFacing()) score += 2;
    if (model.makesDecisions()) score += 3;

    // Financial impact
    if (model.getFinancialExposure().compareTo(THRESHOLD) > 0) score += 2;

    // Data sensitivity
    if (model.processesPersonalData()) score += 1;
    if (model.processesSpecialCategory()) score += 2;

    // Complexity
    if (model.isBlackBox()) score += 1;

    return switch (score) {
        case 0, 1, 2 -> RiskTier.LOW;
        case 3, 4, 5 -> RiskTier.MEDIUM;
        case 6, 7 -> RiskTier.HIGH;
        default -> RiskTier.CRITICAL;
    };
}
```

## Model Cards

### Generating Model Cards

```java
@Service
public class ModelCardService {

    private final ModelRegistry registry;
    private final ModelCardGenerator generator;

    public ModelCard generateCard(String modelId) {
        ModelMetadata metadata = registry.getModel(modelId);

        return generator.generate(ModelCardRequest.builder()
            .modelId(modelId)
            .includePerformanceMetrics(true)
            .includeLimitations(true)
            .includeEthicalConsiderations(true)
            .build());
    }
}
```

### Model Card Contents

```java
public record ModelCard(
    // Identification
    String modelId,
    String modelName,
    String version,
    LocalDate effectiveDate,

    // Purpose
    String intendedUse,
    String intendedUsers,
    List<String> outOfScopeUses,

    // Technical Details
    String modelType,
    String underlyingModel,  // e.g., "gemini-2.0-flash"
    String architecture,

    // Training & Data
    String trainingData,
    String dataPreprocessing,

    // Performance
    Map<String, Double> performanceMetrics,
    List<String> evaluationDatasets,

    // Limitations
    List<String> knownLimitations,
    List<String> biasConsiderations,

    // Ethical Considerations
    String ethicalReview,
    List<String> mitigations,

    // Governance
    String owner,
    RiskTier riskTier,
    LocalDate lastReview,
    LocalDate nextReview,
    List<String> approvers
) {}
```

### Example Model Card

```markdown
# Model Card: Customer Support Agent v1.0

## Model Details
- **Model ID**: customer-support-agent-v1
- **Version**: 1.0.0
- **Type**: LLM Agent
- **Underlying Model**: Gemini 2.0 Flash
- **Owner**: AI Platform Team

## Intended Use
- **Primary Use**: Answering customer support queries
- **Intended Users**: Bank customers via chat interface
- **Out of Scope**: Financial advice, investment recommendations

## Risk Classification
- **Risk Tier**: MEDIUM
- **Materiality**: Significant
- **Review Cadence**: Quarterly

## Performance
- **Accuracy**: 94.2% on test set
- **Response Quality**: 4.3/5 human evaluation
- **Latency P99**: 2.1s

## Limitations
- Cannot access real-time account data
- May not understand complex financial products
- Limited to English language

## Governance
- **Last Review**: 2024-01-15
- **Next Review**: 2024-04-15
- **Approvers**: Model Risk, Compliance
```

## Validation Reports

### Generating Validation Reports

```java
@Service
public class ValidationReportService {

    private final ModelValidator validator;

    public ValidationReport generateReport(String modelId) {
        return validator.validate(ValidationRequest.builder()
            .modelId(modelId)
            .includePerformanceTests(true)
            .includeBiasTests(true)
            .includeRobustnessTests(true)
            .includeSecurityTests(true)
            .build());
    }
}
```

### Validation Report Contents

```java
public record ValidationReport(
    String modelId,
    LocalDateTime validationDate,
    String validatorId,

    // Performance validation
    PerformanceValidation performance,

    // Bias and fairness
    BiasValidation bias,

    // Robustness
    RobustnessValidation robustness,

    // Security
    SecurityValidation security,

    // Overall assessment
    ValidationStatus overallStatus,
    List<Finding> findings,
    List<Recommendation> recommendations
) {}
```

## Approval Workflows

### Integration with ServiceNow

```java
@Service
public class ApprovalWorkflowService {

    private final ServiceNowClient serviceNow;
    private final ModelRegistry registry;

    public Mono<String> requestApproval(String modelId) {
        ModelMetadata model = registry.getModel(modelId);

        return serviceNow.createApprovalRequest(
            ApprovalRequest.builder()
                .type("AI_MODEL_APPROVAL")
                .modelId(modelId)
                .modelName(model.name())
                .riskTier(model.riskTier())
                .requestedBy(SecurityContextHolder.getContext()
                    .getAuthentication().getName())
                .approverGroups(getApproverGroups(model.riskTier()))
                .attachments(List.of(
                    generateModelCard(modelId),
                    generateValidationReport(modelId)
                ))
                .build()
        ).map(ApprovalResponse::requestId);
    }

    private List<String> getApproverGroups(RiskTier tier) {
        return switch (tier) {
            case LOW -> List.of("model-risk-team");
            case MEDIUM -> List.of("model-risk-team", "compliance-team");
            case HIGH, CRITICAL -> List.of(
                "model-risk-team",
                "compliance-team",
                "senior-management"
            );
        };
    }
}
```

## Registry Synchronization

### Syncing with External Registry

```java
@Scheduled(fixedRateString = "${regulus.model-registry.external-registry.sync-interval}")
public void syncWithExternalRegistry() {
    List<ModelMetadata> models = modelRegistry.getAllModels();

    models.forEach(model -> {
        externalRegistry.upsert(ExternalModelRecord.builder()
            .modelId(model.id())
            .modelName(model.name())
            .riskTier(model.riskTier())
            .owner(model.owner())
            .lastReview(model.lastReview())
            .nextReview(model.nextReview())
            .status(model.status())
            .build());
    });

    log.info("Synced {} models with external registry", models.size());
}
```

## Audit Trail

All registry operations are logged:

```java
@Aspect
@Component
public class ModelRegistryAuditAspect {

    @AfterReturning("execution(* com.regulus.registry.ModelRegistry.*(..))")
    public void auditRegistryOperation(JoinPoint jp) {
        String operation = jp.getSignature().getName();
        Object[] args = jp.getArgs();

        auditLogger.log(AuditEvent.builder()
            .type("MODEL_REGISTRY_OPERATION")
            .operation(operation)
            .details(Map.of(
                "arguments", Arrays.toString(args),
                "user", getCurrentUser()
            ))
            .build());
    }
}
```

## Metrics

Available metrics:

- `regulus.registry.models.total` - Total registered models
- `regulus.registry.models.by_tier` - Models by risk tier
- `regulus.registry.reviews.overdue` - Models with overdue reviews

```promql
# Models by risk tier
regulus_registry_models_by_tier

# Overdue reviews alert
regulus_registry_reviews_overdue > 0
```

## Best Practices

1. **Register early** - Register models before development starts
2. **Update regularly** - Keep registry current with model changes
3. **Document thoroughly** - Complete model cards for all models
4. **Review on schedule** - Never skip scheduled reviews
5. **Integrate with GRC** - Sync with enterprise GRC systems
6. **Audit access** - Log all registry operations
