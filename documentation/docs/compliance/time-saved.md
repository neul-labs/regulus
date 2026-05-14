# Time saved

What it would actually cost you to build the Regulus control set yourself,
written so you can show the table to an architect or director and have the
conversation be 60 seconds long.

## Assumptions

- **Engineer profile.** Senior backend engineer, Java + Spring Boot, no
  prior regtech background. Has shipped agents on ADK before.
- **Starting point.** Greenfield project. No existing audit pipeline, no
  pre-built PII patterns, no kill-switch primitive, no ICT register.
- **Effort definition.** Working time from "design started" to "tested and
  in production for one tenant," including: test coverage, basic docs, a
  runbook, and one round of internal review. **Excludes** the time to get
  legal sign-off, since that's not engineer time and varies wildly.
- **Compliance scope** for each row: enough to satisfy the regulator's
  technical floor and survive a first-cycle audit. Real-world deployments
  usually add 25–50% more for organisation-specific extras.
- **Ongoing cost** is mostly excluded from the table because it depends on
  the regulation. We flag it in the right-hand column.

These numbers are conservative. We've seen teams spend 2–3x as long in
practice, mostly because the regulator's interpretation moves while you're
building.

The last three rows are **program-weeks**, not engineer-weeks — work
distributed across model risk, compliance, security, and a build-side
engineer. Program-weeks are denominated in elapsed time at typical
allocation, not raw FTE-time.

## The table

| Control | Build it yourself | Regulus | Ongoing |
|---|---|---|---|
| **PII redaction** — pattern library (NINO/IBAN/BIC/SORT_CODE/UK_POSTCODE/EMAIL/NHS_NUMBER), tests, integration into LLM request path, redacted-output guarantees, audit hook | ~3 engineer-weeks | `RegulusPrivacyPlugin` — one plugin | Track new patterns (eIDAS 2.0 IDs, BIC format changes, sectoral PII), re-verify after every model change |
| **Dual-control kill switch** — state store, 4-eyes approval API, ADK `BeforeAgentCallback` integration, monotonic audit, operator UI bindings | ~4 engineer-weeks | `RegulusKillSwitchPlugin` + `InMemoryKillSwitchStore` (swap to Postgres in prod) | Quarterly rehearsals, post-incident reviews |
| **Audit pipeline + regulation-aware retention** — Kafka schema, immutability guarantees, retention windows per regulation, summarisation past retention horizon, erasure-on-request where allowed | ~6 engineer-weeks | `RegulusAuditPlugin` + `RegulusRetentionEventCompactor` | Adjust retention as regulations shift, manage storage cost |
| **Residency proof** — region allowlist, startup fail-closed, per-call validation, evidence export for auditors | ~2 engineer-weeks | `RegulusDataResidencyPlugin` | Watch for new GCP regions, recertify yearly |
| **EU AI Act Annex III classification** — risk-tier registry, per-agent classification capture, auditor evidence pack | ~5 engineer-weeks | `RegulusModelRiskPlugin` + `ModelRegistry` | Track Annex III amendments |
| **Compliant `SessionService` (Vertex AI / Firestore)** — wrap the Google-shipped service, enforce CMEK + residency at construction, audit envelope per call, GDPR Art. 17 erasure path | ~3 engineer-weeks | `RegulusVertexAiSessionService` / `RegulusFirestoreSessionService` | Track ADK service interface changes |
| **Compliant `ArtifactService`** — bucket residency + CMEK enforcement, sensitive-artifact tagging, audit envelope | ~2 engineer-weeks | `RegulusGcsArtifactService` | Same |
| **A2A envelope** — apply policy / privacy / audit / kill-switch on inbound `AgentExecutor` and outbound `RemoteA2AAgent` calls, optional request signing | ~3 engineer-weeks | `regulus-ai-adk-a2a` | Track A2A protocol evolution |
| **Compliant `BaseComputer`** — domain allowlist, screenshot PII redaction, dual-control confirmation on high-risk actions | ~5 engineer-weeks | `RegulusComplianceBaseComputer` | Track new high-risk action shapes (browser, file system) |
| **DORA incident pipeline** — RTS-shaped incident classification, 4h / 72h / 1-month notification timer, register linkage | ~5 engineer-weeks | `dora` profile + audit pipeline | Track RTS updates |
| **PRA SS1/23 model risk integration** — tier classification per model, validation evidence persistence, tier-aware policy, deactivation drill plumbing | ~6 engineer-weeks (mostly shared with kill switch + model risk + audit) | `pra-ss1-23` profile + `RegulusModelRiskPlugin` + `RegulusKillSwitchPlugin` | Track PRA Dear CEO letters |
| **NHS DSPT specifics** — NHS Number pattern, clinician identity audit attribution, IG SIRI process integration | ~3 engineer-weeks | `nhs-dspt` profile + `RegulusPrivacyPlugin` (built-in NHS_NUMBER) | Track DSPT annual cycle |
| **NIST AI RMF / 600-1 mapping** — turning controls into framework-shaped evidence, function-by-function | ~6 program-weeks (model-risk function) | `nist-ai-rmf` / `nist-ai-rmf-600-1` frameworks + binding-aware audit events | Track AI Office / NIST profile additions |
| **ISO 42001 Statement of Applicability** — Annex A control inventory + implementation status + justifications, refreshed annually | ~3 program-weeks per cycle | `StatementOfApplicability` generator from `GovernanceProgramState` | Refresh per certification cycle |
| **GRC tool integration (any one vendor)** — REST adapter, field mappings, signature / auth, retry, health-check, evidence-schema alignment | ~4–8 engineer-weeks per vendor | One of the four shipped `GrcEvidenceAdapter`s + `fieldMappings` override | Field-mapping maintenance as the vendor schema evolves |

## Where the numbers come from

These are based on:

- Observation of internal Skelf Research and partner-team builds doing each
  of the above from scratch.
- Public engineering write-ups from large financial-services firms
  describing similar primitives.
- Cross-checking against vendor roadmap timelines for adjacent products.

The point isn't precision — it's making the unbuilt cost visible. Half of
the table's value is the right-hand column: even after you build the thing,
**you have to keep building it** as the regulations evolve.

## What Regulus doesn't save you

- **Legal sign-off.** Your DPO / legal / compliance function still has to
  validate the choice of profiles, the lawful bases, the DPIA, and the
  contracts.
- **Regulator engagement.** When the FCA / PRA / ICO / AI Office wants a
  meeting, Regulus produces evidence but does not attend the meeting.
- **The non-AI parts of the same regulations.** GDPR and DORA and NIS2 cover
  much more than AI agents. We do the AI-agent slice; you handle the rest.
- **Sector knowledge.** Which Annex III category your agent falls under,
  what your Consumer Duty outcomes are, which models are material under
  SS1/23 — those are your call.

## Use this page

If you're an architect picking controls, this page is the elevator pitch.
If you're an engineer arguing for the dependency, this page is the
business case. If you're a director approving the spend, this page is the
forecast.
