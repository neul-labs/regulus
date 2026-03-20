# ADR-002: Data Residency Enforcement at Platform Level

## Status
Accepted

## Date
2025-01-15

## Context

UK financial services firms using cloud-based LLM providers face strict data residency requirements:

1. **UK GDPR (Articles 44-49)**: Personal data transfers outside UK/EEA require adequacy decisions or appropriate safeguards
2. **FCA SYSC 13.9**: Firms must ensure effective access to data and appropriate supervision of outsourced functions
3. **PRA SS2/21**: Third-party risk management requires understanding where data is processed
4. **Consumer expectations**: UK customers expect their financial data to remain within UK jurisdiction

Cloud LLM providers operate in multiple regions globally. Without enforcement:
- Developers might inadvertently configure US or APAC endpoints
- Load balancing could route requests to non-UK regions
- Failover mechanisms might direct traffic outside approved jurisdictions
- API endpoint URLs don't always clearly indicate processing region

The platform needs to prevent data from leaving approved jurisdictions regardless of developer configuration or provider behaviour.

## Decision

We will implement **platform-level data residency enforcement** with the following characteristics:

### Core Design

1. **Allowlist-based regions**: Only explicitly approved regions are permitted
2. **Endpoint validation**: Every LLM request URL is validated before execution
3. **Data classification**: Different data types have different residency requirements
4. **Block by default**: Unknown regions are blocked unless explicitly allowed
5. **Audit trail**: All residency checks logged for compliance evidence

### Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     DataResidencyEnforcer                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Request ──▶ ┌──────────────────┐    ┌───────────────────┐     │
│              │ Data Classifier  │───▶│ Region Extractor  │     │
│              └──────────────────┘    └─────────┬─────────┘     │
│                                                │                │
│                                                ▼                │
│                                    ┌───────────────────────┐   │
│                                    │   Allowlist Checker   │   │
│                                    │                       │   │
│                                    │  UK_REGULATED → UK    │   │
│                                    │  PII → UK/EEA         │   │
│                                    │  INTERNAL → UK/EEA/US │   │
│                                    └───────────┬───────────┘   │
│                                                │                │
│                              ┌─────────────────┼──────────────┐│
│                              ▼                 ▼              ▼││
│                         ┌────────┐       ┌─────────┐    ┌─────┐│
│                         │ ALLOW  │       │  BLOCK  │    │AUDIT││
│                         └────────┘       └─────────┘    └─────┘│
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Data Classification Levels

| Level | Description | Allowed Regions |
|-------|-------------|-----------------|
| `PUBLIC` | Non-sensitive, published data | Any |
| `INTERNAL` | Internal business data | UK, EEA, US (with DPF) |
| `PII` | Personal identifiable information | UK, EEA |
| `SENSITIVE` | Special category data (Art 9) | UK, EEA |
| `UK_REGULATED` | FCA/PRA regulated data | UK only |
| `CRITICAL` | Systemically important | UK only, specific providers |

### Cloud Region Mapping

```yaml
# Approved UK regions by provider
allowed-regions:
  gcp:
    - europe-west2      # London
  aws:
    - eu-west-2         # London
  azure:
    - uksouth           # London
    - ukwest            # Cardiff

# Region extraction patterns
endpoint-patterns:
  gcp: "https://([a-z]+-[a-z]+\\d+)-aiplatform.googleapis.com"
  aws: "https://bedrock-runtime\\.([a-z]+-[a-z]+-\\d+)\\.amazonaws.com"
  azure: "https://([a-z]+)\\.openai.azure.com"
```

### Enforcement Points

1. **LLM Client Interceptor**: Validates endpoint before every LLM call
2. **Configuration Validation**: Rejects invalid region configuration at startup
3. **Runtime Monitoring**: Alerts on attempted violations
4. **Audit Logging**: Records all checks for compliance evidence

## Consequences

### Positive

1. **Regulatory compliance**: Automatic enforcement of UK GDPR and FCA requirements
2. **Developer safety**: Impossible to accidentally send data to wrong region
3. **Audit evidence**: Complete trail of data residency compliance
4. **Fail-secure**: Unknown regions blocked by default
5. **Flexibility**: Different rules for different data classifications

### Negative

1. **Reduced flexibility**: Cannot use providers without UK presence
2. **Performance impact**: Every request requires validation check
3. **Configuration complexity**: Must maintain accurate region mappings
4. **Provider dependency**: Relies on providers maintaining stable endpoint patterns

### Mitigations

| Concern | Mitigation |
|---------|------------|
| Provider availability | Support multiple providers with UK regions |
| Performance overhead | Cache region lookups, validate at configuration time |
| Pattern changes | Version endpoint patterns, monitor for changes |
| New providers | Extensible pattern registry |

## Alternatives Considered

### 1. Trust Provider Configuration

**Pros**: Simpler, no validation overhead
**Cons**: Relies on correct developer configuration, no protection against misconfiguration
**Decision**: Rejected - insufficient assurance for regulated environment

### 2. Network-Level Enforcement

**Pros**: Cannot be bypassed at application level
**Cons**: Requires network infrastructure changes, less granular control
**Decision**: Rejected as primary control - may be used as defense-in-depth

### 3. Per-Request User Consent

**Pros**: Maximum flexibility, user choice
**Cons**: Poor UX, users unlikely to understand implications
**Decision**: Rejected - inappropriate for automated agent processing

### 4. Data Tokenization/Anonymization

**Pros**: Allows any region if data is anonymized
**Cons**: Complex, may not meet regulatory requirements for certain data types
**Decision**: Complementary - can be used alongside residency enforcement

## References

- [UK GDPR - International Transfers](https://ico.org.uk/for-organisations/uk-gdpr-guidance-and-resources/international-transfers/)
- [FCA SYSC 13.9 - Outsourcing](https://www.handbook.fca.org.uk/handbook/SYSC/13/9.html)
- [PRA SS2/21 - Outsourcing](https://www.bankofengland.co.uk/prudential-regulation/publication/2021/march/outsourcing-and-third-party-risk-management-ss)
- [Data Residency Guide](../guides/data-residency.md)
- [Regulatory Reference](../references/regulatory-reference.md)
