# Regulatory Control Matrix

This matrix links key UK financial services regulations to Regulus controls so risk, compliance, and audit teams can trace coverage.

---

## Core Regulatory Controls

| Regulation / Requirement | Regulus Control | Evidence / Artefact | Owner |
| --- | --- | --- | --- |
| [PRA SS1/23](https://www.bankofengland.co.uk/prudential-regulation/publication/2023/may/model-risk-management-principles-for-banks-ss) – Model Inventory | `@ModelArtefact` + Model Registry | Model inventory extract, risk tier assignments | Model Risk |
| [PRA SS1/23](https://www.bankofengland.co.uk/prudential-regulation/publication/2023/may/model-risk-management-principles-for-banks-ss) – Independent Validation | CI gating + validation workflow | Validation sign-off records, challenger reports | Model Validation |
| [PRA SS1/23](https://www.bankofengland.co.uk/prudential-regulation/publication/2023/may/model-risk-management-principles-for-banks-ss) – Monitoring & KRIs | Model Registry performance tracking | Drift metrics, eval pass rates, alert history | Platform Ops |
| [PRA PS21/3](https://www.bankofengland.co.uk/prudential-regulation/publication/2021/march/operational-resilience-impact-tolerances-for-important-business-services) – Operational Resilience | Circuit breakers, kill switches, chaos drills | DR test reports, runbook links, kill switch logs | SRE / Resilience |
| [PRA SS2/21](https://www.bankofengland.co.uk/prudential-regulation/publication/2021/march/outsourcing-and-third-party-risk-management-ss) – Outsourcing Register | Vendor registry module | Vendor records, due diligence checklists, exit test evidence | Third Party Risk |
| [FCA Consumer Duty](https://www.fca.org.uk/firms/consumer-duty) – Customer Outcomes | Policy guards, suitability checks | Policy configuration, decision trace samples | Product / Compliance |
| [UK GDPR](https://ico.org.uk/for-organisations/uk-gdpr-guidance-and-resources/) – Data Protection | Privacy shim, PII redaction | DPIA records, DSAR logs, lawful basis audit | Data Privacy |
| [FCA SYSC 13.9](https://www.handbook.fca.org.uk/handbook/SYSC/13/9.html) – Outsourcing | Data Residency Enforcer | Region validation logs, violation reports | Third Party Risk |

---

## Safety & Governance Controls

| Control Area | Regulus Component | Configuration | Evidence |
| --- | --- | --- | --- |
| **Kill Switch (Global)** | `KillSwitchManager` | `regulus.ai.safety.kill-switch.enabled` | Activation logs, audit trail |
| **Kill Switch (Scoped)** | `KillSwitchManager` | Scope: AGENT, CONNECTOR, TOOL | Per-scope activation records |
| **Dual-Control (4-Eyes)** | `DualControlKillSwitch` | `regulus.ai.safety.kill-switch.dual-control.enabled` | Approval workflow logs, approver list |
| **Emergency Bypass** | `DualControlKillSwitch` | `allow-emergency-bypass: true` | Emergency activation audit entries |
| **Data Residency** | `DataResidencyEnforcer` | `regulus.ai.safety.data-residency.enabled` | Violation logs, blocked requests |
| **UK-Only Processing** | `DataResidencyEnforcer` | `enforce-uk-residency: true` | UK_REGULATED data audit trail |
| **PII Redaction** | `PiiPatternFilter` | `regulus.ai.safety.privacy.pii-pattern.enabled` | Redaction statistics |
| **JSONPath Redaction** | `JsonPathRedactionFilter` | `regulus.ai.safety.privacy.json-path.paths` | Configured paths, redaction logs |
| **Prompt Injection** | `PromptInjectionDetector` | `regulus.ai.safety.prompt-injection.enabled` | Detection events, blocked requests |

---

## Model Registry Controls (SS1/23)

| SS1/23 Principle | Regulus Control | Evidence |
| --- | --- | --- |
| Comprehensive inventory | `ModelRegistry.register()` | Registry export, ownership records |
| Risk classification | `RiskTier` enum (TIER_1 to TIER_4) | Tier assignments, classification rationale |
| Validation framework | `ValidationRecord` tracking | Validation dates, outcomes, next review |
| Performance monitoring | `PerformanceRecord` metrics | Accuracy, latency, drift scores |
| Change management | Registry audit trail | All changes with actor and timestamp |
| Model lifecycle | `DeploymentStatus` tracking | Status transitions, retirement records |

---

## Data Residency Controls (UK GDPR / FCA SYSC 13.9)

| Data Classification | Allowed Regions | Control |
| --- | --- | --- |
| `PUBLIC` | Any | No restriction |
| `STANDARD` | Configured allowed regions | Region whitelist validation |
| `PII` | UK/EEA only | Cross-border approval required |
| `SENSITIVE` | UK/EEA only | Cross-border approval required |
| `UK_REGULATED` | UK only | No cross-border transfers |
| `CRITICAL` | UK only | No cross-border transfers |

### Approved UK Regions

| Cloud Provider | Region Code | Location | Approved |
| --- | --- | --- | --- |
| GCP | `europe-west2` | London | Yes |
| GCP | `europe-west1` | Belgium | Yes (EU adequacy) |
| AWS | `eu-west-2` | London | Yes |
| Azure | `uksouth` | London | Yes |
| Azure | `ukwest` | Cardiff | Yes |

---

## MCP/A2A Protocol Controls

| Control | MCP | A2A | Evidence |
| --- | --- | --- | --- |
| Policy enforcement | `PolicyGuard` aspect | `PolicyGuard` aspect | Policy violation logs |
| Privacy filtering | `PrivacyFilterChain` | `PrivacyFilterChain` | Redaction statistics |
| Kill switch | Interceptor wrapping | Interceptor wrapping | Kill state in responses |
| Audit logging | Kafka events | Kafka events | Splunk/OTEL records |
| Data residency | Endpoint validation | Agent card validation | Region check logs |
| mTLS | Server/client certs | Server/client certs | Certificate chain validation |
| Rate limiting | Token bucket | Token bucket | Rate limit events |

---

## Compliance Evidence Summary

| Regulation | Primary Evidence | Location | Retention |
| --- | --- | --- | --- |
| PRA SS1/23 | Model cards, validation reports | GRC repository | 7 years |
| PRA PS21/3 | Kill switch logs, DR test reports | Splunk + ServiceNow | 7 years |
| PRA SS2/21 | Vendor records, exit test evidence | Vendor management system | 7 years |
| FCA Consumer Duty | Decision traces, suitability letters | Audit database | 7 years |
| UK GDPR | DSAR logs, DPIA records | Privacy register | 7 years |
| FCA SYSC 13.9 | Data residency violation logs | Compliance dashboard | 7 years |

---

## Control Ownership

| Control Area | Primary Owner | Secondary Owner |
| --- | --- | --- |
| Model Registry | Model Risk | Platform Team |
| Kill Switch | Platform Ops | Risk Team |
| Data Residency | Data Privacy | Third Party Risk |
| Privacy Filters | Data Privacy | Platform Team |
| MCP/A2A Security | AppSec | Platform Team |
| Eval Quality Gates | AI Risk | Model Validation |

> Evidence locations and owners should be updated per release to keep the matrix current.

---

## Related Documentation

- [Governance & Security](./governance-security.md) - Regulatory overview
- [Model Registry](./model-registry.md) - SS1/23 model inventory
- [Kill Switch Design](./kill-switch.md) - Dual-control implementation
- [Data Residency Guide](../guides/data-residency.md) - UK GDPR compliance

