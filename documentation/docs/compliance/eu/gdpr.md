# GDPR

## In one sentence

GDPR is the EU's omnibus personal-data law: it defines what counts as
personal data, who's responsible for it, what lawful bases exist to process
it, and what rights data subjects keep.

## Who does it apply to?

- Anyone processing personal data of EU/EEA residents.
- Anyone established in the EU/EEA who processes personal data (even of
  non-EU subjects).
- Non-EU controllers offering goods or services to EU subjects, or monitoring
  their behaviour.

For AI agents, the practical test is: if the prompt, output, or tool input
contains identifiable information about an EU person — names, emails, IPs,
behavioural data — you're in scope.

## The two-minute explainer

GDPR replaced a 1995 directive in 2018 with a regulation: directly
applicable across the EU, with teeth (fines up to 4% of global turnover).
The core moves were: extraterritorial reach, mandatory DPOs in some cases,
breach notification within 72 hours, and explicit data-subject rights
(access, rectification, erasure, portability, objection).

For engineers, two shapes dominate.

**Principles in Art. 5.** Personal data must be processed lawfully, fairly,
and transparently; collected for explicit purposes and not used for
incompatible ones; kept only as long as needed; protected by appropriate
technical and organisational measures. These principles back-stop everything
else.

**Lawful bases in Art. 6.** You can only process personal data if you have a
lawful basis: consent, contract, legal obligation, vital interests, public
task, or legitimate interests. Special-category data (health, race,
biometrics, etc.) needs an additional Art. 9 basis. Pick wrong and the whole
processing falls.

The AI-relevant articles for engineers: **Art. 22** (no fully-automated
decisions with legal effect without safeguards), **Art. 25** (data protection
by design and by default), **Art. 30** (records of processing), **Art. 33**
(72-hour breach notification), **Arts. 44-49** (international transfers).

## What it actually requires of an engineer

- **Purpose binding** for every processing activity (Art. 5(1)(b)). You
  record the purpose; you don't reuse the data for an incompatible one.
- **Storage limitation** (Art. 5(1)(e)). Define a retention window and
  enforce it. Regulus' `RegulusRetentionEventCompactor` is the technical
  enforcement.
- **Privacy by design** (Art. 25). Minimisation, pseudonymisation, the right
  defaults. PII redaction before LLM call is a textbook Art. 25 control.
- **Records of processing** (Art. 30). The list of processing activities,
  what data they touch, who has access, retention. Auditors want to see
  this synced with reality.
- **Subject rights** (Arts. 15–22). Access, rectification, erasure,
  portability, objection, and the Art. 22 safeguards on automated decisions.
- **International transfers** (Arts. 44–49). Adequacy decisions, SCCs, BCRs,
  derogations. Region pinning + transfer paperwork.
- **Breach notification** (Art. 33). 72-hour clock from awareness. Audit
  trail must support reconstruction.

## What Regulus does for you

- `RegulusPrivacyPlugin` runs PII redaction before every model call. Built-
  in patterns cover the common high-risk data (NINO, IBAN, BIC, email,
  postcode, NHS Number).
- `RegulusPolicyPlugin` enforces purpose codes — agent cannot run without
  one — and `RequireConfirmation` for Art. 22 automated decisions with
  legal effect.
- `RegulusAuditPlugin` emits the Art. 30 records (subject_id, purpose_code,
  lawful_basis, data_categories) so your DPO has live ROPA evidence.
- `RegulusRetentionEventCompactor` enforces storage limitation with
  per-profile retention.
- `RegulusDataResidencyPlugin` pins regions; the GDPR profile defaults to
  EU/EEA regions with SCC allowance for transfers.
- `Regulus*SessionService` and `RegulusFirestoreMemoryService` expose an
  Art. 17 erasure path.

## Saves you ~

