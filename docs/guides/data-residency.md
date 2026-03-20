# Data Residency Guide

UK financial services firms must ensure AI processing occurs in approved regions to comply with [UK GDPR](https://ico.org.uk/for-organisations/uk-gdpr-guidance-and-resources/), [FCA SYSC 13.9](https://www.handbook.fca.org.uk/handbook/SYSC/13/9.html) outsourcing requirements, and [PRA SS2/21](https://www.bankofengland.co.uk/prudential-regulation/publication/2021/march/outsourcing-and-third-party-risk-management-ss) third-party risk management expectations.

Regulus provides the `DataResidencyEnforcer` to automatically validate and block requests that would process regulated data in non-approved regions.

---

## Why Data Residency Matters for UK Financial Services

| Regulation | Requirement | Risk if Non-Compliant |
|------------|-------------|----------------------|
| [UK GDPR](https://ico.org.uk/for-organisations/uk-gdpr-guidance-and-resources/) | Personal data transfers outside UK/EEA require adequacy or safeguards | ICO enforcement, fines up to £17.5M or 4% turnover |
| [FCA SYSC 13.9](https://www.handbook.fca.org.uk/handbook/SYSC/13/9.html) | Outsourcing must not impair supervision or customer data access | FCA enforcement action |
| [PRA SS2/21](https://www.bankofengland.co.uk/prudential-regulation/publication/2021/march/outsourcing-and-third-party-risk-management-ss) | Critical functions must have geographic risk assessment | PRA supervisory action |
| [EBA Guidelines](https://www.eba.europa.eu/regulation-and-policy/internal-governance/guidelines-on-outsourcing-arrangements) | Cloud outsourcing requires data location controls | Regulatory findings |

---

## Configuration

### Basic Setup

```yaml
regulus:
  ai:
    safety:
      data-residency:
        enabled: true
        allowed-regions:
          - europe-west2      # GCP London
          - europe-west1      # GCP Belgium (EU adequacy)
          - eu-west-2         # AWS London
          - uksouth           # Azure UK South
          - ukwest            # Azure UK West
        enforce-uk-residency: true
        block-violations: true
        allow-unknown-regions: false
```

### Configuration Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `enabled` | boolean | `false` | Enable data residency enforcement |
| `allowed-regions` | List | `[europe-west2, europe-west1]` | Approved cloud regions |
| `enforce-uk-residency` | boolean | `true` | UK_REGULATED data must stay in UK |
| `block-violations` | boolean | `true` | Block requests to non-approved regions |
| `allow-unknown-regions` | boolean | `false` | Allow requests when region cannot be determined |

---

## Data Classification

The enforcer uses data classification to determine region restrictions:

| Classification | Description | Transfer Rules |
|----------------|-------------|----------------|
| `PUBLIC` | Publicly available data | No restrictions |
| `STANDARD` | Standard business data | Allowed regions only |
| `INTERNAL` | Internal operational data | Allowed regions only |
| `CONFIDENTIAL` | Confidential business data | Allowed regions only |
| `PII` | Personally Identifiable Information | UK/EEA only, external transfers require approval |
| `SENSITIVE` | Special category data (GDPR Article 9) | UK/EEA only, external transfers require approval |
| `UK_REGULATED` | FCA/PRA regulated data | UK only - no cross-border transfers |
| `CRITICAL` | Systemically important data | UK only - no cross-border transfers |

### Classification Examples

| Data Type | Recommended Classification | Rationale |
|-----------|---------------------------|-----------|
| Customer name, address | `PII` | Personal data under UK GDPR |
| National Insurance Number | `SENSITIVE` | Government identifier |
| Account balance | `CONFIDENTIAL` | Financial data |
| Transaction history | `UK_REGULATED` | FCA regulated activity |
| Credit score | `UK_REGULATED` | Consumer credit data |
| Health information | `SENSITIVE` | Special category (GDPR Art 9) |
| Marketing preferences | `STANDARD` | Business data |
| Public company info | `PUBLIC` | Publicly available |

---

## Java API

### Basic Usage

```java
@Autowired
private DataResidencyEnforcer dataResidencyEnforcer;

// Check if a request is allowed
ResidencyCheckRequest request = ResidencyCheckRequest.of(
    "customer-pii",      // data type
    "us-east-1"          // target region (AWS Virginia)
);

ResidencyCheckResult result = dataResidencyEnforcer.checkResidency(request);

if (!result.isAllowed()) {
    if (result.requiresApproval()) {
        // Route to approval workflow
        log.info("Transfer requires approval: {}", result.message());
    } else {
        // Block the request
        throw new DataResidencyViolationException(result.message());
    }
}
```

### Full Request with Source Region

```java
ResidencyCheckRequest request = new ResidencyCheckRequest(
    UUID.randomUUID().toString(),  // requestId
    "transaction-data",            // dataType
    "europe-west2",                // sourceRegion (GCP London)
    "us-central1",                 // targetRegion (GCP Iowa)
    "batch-processor@system",      // requestedBy
    Map.of("purpose", "analytics") // metadata
);

ResidencyCheckResult result = dataResidencyEnforcer.checkResidency(request);

// Result contains:
// - isAllowed: boolean
// - requiresApproval: boolean
// - message: String (reason if blocked)
// - violation: DataResidencyViolation (if logged)
```

### Checking Model/Endpoint Regions

```java
// Get allowed regions for a specific model
Set<String> allowedRegions = dataResidencyEnforcer.getAllowedRegionsForModel("gpt-4o");
// Returns: [europe-west2, eu-west-2, uksouth]

// Validate an endpoint URL
boolean allowed = dataResidencyEnforcer.isEndpointAllowed(
    "https://europe-west2-aiplatform.googleapis.com/v1/projects/..."
);
// Returns: true (GCP London is allowed)

boolean blocked = dataResidencyEnforcer.isEndpointAllowed(
    "https://us-central1-aiplatform.googleapis.com/v1/projects/..."
);
// Returns: false (US region not allowed)
```

### Retrieving Violations

```java
// Get all recorded violations
List<DataResidencyViolation> violations = dataResidencyEnforcer.getViolations();

for (DataResidencyViolation v : violations) {
    log.warn("Violation: {} attempted {} -> {} (allowed: {})",
        v.dataType(),
        v.attemptedRegion(),
        v.allowedRegions(),
        v.timestamp()
    );
}

// Violation record contains:
// - violationId: Unique identifier
// - requestId: Original request ID
// - dataType: Type of data
// - classification: Data classification level
// - attemptedRegion: Region that was attempted
// - allowedRegions: Set of allowed regions
// - requestedBy: Who made the request
// - timestamp: When violation occurred
```

---

## Cloud Region Mapping

### Google Cloud Platform (GCP)

| Region Code | Location | UK Approved | Notes |
|-------------|----------|-------------|-------|
| `europe-west2` | London | Yes | Primary UK region |
| `europe-west1` | Belgium | Yes | EU adequacy |
| `europe-west3` | Frankfurt | Yes | EU adequacy |
| `europe-west4` | Netherlands | Yes | EU adequacy |
| `us-central1` | Iowa | No | US - requires approval |
| `asia-east1` | Taiwan | No | APAC - requires approval |

### Amazon Web Services (AWS)

| Region Code | Location | UK Approved | Notes |
|-------------|----------|-------------|-------|
| `eu-west-2` | London | Yes | Primary UK region |
| `eu-west-1` | Ireland | Yes | EU adequacy |
| `eu-central-1` | Frankfurt | Yes | EU adequacy |
| `us-east-1` | Virginia | No | US - requires approval |
| `ap-southeast-1` | Singapore | No | APAC - requires approval |

### Microsoft Azure

| Region Code | Location | UK Approved | Notes |
|-------------|----------|-------------|-------|
| `uksouth` | London | Yes | Primary UK region |
| `ukwest` | Cardiff | Yes | UK secondary |
| `northeurope` | Ireland | Yes | EU adequacy |
| `westeurope` | Netherlands | Yes | EU adequacy |
| `eastus` | Virginia | No | US - requires approval |

---

## Transfer Restrictions

### Cross-Border Transfer Rules

```
┌─────────────────────────────────────────────────────────────────┐
│                    TRANSFER DECISION FLOW                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Request to process data in target region                       │
│       │                                                         │
│       ▼                                                         │
│  ┌─────────────────┐                                           │
│  │ Is target in    │──── Yes ────▶ ALLOWED                     │
│  │ allowed regions?│                                           │
│  └────────┬────────┘                                           │
│           │ No                                                  │
│           ▼                                                     │
│  ┌─────────────────┐                                           │
│  │ Is data         │──── No ─────▶ BLOCKED                     │
│  │ PUBLIC/STANDARD?│                                           │
│  └────────┬────────┘                                           │
│           │ Yes (for PII/SENSITIVE)                            │
│           ▼                                                     │
│  ┌─────────────────┐                                           │
│  │ Is transfer     │──── No ─────▶ BLOCKED                     │
│  │ UK/EEA → UK/EEA?│                                           │
│  └────────┬────────┘                                           │
│           │ No (external)                                       │
│           ▼                                                     │
│  ┌─────────────────┐                                           │
│  │ REQUIRES        │                                           │
│  │ APPROVAL        │                                           │
│  └─────────────────┘                                           │
│                                                                 │
│  UK_REGULATED / CRITICAL data: UK only, no exceptions           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Configuring Transfer Restrictions

```java
// Programmatic configuration
DataResidencyConfig config = new DataResidencyConfig();
config.setAllowedRegions(List.of("europe-west2", "eu-west-2", "uksouth"));
config.setEnforceUkResidency(true);
config.setBlockViolations(true);

// Set classification-specific region restrictions
Map<DataClassification, Set<String>> classificationRegions = new HashMap<>();
classificationRegions.put(DataClassification.UK_REGULATED, Set.of("europe-west2", "eu-west-2", "uksouth"));
classificationRegions.put(DataClassification.CRITICAL, Set.of("europe-west2", "eu-west-2", "uksouth"));
classificationRegions.put(DataClassification.PII, Set.of("europe-west2", "europe-west1", "eu-west-2", "uksouth"));
config.setClassificationRegions(classificationRegions);

// Set model-specific restrictions
Map<String, Set<String>> modelRegions = new HashMap<>();
modelRegions.put("gpt-4o", Set.of("europe-west2")); // GPT-4o only in UK
modelRegions.put("gemini-1.5-pro", Set.of("europe-west2", "europe-west1"));
config.setModelRegionRestrictions(modelRegions);

DataResidencyEnforcer enforcer = new DataResidencyEnforcer(config);
```

---

## Integration with LLM Providers

### Vertex AI (GCP)

```yaml
regulus:
  ai:
    llm:
      provider: gemini
      gemini:
        project-id: ${GCP_PROJECT_ID}
        location: europe-west2  # GCP London - UK data residency
        model: gemini-1.5-pro
```

### Azure OpenAI

```yaml
regulus:
  ai:
    llm:
      provider: azure-openai
      azure-openai:
        endpoint: https://my-openai.openai.azure.com/  # UK South deployment
        deployment-name: gpt-4o-uk
        api-key: ${AZURE_OPENAI_KEY}
```

### Anthropic (via AWS Bedrock in eu-west-2)

```yaml
regulus:
  ai:
    llm:
      provider: anthropic
      anthropic:
        # Use Bedrock endpoint in eu-west-2 for UK residency
        base-url: https://bedrock-runtime.eu-west-2.amazonaws.com
        model: claude-sonnet-4-20250514
```

---

## Compliance Evidence

### Generating Audit Reports

```java
// Get all violations for audit
List<DataResidencyViolation> violations = dataResidencyEnforcer.getViolations();

// Export for compliance reporting
violations.stream()
    .filter(v -> v.timestamp().isAfter(auditPeriodStart))
    .forEach(v -> {
        auditReport.addViolation(
            v.violationId(),
            v.dataType(),
            v.classification().name(),
            v.attemptedRegion(),
            v.allowedRegions().toString(),
            v.requestedBy(),
            v.timestamp()
        );
    });
```

### Metrics for Monitoring

The enforcer exposes Micrometer metrics:

```
regulus_data_residency_checks_total{result="allowed"} 15234
regulus_data_residency_checks_total{result="blocked"} 12
regulus_data_residency_checks_total{result="requires_approval"} 3
regulus_data_residency_violations_total{classification="PII"} 8
regulus_data_residency_violations_total{classification="UK_REGULATED"} 4
```

---

## Implementation Checklist

### Initial Setup
- [ ] Enable data residency in configuration
- [ ] Define allowed regions based on cloud provider contracts
- [ ] Configure UK-only enforcement for regulated data
- [ ] Set violation blocking policy

### Data Classification
- [ ] Map data types to classification levels
- [ ] Document classification rationale for audit
- [ ] Configure classification-specific region rules
- [ ] Train development teams on classification requirements

### LLM Provider Configuration
- [ ] Deploy LLM endpoints in approved regions only
- [ ] Configure model-specific region restrictions
- [ ] Validate endpoint URLs resolve to approved regions
- [ ] Document provider region commitments

### Monitoring & Compliance
- [ ] Set up violation alerting
- [ ] Configure audit log retention (7 years for FCA)
- [ ] Schedule quarterly compliance reviews
- [ ] Document exception/approval process

---

## Related Documentation

- [Safety Starter Configuration](./starters.md#regulus-ai-safety-starter)
- [Governance & Security](../governance/governance-security.md)
- [Risk Control Matrix](../governance/risk-control-matrix.md)
- [Kill Switch Design](../governance/kill-switch.md)
