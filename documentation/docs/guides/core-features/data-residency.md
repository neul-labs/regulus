# Data Residency

Enforce data location requirements for UK GDPR and FCA compliance.

## Overview

Data residency controls ensure that customer data is processed and stored only in approved geographic locations. For UK financial services, this typically means:

- **UK regions** - Primary choice for UK customer data
- **EEA regions** - Acceptable under adequacy decisions
- **Blocked regions** - US, APAC, etc. for regulated data

## Configuration

### Basic Configuration

```yaml title="application.yml"
regulus:
  data-residency:
    enabled: true
    default-region: uk
    enforcement: strict
```

### Full Configuration

```yaml title="application.yml"
regulus:
  data-residency:
    enabled: true
    enforcement: strict  # strict, warn, or disabled

    # Approved regions
    regions:
      uk:
        allowed: true
        providers:
          gcp: europe-west2        # London
          aws: eu-west-2           # London
          azure: uksouth, ukwest   # UK South, UK West

      eu:
        allowed: true
        adequacy: true  # Under UK GDPR adequacy
        providers:
          gcp: europe-west1, europe-west3, europe-west4
          aws: eu-west-1, eu-central-1
          azure: westeurope, northeurope

      us:
        allowed: false
        reason: "US does not have UK GDPR adequacy"

    # Classification-based rules
    classification:
      public:
        allowed-regions: [uk, eu]
      internal:
        allowed-regions: [uk, eu]
      confidential:
        allowed-regions: [uk]
      restricted:
        allowed-regions: [uk]

    # Audit
    audit:
      log-region-checks: true
      alert-on-violation: true
```

## Using Data Residency Controls

### Basic Validation

```java
@Service
public class AgentService {

    private final DataResidencyEnforcer residencyEnforcer;
    private final LlmClient llmClient;

    public Mono<String> process(String input, DataContext context) {
        return residencyEnforcer.validate(context)
            .then(llmClient.chat(input))
            .map(ChatResponse::content);
    }
}
```

### With Classification

```java
public Mono<String> processClassified(
        String input,
        DataClassification classification) {

    DataContext context = DataContext.builder()
        .classification(classification)
        .targetRegion(llmClient.getRegion())
        .build();

    return residencyEnforcer.validate(context)
        .onErrorMap(DataResidencyViolationException.class, e ->
            new ForbiddenException(
                "Cannot process " + classification + " data in region " +
                llmClient.getRegion()
            )
        )
        .then(llmClient.chat(input));
}
```

## LLM Provider Region Mapping

### Google Cloud Platform

| Region | Location | UK Approved |
|--------|----------|-------------|
| `europe-west2` | London | Yes |
| `europe-west1` | Belgium | Yes (EU adequacy) |
| `europe-west3` | Frankfurt | Yes (EU adequacy) |
| `us-central1` | Iowa | No |

### AWS

| Region | Location | UK Approved |
|--------|----------|-------------|
| `eu-west-2` | London | Yes |
| `eu-west-1` | Ireland | Yes (EU adequacy) |
| `eu-central-1` | Frankfurt | Yes (EU adequacy) |
| `us-east-1` | Virginia | No |

### Azure

| Region | Location | UK Approved |
|--------|----------|-------------|
| `uksouth` | London | Yes |
| `ukwest` | Cardiff | Yes |
| `westeurope` | Netherlands | Yes (EU adequacy) |
| `eastus` | Virginia | No |

## Provider-Specific Configuration

### Google Vertex AI

```yaml
regulus:
  llm:
    provider: gemini
    gemini:
      project-id: ${GOOGLE_CLOUD_PROJECT}
      location: europe-west2  # London - UK approved
```

### Azure OpenAI

```yaml
regulus:
  llm:
    provider: azure-openai
    azure-openai:
      endpoint: https://my-resource.uksouth.openai.azure.com
      # Region is embedded in endpoint
```

## Runtime Region Validation

### Validating Provider Regions

```java
@Component
public class ProviderRegionValidator {

    private final DataResidencyConfig config;

    public void validateProvider(LlmProvider provider) {
        String region = provider.getRegion();

        if (!config.isRegionAllowed(region)) {
            throw new DataResidencyViolationException(
                "Provider region " + region + " is not approved"
            );
        }
    }
}
```

### Request-Time Validation