- Pattern library + tests for PII: ~3 engineer-weeks.
- ROPA + audit pipeline + retention: ~6 engineer-weeks.
- Region pinning with startup fail-closed: ~2 engineer-weeks.
- Art. 17 erasure path that doesn't leak the body to the audit log: ~1
  engineer-week.

Total: ~12 engineer-weeks of foundation, before you've started on
sector-specific layers. Regulus is one dependency.

## Code: minimal

```yaml
regulus:
  compliance:
    profiles: [gdpr]
```

## Code: production

```yaml
regulus:
  compliance:
    profiles: [gdpr, eu-ai-act]
  adk:
    audit:
      sink: kafka
      kafka-topic: audit.gdpr.v1
    residency:
      allowed-regions:
        - europe-west1
        - europe-west2
        - europe-west3
        - europe-west4
    session-service:
      kind: vertex-ai
      project-id: ${GOOGLE_CLOUD_PROJECT}
      location: europe-west1
```

## How to verify

- Request with an embedded NINO: audit event shows `redactions: [NINO_1]`;
  raw NINO never reaches the model.
- Request without `purpose_code`: blocked at `BeforeModelCallback` with
  `Block(missing_purpose, ..., Art. 5(1)(b))`.
- Tear down a customer: erasure path on `RegulusFirestoreSessionService`
  removes session payloads, replaces with tombstones in the audit log
  referencing the deletion event.

## What an auditor will ask

1. **"Where is your ROPA?"** Audit topic with `purpose_code`, `lawful_basis`,
   `data_categories` fields.
2. **"Show me your retention policy."** `EventCompactionPolicy` for the
   `gdpr` profile; Kafka topic retention setting; demonstration of
   compaction running.
3. **"How do you action a subject access request?"** Subject-linked query in
   the audit log + erasure path demo.
4. **"How do you stop personal data from leaving the EEA?"**
   `RegulusDataResidencyPlugin` startup check; show the configured
   allowlist; show fail-closed behaviour when a misconfiguration is forced.
5. **"What happens for fully-automated decisions with legal effect?"**
   `RegulusPolicyPlugin` policy chain produces `RequireConfirmation`; demo
   the ADK `ToolConfirmation` flow.

## What this doesn't cover

- **Choosing your lawful basis.** That's a DPO / legal call. We record what
  you say it is; we don't validate the choice.
- **DPIA execution.** Regulus exports evidence; you write the DPIA.
- **Cross-border transfer paperwork** (SCCs, BCRs). Operational; we surface
  the technical state.
- **Special-category data classification.** You tag the data category in
  the invocation context; we audit and enforce on the tag.
- **The right to be forgotten under the e-Commerce Directive / Digital
  Services Act.** Separate framework; not in scope.

## Framework mapping

The GDPR articles Regulus most actively enforces map to:

- **NIST AI RMF** — Art. 5(1)(b) (purpose) ↔ `MAP-1.1`. Art. 25
  (privacy by design) ↔ `GOVERN-1.1`. Arts. 44-49 (transfers) ↔
  `MEASURE-2.7`.
- **NIST 600-1 GenAI Profile** — Art. 25 ↔ `GAI-4` (data privacy).
- **ISO/IEC 42001** — Art. 25 ↔ `A.7` (Data for AI). Art. 30
  (records of processing) ↔ `A.6.2.7` (event logs). Arts. 44-49 ↔
  `A.6.2.4` (deployment criteria).

See [Coverage matrix](../coverage-matrix.md) for full bindings.

## Citations

- Regulation (EU) 2016/679 — https://eur-lex.europa.eu/eli/reg/2016/679/oj
- Art. 5 — principles.
- Art. 6 — lawful bases.
- Art. 9 — special categories.
- Art. 17 — right to erasure.
- Art. 22 — automated individual decision-making.
- Art. 25 — data protection by design and by default.
- Art. 30 — records of processing activities.
- Art. 33 — breach notification.
- Arts. 44–49 — international transfers.
- EDPB guidance — https://edpb.europa.eu/edpb_en
