# NIST AI RMF

## In one sentence

The US National Institute of Standards and Technology's voluntary
framework for managing risks from AI systems, organised around four
functions: **GOVERN, MAP, MEASURE, MANAGE**.

## Who in your org owns it

- **CAIO / CISO** — strategic sponsorship.
- **Head of Model Risk** — implementation across the AI portfolio.
- **2L risk function** — independent review against the framework.

## The two-minute explainer

NIST AI RMF 1.0 was published in January 2023 as a voluntary framework —
not a regulation, not a standard, not a certification. It's organised
around four functions:

- **GOVERN** — culture, accountability, policy, third-party risk.
- **MAP** — context, intended uses, risks identified.
- **MEASURE** — quantitative + qualitative metrics for risks.
- **MANAGE** — treatment, response, decommissioning.

Each function has categories (e.g. GOVERN-1, GOVERN-2) and subcategories
(GOVERN-1.1, GOVERN-1.2, ...). Roughly 70 subcategories in total.

The framework has gained substantial international traction since
publication, often as the de-facto AI risk reference even in jurisdictions
not bound by US rules. EU firms commonly run it alongside ISO/IEC 42001.

## The two extensions

### GenAI Profile

In July 2024 NIST published **AI 600-1**, a Generative AI Profile that
identifies 12 GAI-specific risks (confabulation, data privacy, harmful
bias, info security, IP, etc.) and maps each onto actions across the
four functions. Regulus' `nist-ai-rmf-600-1` framework binds Regulus
mechanisms to these GAI risks directly — e.g. `pii-redaction` ↔ GAI-4
data privacy.

### Agent Interop Profile (planned Q4 2026)

On 7 April 2026 NIST published a concept note for an **AI Agent
Interoperability Profile** covering identity and authorisation, security
and risk management, and monitoring and logging. The final profile is
targeted for Q4 2026. Regulus pre-stubs the categories named in the
concept note so adopters can pre-bind; control IDs will be remapped to
match NIST's final publication.

## What Regulus does for you

The `NistAiRmfFramework` class binds selected subcategories to Regulus
mechanisms. Highlights:

- **GOVERN-1.5** (ongoing monitoring) ↔ `audit-trail`.
- **GOVERN-2.1** (roles and responsibilities) ↔
  `senior-management-arrangements`.
- **GOVERN-6.1** (third-party risk) ↔ `third-party-risk` + model registry.
- **MAP-1.1** (context characterisation) ↔ `purpose-binding`.
- **MAP-4.1** (risk classification) ↔ `model-risk-tier`.
- **MEASURE-2.7** (security and resiliency) ↔ `data-residency`.
- **MANAGE-2.2** (incident response) ↔ `incident-classification`.

The full inventory of subcategories Regulus binds is rendered in the
[Coverage matrix](../../compliance/coverage-matrix.md) and is generated
from `NistAiRmfFramework.bindings()`.

## Activation

```yaml
regulus:
  governance:
    frameworks: [nist-ai-rmf, nist-ai-rmf-600-1]
```

Add `nist-ai-rmf-agent-interop` once your team is comfortable working
against placeholder IDs.

## What an assessor will ask

1. **"Which subcategories does this AI system address?"** Coverage matrix
   answers per active mechanism.
2. **"Show me an example of each function in operation."** Audit events
   tagged with `framework_control_id` populate the GOVERN/MAP/MEASURE/MANAGE
   evidence buckets.
3. **"What about the 12 GAI risks?"** The `nist-ai-rmf-600-1` bindings
   show which Regulus mechanisms address each risk.

## What this doesn't cover

- **Self-assessment / certification.** NIST AI RMF isn't certifiable.
  ISO/IEC 42001 is — and Regulus also supports it.
- **Quantitative metric design** (e.g. fairness measurement). External
  measurement; Regulus emits the audit substrate.
- **Subcategories Regulus doesn't bind.** Roughly 70 subcategories total
  — Regulus binds the ones it actually enforces. The unbound ones are
  organisational / process work that lives outside the runtime.

## Citations

- NIST AI RMF 1.0 — https://nvlpubs.nist.gov/nistpubs/ai/nist.ai.100-1.pdf
- AI 600-1 GenAI Profile — https://nvlpubs.nist.gov/nistpubs/ai/NIST.AI.600-1.pdf
- AI RMF home — https://www.nist.gov/itl/ai-risk-management-framework
- Agent Interop Profile concept note (April 2026) — NIST AI RMF home.