```java
@Aspect
@Component
public class DataResidencyAspect {

    private final DataResidencyEnforcer enforcer;

    @Around("@annotation(RequiresUkResidency)")
    public Object enforceResidency(ProceedingJoinPoint pjp) throws Throwable {
        enforcer.validateCurrentContext();
        return pjp.proceed();
    }
}

// Usage
@RequiresUkResidency
public Mono<String> processUkCustomerData(String input) {
    return llmClient.chat(input);
}
```

## Data Classification

### Classification Levels

```java
public enum DataClassification {
    PUBLIC,       // No restrictions
    INTERNAL,     // UK or EU
    CONFIDENTIAL, // UK only
    RESTRICTED    // UK only, additional controls
}
```

### Classification-Based Routing

```java
@Service
public class ClassifiedDataRouter {

    private final Map<String, LlmClient> regionalClients;

    public Mono<String> route(String input, DataClassification classification) {
        String region = getRequiredRegion(classification);
        LlmClient client = regionalClients.get(region);

        if (client == null) {
            return Mono.error(new ConfigurationException(
                "No client configured for region: " + region
            ));
        }

        return client.chat(input);
    }

    private String getRequiredRegion(DataClassification classification) {
        return switch (classification) {
            case PUBLIC, INTERNAL -> "eu";  // Can use EU
            case CONFIDENTIAL, RESTRICTED -> "uk";  // UK only
        };
    }
}
```

## Monitoring

### Metrics

- `regulus.residency.checks.total` - Total region checks
- `regulus.residency.violations.total` - Violations by classification
- `regulus.residency.requests.by_region` - Request distribution

```promql
# Violation rate
rate(regulus_residency_violations_total[5m])

# Requests by region
sum by (region) (regulus_residency_requests_by_region)
```

### Alerting

```yaml
groups:
  - name: data-residency
    rules:
      - alert: DataResidencyViolation
        expr: increase(regulus_residency_violations_total[5m]) > 0
        labels:
          severity: critical
        annotations:
          summary: "Data residency violation detected"
```

## Audit Trail

```java
@Component
public class ResidencyAuditLogger {

    private final AuditLogger auditLogger;

    public void logCheck(DataContext context, boolean allowed) {
        auditLogger.log(AuditEvent.builder()
            .type("DATA_RESIDENCY_CHECK")
            .details(Map.of(
                "classification", context.classification(),
                "targetRegion", context.targetRegion(),
                "allowed", allowed
            ))
            .build());
    }

    public void logViolation(DataContext context, String reason) {
        auditLogger.log(AuditEvent.builder()
            .type("DATA_RESIDENCY_VIOLATION")
            .severity(Severity.HIGH)
            .details(Map.of(
                "classification", context.classification(),
                "targetRegion", context.targetRegion(),
                "reason", reason
            ))
            .build());
    }
}
```

## Testing

```java
@SpringBootTest
class DataResidencyTest {

    @Autowired
    private DataResidencyEnforcer enforcer;

    @Test
    void shouldAllowUkRegion() {
        DataContext context = DataContext.builder()
            .classification(DataClassification.CONFIDENTIAL)
            .targetRegion("europe-west2")
            .build();

        StepVerifier.create(enforcer.validate(context))
            .verifyComplete();
    }

    @Test
    void shouldBlockUsRegion() {
        DataContext context = DataContext.builder()
            .classification(DataClassification.CONFIDENTIAL)
            .targetRegion("us-central1")
            .build();

        StepVerifier.create(enforcer.validate(context))
            .expectError(DataResidencyViolationException.class)
            .verify();
    }

    @Test
    void shouldAllowEuForPublicData() {
        DataContext context = DataContext.builder()
            .classification(DataClassification.PUBLIC)
            .targetRegion("europe-west1")  // Belgium
            .build();

        StepVerifier.create(enforcer.validate(context))
            .verifyComplete();
    }
}
```

## Best Practices

1. **Default to UK** - Use UK regions as the default for all regulated data
2. **Classify data** - Implement clear data classification policies
3. **Validate at boundaries** - Check residency at API boundaries
4. **Audit everything** - Log all residency checks and violations
5. **Review adequacy** - Monitor UK GDPR adequacy decisions for changes
6. **Test failover** - Ensure failover doesn't route to non-compliant regions
