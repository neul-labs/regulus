# UK Financial Services Regulatory Reference

Complete reference guide to regulations applicable to AI agents in UK financial services, with direct links to source documents and Regulus control mappings.

---

## Quick Reference Table

| Regulation | Authority | Effective Date | Regulus Controls |
|------------|-----------|----------------|------------------|
| [SS1/23](#pra-ss123-model-risk-management) | PRA | May 2024 | Model Registry, Validation Gates |
| [PS21/3](#pra-ps213-operational-resilience) | PRA | March 2025 | Kill Switch, Circuit Breakers |
| [SS2/21](#pra-ss221-outsourcing) | PRA | March 2022 | Vendor Registry, Data Residency |
| [Consumer Duty](#fca-consumer-duty) | FCA | July 2023 | Suitability Checks, Explainability |
| [SYSC 13.9](#fca-sysc-139-outsourcing) | FCA | Ongoing | Data Residency, Vendor Due Diligence |
| [UK GDPR](#uk-gdpr) | ICO | January 2021 | PII Redaction, Data Residency |
| [DORA](#dora) | EBA | January 2025 | Resilience Testing, ICT Risk |

---

## PRA SS1/23: Model Risk Management

### Source Document
**[Model risk management principles for banks](https://www.bankofengland.co.uk/prudential-regulation/publication/2023/may/model-risk-management-principles-for-banks-ss)**

Published: May 2023 | Effective: May 2024

### Key Requirements

| Principle | Requirement | Regulus Control |
|-----------|-------------|-----------------|
| **MRM1** | Comprehensive model inventory | `ModelRegistry` with `@ModelArtefact` |
| **MRM2** | Risk-based classification | `RiskTier` enum (TIER_1 to TIER_4) |
| **MRM3** | Independent validation | Validation workflow with challenger |
| **MRM4** | Ongoing monitoring | Performance tracking, drift detection |
| **MRM5** | Change management | Full audit trail |
| **MRM6** | Model lifecycle | Deployment status tracking |

### Regulus Implementation

```java
@Agent(name = "mortgage-adviser")
@ModelArtefact(
    owner = "Lending Team",
    riskTier = "TIER_2",                    // MRM2
    intendedUse = "Mortgage affordability",  // MRM1
    reviewCadence = "QUARTERLY"              // MRM4
)
public class MortgageAdviserAgent { }
```

### Evidence Requirements

| Artefact | Purpose | Regulus Source |
|----------|---------|----------------|
| Model inventory extract | MRM1 compliance | `ModelRegistry.exportInventory()` |
| Risk tier assessment | MRM2 classification | `ModelRegistryEntry.riskTier` |
| Validation report | MRM3 sign-off | `ModelRegistry.generateValidationReport()` |
| Performance metrics | MRM4 monitoring | `ModelRegistry.getPerformanceHistory()` |
| Change log | MRM5 audit | `ModelRegistry.getAuditTrail()` |

### Related Documentation
- [Model Registry Guide](../governance/model-registry.md)
- [Risk Control Matrix](../governance/risk-control-matrix.md)

---

## PRA PS21/3: Operational Resilience

### Source Document
**[Operational resilience: Impact tolerances for important business services](https://www.bankofengland.co.uk/prudential-regulation/publication/2021/march/operational-resilience-impact-tolerances-for-important-business-services)**

Published: March 2021 | Effective: March 2022 (full compliance March 2025)

### Key Requirements

| Requirement | Description | Regulus Control |
|-------------|-------------|-----------------|
| **IBS Identification** | Identify important business services | Agent classification |
| **Impact Tolerances** | Set maximum tolerable disruption | RTO/RPO configuration |
| **Mapping** | Map resources supporting IBS | Dependency documentation |
| **Scenario Testing** | Test ability to remain within tolerances | Risk Simulation module |
| **Self-Assessment** | Annual board-level assessment | Governance dashboards |

### Regulus Implementation

```yaml
regulus:
  ai:
    safety:
      kill-switch:
        enabled: true
        dual-control:
          enabled: true           # Prevent single point of failure
          required-approvers: 2
    resilience:
      circuit-breaker:
        enabled: true
        failure-rate-threshold: 50
        wait-duration-in-open-state: 60s
```

### Evidence Requirements

| Artefact | Purpose | Regulus Source |
|----------|---------|----------------|
| Kill switch activation logs | Demonstrate control capability | `DualControlKillSwitch.getAuditLog()` |
| DR test reports | Scenario testing evidence | Risk Simulation output |
| Recovery time metrics | Impact tolerance validation | OTEL metrics |
| Dependency maps | Resource mapping | Agent configuration |

### Related Documentation
- [Kill Switch Design](../governance/kill-switch.md)
- [Risk Simulation](../governance/risk-simulation.md)

---

## PRA SS2/21: Outsourcing and Third Party Risk

### Source Document
**[Outsourcing and third party risk management](https://www.bankofengland.co.uk/prudential-regulation/publication/2021/march/outsourcing-and-third-party-risk-management-ss)**

Published: March 2021 | Effective: March 2022

### Key Requirements

| Requirement | Description | Regulus Control |
|-------------|-------------|-----------------|
| **Due Diligence** | Assess third parties before engagement | Vendor Registry |
| **Contracts** | Appropriate contractual arrangements | Contract tracking |
| **Concentration Risk** | Monitor concentration | Vendor dashboard |
| **Exit Strategy** | Plan for termination | Exit test documentation |
| **Sub-outsourcing** | Manage sub-contractors | Subprocessor tracking |

### Regulus Implementation

```yaml
regulus:
  ai:
    safety:
      data-residency:
        enabled: true
        allowed-regions:
          - europe-west2    # GCP London
          - eu-west-2       # AWS London
          - uksouth         # Azure UK South
        enforce-uk-residency: true
```

### LLM Provider Assessment

| Provider | UK Region | Data Processing | Exit Strategy |
|----------|-----------|-----------------|---------------|
| Google Vertex AI | europe-west2 (London) | EU Model Garden | Multi-provider |
| Azure OpenAI | uksouth | UK data boundary | Anthropic fallback |
| Anthropic (Bedrock) | eu-west-2 | AWS UK region | Gemini fallback |
| AWS Bedrock | eu-west-2 | UK data boundary | Multi-provider |

### Related Documentation
- [Data Residency Guide](../guides/data-residency.md)
- [Implementation Playbooks](./implementation-playbooks.md)

---

## FCA Consumer Duty

### Source Document
**[Consumer Duty](https://www.fca.org.uk/firms/consumer-duty)**

- [FG22/5 Final Guidance](https://www.fca.org.uk/publication/finalised-guidance/fg22-5.pdf)
- [PS22/9 Policy Statement](https://www.fca.org.uk/publication/policy/ps22-9.pdf)

Published: July 2022 | Effective: July 2023 (new products), July 2024 (closed products)

### The Four Outcomes

| Outcome | FCA Expectation | Regulus Control |
|---------|-----------------|-----------------|
| **Products & Services** | Products designed to meet target market needs | Suitability checks, eligibility validation |
| **Price & Value** | Fair value for customers | Cost transparency, fee audit trail |
| **Consumer Understanding** | Clear communications | Plain language validation, explainability |
| **Consumer Support** | Support throughout lifecycle | Escalation paths, kill switch |

### Cross-Cutting Rules

| Rule | Description | Regulus Control |
|------|-------------|-----------------|
| **Acting in Good Faith** | Act honestly and fairly | Policy guards, audit trail |
| **Avoiding Foreseeable Harm** | Prevent harm to customers | Kill switch, safety filters |
| **Enabling/Supporting** | Help achieve financial objectives | Suitability assessment |

### Regulus Implementation

```yaml
regulus:
  ai:
    governance:
      consumer-duty:
        enabled: true
        # Products & Services
        suitability-check-required: true
        target-market-validation: true
        # Consumer Understanding
        explanation-detail-level: HIGH
        plain-language-validation: true
        # Consumer Support
        vulnerable-customer-detection: true
        escalation-enabled: true
```

```java
@Tool(name = "recommend_product")
@RequireSuitabilityCheck           // Products & Services
@RequireExplanation(level = HIGH)  // Consumer Understanding
@VulnerableCustomerAware           // Consumer Support
public ProductRecommendation recommendProduct(CustomerProfile customer) {
    // Implementation
}
```

### Evidence Requirements

| Outcome | Evidence | Regulus Source |
|---------|----------|----------------|
| Products & Services | Suitability assessments | Decision trace logs |
| Price & Value | Fee disclosure records | Audit trail |
| Consumer Understanding | Communication samples | Agent response logs |
| Consumer Support | Complaint resolution | Escalation records |

### Related Documentation
- [Governance & Security](../governance/governance-security.md)
- [Quickstart Tutorial](../guides/quickstart-tutorial.md)

---

## FCA SYSC 13.9: Outsourcing

### Source Document
**[SYSC 13.9 Outsourcing](https://www.handbook.fca.org.uk/handbook/SYSC/13/9.html)**

### Key Requirements

| Requirement | Description | Regulus Control |
|-------------|-------------|-----------------|
| **SYSC 13.9.2** | Appropriate skill and care in selection | Vendor due diligence |
| **SYSC 13.9.4** | Appropriate supervision | Data residency enforcement |
| **SYSC 13.9.5** | Effective access to data | UK region requirements |

### Regulus Implementation

```java
// Data residency enforcement
ResidencyCheckResult result = dataResidencyEnforcer.checkResidency(
    ResidencyCheckRequest.of("customer-pii", "us-east-1")
);

if (!result.isAllowed()) {
    // Block processing in non-approved regions
    throw new DataResidencyViolationException(result.message());
}
```

### Related Documentation
- [Data Residency Guide](../guides/data-residency.md)

---

## UK GDPR

### Source Documents
- **[UK GDPR Guidance](https://ico.org.uk/for-organisations/uk-gdpr-guidance-and-resources/)**
- **[International Transfers](https://ico.org.uk/for-organisations/uk-gdpr-guidance-and-resources/international-transfers/)**

### Key Requirements

| Article | Requirement | Regulus Control |
|---------|-------------|-----------------|
| **Art 5** | Data minimisation | PII redaction |
| **Art 6** | Lawful basis | Purpose tracking |
| **Art 9** | Special categories | SENSITIVE classification |
| **Art 17** | Right to erasure | DSAR support |
| **Art 44-49** | International transfers | Data residency |

### Data Classification Mapping

| GDPR Category | Regulus Classification | Transfer Rules |
|---------------|------------------------|----------------|
| Personal data | `PII` | UK/EEA, approval for others |
| Special category | `SENSITIVE` | UK/EEA, approval for others |
| N/A (UK regulated) | `UK_REGULATED` | UK only |

### Adequacy Decisions

| Country/Region | Status | Regulus Config |
|----------------|--------|----------------|
| EU/EEA | Adequate | Allowed regions |
| USA (DPF) | Adequate (with DPF) | Requires approval |
| Others | Case-by-case | Blocked by default |

### Regulus Implementation

```yaml
regulus:
  ai:
    safety:
      privacy:
        pii-pattern:
          enabled: true
        json-path:
          enabled: true
          paths:
            - $.nationalInsuranceNumber   # UK identifier
            - $.sortCode
            - $.accountNumber
      data-residency:
        enabled: true
        enforce-uk-residency: true
        allow-unknown-regions: false
```

### Related Documentation
- [Data Residency Guide](../guides/data-residency.md)
- [Starters Configuration](../guides/starters.md)

---

## DORA (Digital Operational Resilience Act)

### Source Document
**[DORA Regulation](https://www.eba.europa.eu/regulation-and-policy/internal-governance/digital-operational-resilience-act)**

Effective: January 2025

### Key Requirements

| Chapter | Requirement | Regulus Control |
|---------|-------------|-----------------|
| **II** | ICT risk management | Model Registry, Kill Switch |
| **III** | Incident reporting | Audit events, ServiceNow integration |
| **IV** | Resilience testing | Risk Simulation |
| **V** | Third-party risk | Vendor Registry, Data Residency |

### Regulus Implementation

DORA requirements are largely satisfied by existing PS21/3 and SS2/21 controls. Additional DORA-specific configuration:

```yaml
regulus:
  ai:
    governance:
      dora:
        enabled: true
        ict-risk-management:
          enabled: true
        incident-reporting:
          enabled: true
          authority-notification: true
        resilience-testing:
          frequency: ANNUAL
```

### Related Documentation
- [Kill Switch Design](../governance/kill-switch.md)
- [Risk Simulation](../governance/risk-simulation.md)

---

## Regulatory Timeline

```
2021 ─────────────────────────────────────────────────────────────────────
     │ Mar: SS2/21, PS21/3 published
     │
2022 ─────────────────────────────────────────────────────────────────────
     │ Mar: SS2/21 effective
     │ Jul: Consumer Duty published
     │
2023 ─────────────────────────────────────────────────────────────────────
     │ May: SS1/23 published
     │ Jul: Consumer Duty effective (new products)
     │
2024 ─────────────────────────────────────────────────────────────────────
     │ May: SS1/23 effective
     │ Jul: Consumer Duty effective (closed products)
     │
2025 ─────────────────────────────────────────────────────────────────────
     │ Jan: DORA effective
     │ Mar: PS21/3 full compliance deadline
     │
```

---

## Regulatory Contacts

| Authority | Contact | Website |
|-----------|---------|---------|
| PRA | pra.enquiries@bankofengland.co.uk | [bankofengland.co.uk/pra](https://www.bankofengland.co.uk/prudential-regulation) |
| FCA | consumer.queries@fca.org.uk | [fca.org.uk](https://www.fca.org.uk) |
| ICO | casework@ico.org.uk | [ico.org.uk](https://ico.org.uk) |

---

## Related Documentation

- [Governance & Security](../governance/governance-security.md)
- [Risk Control Matrix](../governance/risk-control-matrix.md)
- [Model Registry](../governance/model-registry.md)
- [Kill Switch Design](../governance/kill-switch.md)
- [Data Residency Guide](../guides/data-residency.md)
- [Consumer Duty Guide](../governance/consumer-duty.md)
