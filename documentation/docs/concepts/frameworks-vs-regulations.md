# Frameworks vs regulations

Two words readers conflate at their cost. Regulus draws a hard line.

## The distinction

| | Regulation | Framework |
|---|---|---|
| **Question it answers** | What does the law force me to do? | What should I do to be a mature operator? |
| **Mandatory?** | Yes, if you're in scope | Voluntary (with one wrinkle — see below) |
| **Enforcer** | Government regulator (FCA, ICO, AI Office, etc.) | None directly; certification body for certifiable frameworks |
| **Penalty for non-compliance** | Fines, criminal sanctions, market access loss | Reputational, customer trust, certification denial |
| **Examples** | EU AI Act, GDPR, DORA, FCA SYSC, PRA SS1/23 | NIST AI RMF, ISO/IEC 42001, ISO/IEC 23894 |

The wrinkle: frameworks become *de facto* mandatory when buyers demand
them. ISO/IEC 42001 certification is voluntary in law, but if your
enterprise customers require it before signing a contract, it might as
well be mandatory in practice.

## Why Regulus separates them in code

A single Regulus mechanism (e.g. `pii-redaction`) can satisfy:

- **GDPR Art. 25** — a regulation. The law says so.
- **NIST AI RMF GAI-4** — a framework. Best practice for managing the GAI
  data-privacy risk.
- **ISO/IEC 42001 A.7.4** — a framework. The AIMS control for data
  provenance.

These are not the same kind of obligation. Conflating them means:

- Auditors get confused (different evidentiary standards).
- Buyers can't tell what you *must* do vs what you *chose* to do.
- Lawyers can't safely cite Regulus' evidence to a regulator.

Regulus encodes the distinction:

- `ComplianceProfile` describes regulations. Activation via
  `regulus.compliance.profiles: [...]`.
- `GovernanceFramework` describes frameworks. Activation via
  `regulus.governance.frameworks: [...]`.

Audit events carry both — `regulation_clause` (for the regulatory
citation) *and* `framework_control_id` (for the framework reference) —
so the same evidence stream serves both views.

## How they compose

Most production deployments run **both** layers active. A UK retail bank
running an FCA-authorised mortgage-advice agent might configure:

```yaml
regulus:
  compliance:
    profiles: [eu-ai-act, uk-gdpr, fca-sysc, pra-ss1-23, pra-ss2-21]
  governance:
    frameworks: [nist-ai-rmf, nist-ai-rmf-600-1, iso-42001]
```

The composite of the regulation profiles enforces the legal floor; the
composite of the frameworks expresses the maturity ceiling the firm has
chosen. The coverage matrix shows you both views side by side.

## A practical heuristic

If a clause cites an **Article**, **Section**, **Schedule**, **Annex** of
a named statute or regulation → it's a regulation. Use
`ComplianceProfile`.

If a clause cites a **Function** (GOVERN), **Subcategory** (GOVERN-1.5),
**Annex A control** (A.6.2.7), or a **Clause** (CL-5) of a numbered
standard → it's a framework. Use `GovernanceFramework`.

## Where to read next

- [Governance frameworks](../governance/frameworks/index.md)
- [Compliance regulations](../compliance/index.md)
- [Coverage matrix](../compliance/coverage-matrix.md)
