# Model Registry (SS1/23 Compliance)

The Model Registry provides [PRA SS1/23](https://www.bankofengland.co.uk/prudential-regulation/publication/2023/may/model-risk-management-principles-for-banks-ss) compliant model inventory management for AI agents and models deployed on the Regulus platform.

---

## Why Model Registry Matters

PRA SS1/23 (Model Risk Management Principles for Banks) requires firms to:

> "Maintain a comprehensive inventory of all models in use, under development, or recently retired."

The Model Registry ensures:
- All AI agents are registered with ownership and risk classification
- Validation status and review cadences are tracked
- Performance metrics are monitored for drift detection
- Full audit trail for regulatory examinations

---

## Risk Tiering

SS1/23 requires risk-based materiality assessment. Regulus implements four tiers:

| Tier | Description | Validation Frequency | Review Cadence |
|------|-------------|---------------------|----------------|
| **TIER_1** | High materiality - direct customer impact | Pre-deployment + quarterly | Quarterly |
| **TIER_2** | Medium materiality - indirect customer impact | Pre-deployment + semi-annual | Semi-annual |
| **TIER_3** | Low materiality - internal operations | Pre-deployment + annual | Annual |
| **TIER_4** | Minimal risk - monitoring only | Annual | Annual |

### Tier Classification Criteria

| Factor | TIER_1 | TIER_2 | TIER_3 | TIER_4 |
|--------|--------|--------|--------|--------|
| Customer impact | Direct financial advice | Indirect support | Internal only | None |
| Decision automation | Fully automated | Semi-automated | Recommendations | Informational |
| Regulatory scope | FCA regulated | Compliance support | Operational | Administrative |
| Data sensitivity | PII/Financial | Confidential | Internal | Public |

---

## Configuration

### Enable Model Registry

```yaml
regulus:
  ai:
    governance:
      enabled: true
      model-registry:
        enabled: true
        sync-to-inventory: true
        inventory-endpoint: ${MODEL_INVENTORY_URL}
        validation:
          require-pre-deployment: true
          require-challenger: true
```

### Annotate Agents

```java
@Agent(name = "mortgage-adviser")
@ModelArtefact(
    owner = "Lending Team",
    riskTier = "TIER_2",
    intendedUse = "Customer-facing mortgage affordability assessments",
    reviewCadence = "QUARTERLY",
    dataClassification = "PII",
    regulatoryScope = "FCA_MCOB"
)
public class MortgageAdviserAgent {
    // ...
}
```

---

## Java API

### Registering Models

```java
@Autowired
private ModelRegistry modelRegistry;

// Create a registry entry
ModelRegistryEntry entry = ModelRegistryEntry.builder()
    .modelId("mortgage-adviser-v1.2.0")
    .name("Mortgage Affordability Adviser")
    .version("1.2.0")
    .modelType(ModelType.AI_AGENT)
    .riskTier(RiskTier.TIER_2)
    .owner("Lending Team")
    .ownerEmail("lending-ai@bank.com")
    .intendedUse("Customer-facing mortgage affordability assessments")
    .dataClassification(DataClassification.PII)
    .regulatoryScope("FCA_MCOB")
    .deploymentStatus(DeploymentStatus.DEVELOPMENT)
    .build();

modelRegistry.register(entry);
```

### Querying the Registry

```java
// Find by ID
Optional<ModelRegistryEntry> model = modelRegistry.findById("mortgage-adviser-v1.2.0");

// Find by risk tier
List<ModelRegistryEntry> tier1Models = modelRegistry.findByRiskTier(RiskTier.TIER_1);

// Find by owner
List<ModelRegistryEntry> teamModels = modelRegistry.findByOwner("Lending Team");

// Find models due for review
List<ModelRegistryEntry> dueForReview = modelRegistry.findDueForReview();
```

### Validation Workflow

```java
// Record validation
modelRegistry.recordValidation("mortgage-adviser-v1.2.0", ValidationRecord.builder()
    .validationType(ValidationType.INDEPENDENT)
    .validatedBy("Model Risk Team")
    .validatedAt(Instant.now())
    .outcome(ValidationOutcome.APPROVED)
    .findings(List.of())
    .nextReviewDate(LocalDate.now().plusMonths(6))
    .build()
);

// Check validation status
ValidationStatus status = modelRegistry.getValidationStatus("mortgage-adviser-v1.2.0");
if (status.requiresRevalidation()) {
    // Trigger revalidation workflow
}
```

### Performance Monitoring

```java
// Record performance metrics
modelRegistry.recordPerformance("mortgage-adviser-v1.2.0", PerformanceRecord.builder()
    .recordedAt(Instant.now())
    .metric("accuracy", 0.94)
    .metric("latency_p99_ms", 245)
    .metric("error_rate", 0.002)
    .metric("drift_score", 0.05)
    .build()
);

// Get performance history
List<PerformanceRecord> history = modelRegistry.getPerformanceHistory(
    "mortgage-adviser-v1.2.0",
    LocalDate.now().minusMonths(3),
    LocalDate.now()
);

// Check for drift alerts
if (modelRegistry.isDriftDetected("mortgage-adviser-v1.2.0")) {
    // Trigger drift investigation
}
```

### Lifecycle Management

```java
// Update deployment status
modelRegistry.updateStatus("mortgage-adviser-v1.2.0", DeploymentStatus.PRODUCTION);

// Retire a model
modelRegistry.retire("mortgage-adviser-v1.0.0", RetirementRecord.builder()
    .retiredAt(Instant.now())
    .retiredBy("Model Risk Team")
    .reason("Superseded by v1.2.0")
    .replacementModelId("mortgage-adviser-v1.2.0")
    .dataRetentionPolicy("7_YEARS")
    .build()
);
```

---

## Model Types

| Type | Description | Example |
|------|-------------|---------|
| `AI_AGENT` | Autonomous AI agent | Mortgage Adviser |
| `LLM` | Large Language Model | GPT-4o, Gemini |
| `CLASSIFIER` | Classification model | Fraud detector |
| `REGRESSION` | Regression model | Credit scorer |
| `EMBEDDING` | Embedding model | Text embeddings |
| `ENSEMBLE` | Combined models | Multi-model pipeline |

---

## Deployment Statuses

| Status | Description | Allowed Operations |
|--------|-------------|-------------------|
| `DEVELOPMENT` | Under development | Testing only |
| `VALIDATION` | Awaiting validation | Limited testing |
| `STAGING` | Pre-production testing | Shadow mode |
| `PRODUCTION` | Live in production | Full access |
| `DEPRECATED` | Scheduled for retirement | Read-only |
| `RETIRED` | No longer in use | Audit access only |

### Status Transitions

```
DEVELOPMENT → VALIDATION → STAGING → PRODUCTION → DEPRECATED → RETIRED
                  ↓             ↓
              DEVELOPMENT   DEVELOPMENT (if issues found)
```

---

## Audit Trail

Every registry action is recorded for compliance:

```java
// Get audit trail for a model
List<AuditEntry> audit = modelRegistry.getAuditTrail("mortgage-adviser-v1.2.0");

// Audit entry contains:
// - timestamp: When the action occurred
// - action: REGISTERED | UPDATED | VALIDATED | STATUS_CHANGED | RETIRED
// - actor: Who performed the action
// - details: What changed
// - correlationId: Link to related systems
```

### Sample Audit Export

```json
{
  "modelId": "mortgage-adviser-v1.2.0",
  "auditTrail": [
    {
      "timestamp": "2025-01-10T09:00:00Z",
      "action": "REGISTERED",
      "actor": "developer@bank.com",
      "details": "Initial registration"
    },
    {
      "timestamp": "2025-01-12T14:30:00Z",
      "action": "VALIDATED",
      "actor": "model-risk@bank.com",
      "details": "Independent validation completed - APPROVED"
    },
    {
      "timestamp": "2025-01-15T10:00:00Z",
      "action": "STATUS_CHANGED",
      "actor": "release-manager@bank.com",
      "details": "STAGING → PRODUCTION"
    }
  ]
}
```

---

## GRC Integration

### ServiceNow Integration

```yaml
regulus:
  ai:
    governance:
      model-registry:
        grc-integration:
          provider: servicenow
          instance: ${SNOW_INSTANCE}
          table: u_model_inventory
          sync-schedule: "0 0 * * *"  # Daily sync
```

### Archer Integration

```yaml
regulus:
  ai:
    governance:
      model-registry:
        grc-integration:
          provider: archer
          api-url: ${ARCHER_API_URL}
          solution-id: model-risk-management
```

---

## Artefact Generation

The registry can generate SS1/23 required artefacts:

### Model Card

```java
ModelCard card = modelRegistry.generateModelCard("mortgage-adviser-v1.2.0");
// Contains: purpose, limitations, performance metrics, fairness assessment
```

### Validation Report

```java
ValidationReport report = modelRegistry.generateValidationReport("mortgage-adviser-v1.2.0");
// Contains: validation scope, methodology, findings, recommendations
```

### Change History

```java
ChangeHistory history = modelRegistry.generateChangeHistory("mortgage-adviser-v1.2.0");
// Contains: all changes with rationale and approvals
```

---

## Implementation Checklist

### Initial Setup
- [ ] Enable model registry in configuration
- [ ] Configure GRC integration endpoint
- [ ] Define risk tier criteria for your organisation
- [ ] Set up review cadence reminders

### Model Registration
- [ ] Add `@ModelArtefact` annotation to all agents
- [ ] Assign appropriate risk tier
- [ ] Document intended use and limitations
- [ ] Identify data classification

### Validation Process
- [ ] Establish independent validation team
- [ ] Define validation criteria per tier
- [ ] Create challenger model process
- [ ] Configure automated validation gates

### Monitoring
- [ ] Set up performance metric collection
- [ ] Configure drift detection thresholds
- [ ] Create alerting for review due dates
- [ ] Schedule quarterly governance reviews

---

## Regulatory Alignment

| SS1/23 Principle | Regulus Control |
|------------------|-----------------|
| Model inventory | Comprehensive registry with search |
| Risk classification | Four-tier system with criteria |
| Validation framework | Pre-deployment and periodic validation |
| Performance monitoring | Continuous metrics with drift detection |
| Change management | Full audit trail with approvals |
| Model lifecycle | Status tracking from dev to retirement |
| Documentation | Auto-generated model cards and reports |

---

## Related Documentation

- [Governance & Security](./governance-security.md)
- [Risk Control Matrix](./risk-control-matrix.md)
- [Kill Switch Design](./kill-switch.md)
- [Developer Checklist](../guides/developer-checklist.md)
