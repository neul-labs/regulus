# Governance, Risk, and Security

Regulus is designed for UK financial services and aligns with key regulatory frameworks:

| Regulation | Authority | Focus Area |
|------------|-----------|------------|
| [PRA SS1/23](https://www.bankofengland.co.uk/prudential-regulation/publication/2023/may/model-risk-management-principles-for-banks-ss) | PRA | Model risk management |
| [PRA PS21/3](https://www.bankofengland.co.uk/prudential-regulation/publication/2021/march/operational-resilience-impact-tolerances-for-important-business-services) | PRA | Operational resilience |
| [PRA SS2/21](https://www.bankofengland.co.uk/prudential-regulation/publication/2021/march/outsourcing-and-third-party-risk-management-ss) | PRA | Outsourcing and third-party risk |
| [FCA Consumer Duty](https://www.fca.org.uk/firms/consumer-duty) | FCA | Customer outcomes |
| [FCA SYSC 13.9](https://www.handbook.fca.org.uk/handbook/SYSC/13/9.html) | FCA | Outsourcing requirements |
| [UK GDPR](https://ico.org.uk/for-organisations/uk-gdpr-guidance-and-resources/) | ICO | Data protection |
| [DORA](https://www.eba.europa.eu/regulation-and-policy/internal-governance/digital-operational-resilience-act) | EBA | Digital operational resilience |

This document summarises the layered controls that wrap ADK/MCP/A2A integrations.

---

## Model Governance ([PRA SS1/23](https://www.bankofengland.co.uk/prudential-regulation/publication/2023/may/model-risk-management-principles-for-banks-ss))

- **Inventory & Registration**: Each agent is annotated (e.g., `@ModelArtefact`) and auto-registered in the enterprise model inventory with owner, risk rating, intended use, and review cadence.
- **Policy Taxonomy**: Centrally managed policy rules (LEI, purpose codes, consent checks) are version-controlled in the bank’s governance library and mapped to annotations used in code.
- **Artefact Generation**: CI pipelines produce model cards, evaluation results, challenger comparisons, fairness/bias reports, explainability artefacts, approval logs, and change history. Artefacts are stored in the bank's GRC repository (Archer/ServiceNow GRC).
- **Independent Review**: Regulus emits reminders and gating checks to ensure independent validation and challenger sign-off occur before release and at scheduled revalidations.
- **KRIs & Monitoring**: Drift metrics, data quality scores, policy violation counts, kill-switch activations, and eval pass rates feed the risk dashboard for ongoing oversight.
- **Implementation Guidance**: Refer to `../references/integration-matrix.md` and `../references/implementation-playbooks.md` to scope adapter work with Model Risk and GRC teams.

## Model & Agent Safety Team Engagement

- **Evaluation Gates**: Safety leads define the minimum/maximum thresholds enforced via `@AiGate` annotations and review Gradle eval outputs before changes graduate to higher environments.
- **Scenario & Simulation Oversight**: Safety teams curate the extreme-but-plausible scenario catalogue and sign off on `regulus-ai-risk-simulation` runs before pilot or production releases.
- **Guardrail Governance**: Policy guard changes, privacy mask updates, and new DSL policy vocabularies route through safety approval to ensure LEI/purpose/consent coverage stays intact.
- **Safety Models**: Classifiers/SLMs that detect vulnerability, mis-selling risk, or harmful content are registered as managed tools; safety teams own their training data, bias checks, and performance monitoring.
- **Kill Switch Operations**: Safety owners sit on the dual-control rota for kill switch activations, run quarterly drills, and confirm recovery evidence is archived for PS21/3 compliance.
- **Incident & Drift Review**: Safety stakeholders receive automated alerts for eval breaches, drift signals, and policy violations; they coordinate remediation and record decisions in the GRC workflow.
- **Vendor & Model Registry Alignment**: External LLM/MCP/A2A providers and new model artefacts are reviewed with safety representatives to validate risk scoring, exit testing, and monitoring hooks.
- **Playbook References**: Safety teams should follow the kill switch, eval service, and vendor registry playbooks (`../references/implementation-playbooks.md`) to maintain consistent operations.

## Privacy & Data Protection ([UK GDPR](https://ico.org.uk/for-organisations/uk-gdpr-guidance-and-resources/))

- **Redaction & Minimisation**: Automatic masking of PII in prompts, MCP payloads, and A2A messages using configurable JSONPath selectors, with minimisation rules enforced per data category.
- **Purpose, Lawful Basis & Retention**: Payloads carry metadata for purpose, lawful basis, and retention; downstream services enforce TTLs accordingly.
- **Records & DPIAs**: Each agent links to a record of processing and DPIA stored in the privacy register; updates trigger privacy review workflows.
- **DSAR & Transparency**: Audit trails reconstruct decision traces and expose data subjects' records upon request; responses reference explainability artifacts for transparency obligations.
- **Data Residency**: The `DataResidencyEnforcer` ensures regulated data is processed only in UK-approved regions. See [Data Residency Guide](../guides/data-residency.md).

## Operational Resilience ([PRA PS21/3](https://www.bankofengland.co.uk/prudential-regulation/publication/2021/march/operational-resilience-impact-tolerances-for-important-business-services) / [DORA](https://www.eba.europa.eu/regulation-and-policy/internal-governance/digital-operational-resilience-act))

- **Circuit Breakers & Fallbacks**: Resilience4j wraps MCP/A2A/LLM calls with timeouts, retries, and fallbacks (SLM-first routing).
- **Kill Switches**: Break-glass mechanisms integrate with the enterprise toggle service (ConfigHub or Vault-backed Spring Cloud Config), enforce dual-control (4-eyes) approvals, and emit audit events. See [Kill Switch Design](./kill-switch.md).
- **Continuity Planning**: Each agent documents RTO/RPO, manual fallback procedures, and dependency maps; runbooks live in ServiceNow/Confluence and are linked from the platform console.
- **Testing Calendar**: Chaos experiments, DR failovers, and manual fallback drills are scheduled and captured with remediation tasks to satisfy PS21/3/DORA.
- **Incident Management**: SEV events automatically raise ServiceNow incidents, populate communication templates, and ensure business stakeholders receive impact assessments.

## Outsourcing & Third-Party Management ([PRA SS2/21](https://www.bankofengland.co.uk/prudential-regulation/publication/2021/march/outsourcing-and-third-party-risk-management-ss))

- **Provider Registry**: Maintains region, subprocessors, contract terms, exit strategies, financial health, and concentration risk scores for each MCP/A2A/LLM vendor.
- **Due Diligence Workflow**: Intake checklists capture security attestations, penetration results, SOC reports, and board approvals; periodic reassessments are scheduled with reminders.
- **Exit & Substitution Tests**: Planned exercises validate the bank's ability to disengage or replace providers; evidence is retained in the vendor management system.
- **Contract Drift Detection**: MCP/A2A schemas are version-controlled with CI gates, ensuring contractual interfaces stay in sync.
- **Data Residency Compliance**: All vendor endpoints validated against approved UK/EU regions before activation.

## Security Posture

- **RBAC & Segregation of Duties**: Spring Security enforces least privilege; privileged roles (e.g., kill switch operators) undergo quarterly access certification.
- **Identity & Secrets**: Integration with the bank’s IAM (Azure AD/ADFS) and Vault ensures token rotation, JIT access, and auditable secret usage.
- **Secure SDLC**: Starter releases ship with SBOMs, SAST/DAST results, threat models, penetration testing evidence, and documented mitigations.
- **Network & DLP**: Services run behind the mesh/gateway with mTLS, egress controls, and DLP policies aligned to existing bank standards.
- **Supply Chain**: Dependency updates follow the bank’s vulnerability management process, with CVE triage SLAs and waiver tracking.

## Audit & Assurance

- **Control Matrix**: Regulus maintains a mapping between regulatory requirements and platform controls (see [Risk Control Matrix](./risk-control-matrix.md)) for Internal Audit walkthroughs.
- **Evidence Repository**: All artefacts and approvals are version-controlled in the bank's GRC repository with ownership and review dates.
- **CSA & Testing**: Quarterly control self-assessments capture effectiveness, issues, action plans, and target remediation dates; results feed the audit universe.
- **Metrics & Reporting**: Dashboards surface KRIs/KPIs to risk committees, including adoption metrics, policy breaches, eval performance, and incident stats.

---

## Consumer Duty ([FCA Consumer Duty](https://www.fca.org.uk/firms/consumer-duty))

The FCA Consumer Duty requires firms to deliver good outcomes for retail customers. Regulus supports all four outcome areas:

### Products and Services
- Policy guards validate product suitability before recommendations
- Agent tools enforce eligibility checks
- MCP resources expose product documentation for transparent discovery

### Price and Value
- Audit trails capture fee transparency decisions
- Cost disclosure templates auto-generated for agent responses

### Consumer Understanding
- Explainability artefacts required by `@AiGate` annotations
- Plain language output validation
- Complexity scoring for communications

### Consumer Support
- Kill switch ensures rapid response to issues harming customers
- Vulnerable customer detection flags
- Escalation paths for human review

### Configuration

```yaml
regulus:
  ai:
    governance:
      consumer-duty:
        enabled: true
        vulnerable-customer-detection: true
        suitability-check-required: true
        explanation-detail-level: HIGH
        plain-language-validation: true
```

---

## Related Documentation

- [Model Registry](./model-registry.md) - SS1/23 model inventory
- [Risk Control Matrix](./risk-control-matrix.md) - Regulatory control mapping
- [Kill Switch Design](./kill-switch.md) - Dual-control implementation
- [Data Residency Guide](../guides/data-residency.md) - UK GDPR compliance
- [Spring Boot Starters](../guides/starters.md) - Configuration reference
