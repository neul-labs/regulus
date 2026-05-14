# ADR-010: AI governance framework integration model

- Status: Accepted
- Date: 2026-05-12
- Related: ADR-005 (EU AI Act mapping), ADR-009 (regtech-as-product-docs)

## Context

Regulus today expresses regulations via `ComplianceProfile` — UK GDPR,
FCA SYSC, EU AI Act, etc. As we extend the product into the broader **AI
governance** narrative we have a choice: reuse the same interface to
encode voluntary frameworks (NIST AI RMF, ISO/IEC 42001), or introduce a
distinct concept.

Reusing one interface is simpler. Introducing a second concept is more
expressive.

## Decision

Introduce a separate **`GovernanceFramework`** interface in the new
`regulus-ai-governance` module, parallel to but distinct from
`ComplianceProfile`. The two coexist, both binding to the same set of
mechanism identifiers (`pii-redaction`, `dual-control-kill-switch`,
`audit-trail`, etc.).

```java
package com.neullabs.regulus.governance;

public interface GovernanceFramework {
    String id();                            // "nist-ai-rmf", "iso-42001"
    String displayName();
    String version();
    FrameworkKind kind();                   // VOLUNTARY | CERTIFIABLE | STANDARD
    Set<FrameworkControl> controls();
    Set<FrameworkBinding> bindings();       // mechanism -> control id
    String authorityUrl();
}
```

Shipped implementations:

- `NistAiRmfFramework` — AI RMF 1.0 (GOVERN/MAP/MEASURE/MANAGE)
- `NistAiRmfGenAiProfile` — AI 600-1 (12 GAI risks)
- `NistAiRmfAgentInteropProfile` — planned Q4 2026 (placeholder IDs)
- `Iso42001Framework` — AI Management System + SoA generator
- `Iso23894Framework` — AI risk management
- `Iso23053Framework` — AI/ML system framework

Activation via:

```yaml
regulus:
  governance:
    frameworks: [nist-ai-rmf, nist-ai-rmf-600-1, iso-42001]
```

## Why separate from `ComplianceProfile`

- **Different semantic.** Regulation = mandatory; framework = voluntary.
  Conflating them confuses auditors, regulators, and lawyers.
- **Different metadata.** Frameworks have `FrameworkKind`
  (VOLUNTARY/CERTIFIABLE/STANDARD) and `version()`; regulations have
  `Jurisdiction`, `EventCompactionPolicy`, `ResidencyPolicy`,
  `AuditSchema`. The signature divergence is meaningful.
- **Different artefacts.** Frameworks produce SoA (Statement of
  Applicability) for ISO 42001 certification; regulations don't.
  Regulations produce regulator-shaped retention windows; frameworks
  don't.
- **Joinable in evidence.** The same audit event can carry
  `regulation_clause` *and* `framework_control_id`. One mechanism, two
  citations, two distinct readers — works because the concepts are
  separate.

## Alternatives considered

1. **Reuse `ComplianceProfile`.** Rejected — see "different semantic"
   above. The same code shape with two different meanings is a known
   anti-pattern.
2. **Tags on `ControlBinding` only.** Rejected — no first-class
   framework entity, no place to hang SoA generation, no clean home for
   framework-specific metadata.

## Consequences

Positive: clear separation; SoA generation has a natural home;
audiences (engineer / 2L / auditor) can navigate from either side.

Negative: one more concept to learn. Mitigated by Concepts pages
(`frameworks-vs-regulations.md`) and consistent naming.
