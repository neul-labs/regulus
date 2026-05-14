# NHS DSPT

## In one sentence

The NHS Data Security and Protection Toolkit: an annual self-assessment
against the National Data Guardian's ten standards that every organisation
handling NHS patient data must complete.

## Who does it apply to?

- NHS organisations (trusts, ICBs, etc.).
- Any organisation processing NHS data — including private providers, GP
  practices, dentists, suppliers to the NHS.
- Any digital health product touching NHS data — including AI agents.

If your agent ever sees NHS data, including a single NHS Number, you're
in scope.

## The two-minute explainer

The DSPT was launched in 2018, replacing the older Information Governance
Toolkit. It is an annual self-assessment with mandatory standards aligned
to the National Data Guardian's review. Standards cover personal data
protection, staff awareness, training, managing data, processes for new
suppliers, secure data systems, accountable suppliers, comprehensive
incident management, continuity planning, and unsupported systems.

For AI agents:

- **Personal data protection** maps to PII redaction with NHS-specific
  patterns (NHS Number, clinician smartcard ID).
- **Staff responsibility** maps to per-event attribution to a clinician
  identity.
- **Comprehensive incident management** maps to the IG SIRI (Information
  Governance — Serious Incident Requiring Investigation) process.
- **Secure data systems** maps to residency + CMEK on UK-only
  infrastructure (EU adequacy is not the safest default for confidential
  patient information).

## What it actually requires of an engineer

- **NHS-specific PII patterns.** NHS Number minimum; ideally smartcard IDs
  and clinical-free-text indicators.
- **Per-event attribution** to a clinician's identity (smartcard ID or
  equivalent). Not just "user XYZ" — clinician identity.
- **Incident management process** that aligns with IG SIRI severity
  reporting.
- **Long retention** — adult health records typically 8 years; paediatric
  records to age 25; mental-health up to 20 years.
- **UK-only data residency** strongly preferred for confidential patient
  information.

## What Regulus does for you

- `RegulusPrivacyPlugin` ships with `NHS_NUMBER` as a built-in pattern.
- `RegulusAuditPlugin` emits `clinician_smartcard_id`, `nhs_number_hashed`,
  `care_setting`, `lawful_basis_health` on events when `nhs-dspt` is
  active.
- The `nhs-dspt` profile retention is set to 8 years (raw) + 25 years
  (summary) with `SIGNED` immutability and erasure not permitted (NHS
  records retention overrides the subject's erasure right).
- Residency defaults to `europe-west2`; CMEK required; cross-border
  transfer forbidden by default.

## Saves you ~

- NHS-specific PII patterns + tests: ~1.5 engineer-weeks.
- Clinician-identity audit linkage: ~2 engineer-weeks.
- IG SIRI integration: ~3 engineer-weeks.
- Long retention + UK-only residency: shared with FCA/PRA where applicable.

Total incremental: ~6.5 engineer-weeks.

## Code: minimal

```yaml
regulus:
  compliance:
    profiles: [nhs-dspt]
```

## Code: production

```yaml
regulus:
  compliance:
    profiles: [nhs-dspt, uk-gdpr]   # NHS DSPT + UK GDPR together
  adk:
    audit:
      sink: kafka
      kafka-topic: audit.nhs.v1
    residency:
      allowed-regions: [europe-west2]
      require-cmek: true
    kill-switch:
      enabled: true
      dual-control: true
```

In Java, attach a clinician identity to the invocation context:

```java
PolicyContext context = new PolicyContext(
    "primary-care-triage",
    hashedNhsNumber,
    "clinician:smartcard:" + smartcardId,
    "model",
    "gemini-2.5-pro",
    Map.of(
        "clinician_smartcard_id", smartcardId,
        "care_setting", "primary-care",
        "lawful_basis_health", "Art. 9(2)(h)"
    ));
```

## How to verify

- Sample event includes `clinician_smartcard_id`, `nhs_number_hashed`,
  `care_setting`, `lawful_basis_health`.
- Synthetic NHS Number in a prompt: redacted to `<NHS_NUMBER_1>` before
  reaching the model.
- Forced UK-out residency: startup fails.

## What an auditor will ask

1. **"How is patient identity protected?"** Privacy plugin + audit
   `nhs_number_hashed`.
2. **"Who saw this record?"** Clinician identity per event.
3. **"How would you handle a SIRI?"** Audit pipeline → SIRI report
   template.
4. **"Where does NHS data live?"** UK-only residency demonstration.

## What this doesn't cover

- **The DSPT submission itself.** You complete it annually; we contribute
  to the technical-controls answers.
- **NHS Care Records Service / Spine integration.** Off-scope.
- **Clinical Risk Management (DCB0129, DCB0160).** Separate clinical
  safety regime; document outside Regulus.

## Framework mapping

- **NIST AI RMF** — DSPT 1.x (personal data) ↔ `MEASURE-2.7`. DSPT 4.x
  (staff identity) ↔ `GOVERN-2.1`. DSPT 6.x (incidents) ↔ `MANAGE-2.2`.
- **ISO/IEC 42001** — DSPT 1.x ↔ `A.7` (Data for AI). DSPT 4.x ↔
  `A.3.2`. DSPT 6.x ↔ `A.8.4`.

## Citations

- NHS DSPT — https://www.dsptoolkit.nhs.uk
- National Data Guardian Review — https://www.gov.uk/government/publications/your-data-better-security-better-choice-better-care
- NHS Records Management Code — https://www.nhsx.nhs.uk/information-governance/guidance/records-management-code/
- DCB0129 / DCB0160 — Clinical Risk Management standards (NHS Digital).
