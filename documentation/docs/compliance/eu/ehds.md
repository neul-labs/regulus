# EHDS

## In one sentence

The European Health Data Space: an EU framework for primary use (patient
access to and control over their electronic health records) and secondary
use (research, policy, regulatory work) of health data across member
states.

## Who does it apply to?

- Healthcare providers and EHR system manufacturers in the EU.
- Data holders (anyone holding electronic health data in the EU).
- Data users (researchers, policymakers, regulators applying for secondary
  use permits).
- Non-EU entities offering EHR products to EU healthcare providers.

If your agent processes EU electronic health data — clinical free text,
imaging, demographic codes, NHS or other patient identifiers — you're
adjacent to EHDS and likely in scope.

## The two-minute explainer

EHDS (Regulation EU 2025/327) is the youngest of the EU rulebooks in this
list and the most ambitious for health data. Its two halves:

**Primary use.** Patients get cross-border access to their EHR data,
interoperability is mandatory for EHR systems, and certain priority data
categories (patient summary, e-prescription, imaging, lab results) must be
shareable in a common format.

**Secondary use.** A regulated pathway for researchers, policymakers, and
regulators to access pseudonymised health data via national **Health Data
Access Bodies**, with a permit and quality / utility labels on the data.

EHDS sits on top of GDPR (which still applies to personal health data),
not in place of it. The AI Act overlays where the use case is AI.

## What it actually requires of an engineer

- **Patient access** (Chapter II). Surface an erasure / access path that
  respects EHDS-specific timelines.
- **Secondary use permit** (Chapter IV). If the agent operates in a
  research context, the permit reference must be recorded on every event.
- **Data quality labels** (Art. 56). Provenance and utility tagging on AI-
  assisted records. Auditable.
- **EHR interoperability** (Chapter III). FHIR-shaped audit events; if the
  agent reads/writes EHR systems, conformity to the European EHR Exchange
  Format.

## What Regulus does for you

- `RegulusAuditPlugin` emits `patient_pseudonym`, `permit_ref`,
  `data_quality_label`, `care_setting` on every event when `ehds` is
  active.
- Retention defaults to 10 years (raw) + 30 years (summary) with `SIGNED`
  immutability — reflective of clinical record management.
- Erasure path on Regulus session/memory services aligns with EHDS primary-
  use control.

## Saves you ~

- EHDS-specific audit schema and permit linkage: ~3 engineer-weeks.
- Long-retention pipeline with signing: shared with DORA / FCA pipelines.
- Data quality / utility labelling on AI-assisted records: ~2 engineer-
  weeks.

Net: ~5 engineer-weeks incremental over the GDPR baseline.

## Code: minimal

```yaml
regulus:
  compliance:
    profiles: [ehds]
```

## Code: production

```yaml
regulus:
  compliance:
    profiles: [ehds, gdpr, eu-ai-act]
  adk:
    audit:
      sink: kafka
      kafka-topic: audit.ehds.v1
    residency:
      allowed-regions: [europe-west1, europe-west3]
      require-cmek: true
    kill-switch:
      enabled: true
      dual-control: true
```

## How to verify

- Audit events carry `patient_pseudonym`, `permit_ref`, quality labels.
- Erasure flow on a sample session removes payloads, retains a tombstone.
- Quality labels propagate to downstream EHR write attempts.

## What an auditor will ask

1. **"Show me a secondary-use permit and the linked events."** Permit
   reference per event.
2. **"What quality label did this AI-assisted record receive?"** Per-event
   data quality field.
3. **"How does primary-use access work end to end?"** Demo: patient request,
   Regulus erasure / access path, audit trail.

## What this doesn't cover

- **FHIR mapping.** Off-scope; we provide an FHIR-AuditEvent-compatible
  schema as guidance.
- **EHR conformity certification.** EU-level scheme; external.
- **Medical-device classification.** MHRA / MDR / IVDR; separate
  regulatory layer.

## Citations

- Regulation (EU) 2025/327 — https://eur-lex.europa.eu/eli/reg/2025/327/oj
- Chapter II — primary use.
- Chapter III — interoperability of EHR systems.
- Chapter IV — secondary use.
- Art. 56 — data quality and utility labels.
