# Changelog

All notable changes to Regulus land here. Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
versioning follows [SemVer](https://semver.org/spec/v2.0.0.html). Until `1.0.0`, the
API may change between minor versions.

## [Unreleased]

## [0.1.0] — 2026-05-14

First complete public release. Where Google ADK ends, regulated builds begin.

### Added — ADK extension surface

- **6 ADK `BasePlugin`s** in `platform/core/regulus-ai-adk-plugins`:
  `RegulusPolicyPlugin`, `RegulusPrivacyPlugin`, `RegulusAuditPlugin`,
  `RegulusKillSwitchPlugin`, `RegulusModelRiskPlugin`, `RegulusDataResidencyPlugin`.
- **6 ADK service extensions** in `platform/core/regulus-ai-adk-services`:
  `RegulusVertexAiSessionService`, `RegulusFirestoreSessionService`,
  `RegulusFirestoreMemoryService`, `RegulusGcsArtifactService`,
  `RegulusRetentionEventCompactor`, `RegulusComplianceBaseComputer`.
- **A2A envelope** (`regulus-ai-adk-a2a`): `RegulusAgentExecutor` wraps inbound
  JSON-RPC; `RegulusRemoteA2AAgent` wraps outbound. HMAC-signed cross-org variant.
- **Optional Spring Boot starter** (`regulus-ai-adk-spring-boot-starter`)
  auto-wires the plugin suite from `regulus.adk.*` YAML.

### Added — Compliance regulations

10 shipped `ComplianceProfile`s in `regulus-ai-compliance`, each with
retention windows, residency policy, and audit schema derived from the
regulation: `eu-ai-act`, `gdpr`, `uk-gdpr`, `dora`, `nis2`, `fca-sysc`,
`pra-ss1-23`, `pra-ss2-21`, `nhs-dspt`, `ehds`.
`CompositeComplianceProfile` picks the *stricter* setting when profiles
disagree (longest retention, smallest residency allowlist intersection,
strongest immutability).

### Added — Governance frameworks

`platform/core/regulus-ai-governance` introduces `GovernanceFramework` as a
distinct concept from `ComplianceProfile` (voluntary vs mandatory). Six
shipped implementations:

- `NistAiRmfFramework` — AI RMF 1.0 (GOVERN/MAP/MEASURE/MANAGE).
- `NistAiRmfGenAiProfile` — AI 600-1 GenAI Profile (12 GAI risks).
- `NistAiRmfAgentInteropProfile` — provisional, Q4 2026 planned NIST profile.
- `Iso42001Framework` — AIMS with Annex A inventory + `StatementOfApplicability` generator.
- `Iso23894Framework` — AI risk management.
- `Iso23053Framework` — AI/ML system framework.

`GovernanceProgramState` tracks per-control implementation status and
produces gap analyses; `StatementOfApplicability` generates the artefact
ISO 42001 certification requires.

### Added — GRC integration

`platform/core/regulus-ai-grc-adapters` introduces `GrcEvidenceAdapter` and a
canonical `GrcEvidenceEnvelope`. Six adapters:

- `ServiceNowIrmAdapter` — control-evidence table with OAuth2 or basic auth.
- `OneTrustAiGovernanceAdapter` — AI Governance module evidence intake.
- `MetricStreamAdapter` — tenant-bound mini-app intake.
- `WebhookAdapter` — generic HMAC-SHA256 signed JSON.
- `StdoutAdapter` — development.
- `KafkaAdapter` — placeholder for the Spring Kafka-backed implementation.

Each vendor adapter exposes `fieldMappings` for tenant-customised schemas.
`AdapterHealthCheck` is fail-loud at startup (consistent with ADR-008).

New ADK plugin `RegulusGovernanceEvidencePlugin` fans audit events into
configured adapters per framework binding.

### Added — CLI + Gradle plugin

- **`regulus-cli`** (`platform/cli/regulus-cli`): Picocli-based fat jar.
  `regulus init <name>` scaffolds a working ADK + Regulus project in 12
  files. `regulus doctor` sanity-checks an existing project.
- **Gradle plugin `com.neullabs.compliance`** with:
  `regulusComplianceScan` (fails build if no profile declared),
  `regulusPolicyCompile`, `regulusComplianceMatrix` (renders the matrix),
  `regulusAdkDoctor`, `initRegulusAgent` (wraps the CLI's `Scaffold`).

### Added — Documentation

~55 MkDocs pages anchored on the regtech-explainer template (ADR-009):

- 4 funnel pages: `Why Regulus`, `Show me — the diff`, `Install the CLI`,
  CLI reference.
- 11 Concepts pages (regtech intro, AI governance intro, GRC intro,
  frameworks-vs-regulations, EU/UK landscape, controller/processor/deployer,
  risk tiers, audit trails, data residency, dual control, glossary).
- 10 regulation pages (5 EU + 5 UK), each with the 12-section template and
  a Framework Mapping callout linking to NIST/ISO control IDs.
- 6 plugin pages + 7 service pages.
- 14 Governance section pages (frameworks, three-lines-of-defence, GRC
  adapters, evidence schema, program operating model).
- Coverage matrix with NIST AI RMF + ISO 42001 columns; auto-generated
  from `ComplianceProfile.controls()` + `GovernanceFramework.bindings()`.
- 3 codelab-shaped example READMEs.
- 12 ADRs (5 from launch + 7 added in 0.1.0).

### Added — Distribution

- Root `build.gradle.kts` wires `gradle-nexus-publish-plugin` 2.0.0 + per-
  module `maven-publish` + `signing`.
- `gradle.properties` declares the version property; `regulusVersion`
  override via CI.
- `.github/workflows/release.yml` runs Sonatype OSSRH publish, Gradle
  Plugin Portal publish, and Jib push to GHCR on `v*` tag.
- `install.sh` at repo root: curl-piped installer for the CLI fat jar.
  `curl -fsSL https://raw.githubusercontent.com/neul-labs/regulus/main/install.sh | sh`

### Repository conventions

- License: MIT.
- Domains: docs at `docs.neullabs.com`, marketing at `regulus.neullabs.com`,
  installer served natively from `raw.githubusercontent.com`.
- ADRs: 12 in `docs/decisions/`; ADR-006 supersedes ADR-004 (LangChain4j
  demoted to alternative runtime, not removed).
- Editorial standard: ADR-009 fixes the 12-section regtech-explainer
  template for every compliance / plugin / framework page.

### Tests

~40 smoke test classes added across the new modules — construction
correctness, binding integrity, residency validation, scaffold output,
HMAC signing, kill-switch dual-control. JUnit 5 + AssertJ; ADK runtime
required only for the plugin classes themselves (CI exercises them
against the real ADK jar).

### CI + release plumbing

- `.github/workflows/ci.yml` builds + tests on every PR and `main` push.
- `.github/workflows/release.yml` split into Tier 1 (always — GitHub
  Release + CLI jar) and Tier 3 (gated on secrets — Sonatype + Plugin
  Portal). Tier 2 (GHCR) is `continue-on-error` until the first-push
  package permissions are wired.
- `.github/workflows/nightly-adk.yml` resolves latest ADK 1.x at 03:17
  UTC daily, builds against it, files an `adk-drift` issue on
  regression.
- `regulus.adkVersion` Gradle property (default `1.2.0`) honoured by
  the BOM and example builds; `-PadkVersion=...` overrides for the
  nightly job.

### Known limitations

- ADK 1.2.0 must be resolvable from Maven Central for `./gradlew build`
  to succeed on a fresh checkout. The nightly workflow surfaces drift.
- The `RegulusGovernanceEvidencePlugin`'s adapter dispatch path is unit-
  covered for serialisation + HMAC; not exercised against live vendor
  sandboxes. Field-mapping defaults reflect documented surfaces; tenant
  schemas vary.
- The NIST AI RMF Agent Interoperability Profile (`nist-ai-rmf-agent-interop`)
  uses provisional control IDs from NIST's April 2026 concept note. Final
  IDs land when NIST publishes (target Q4 2026).
- Maven Central publication requires Sonatype OSSRH namespace verification
  for `com.neullabs` — a one-time admin task tracked outside this release.
- `docs.neullabs.com` and `regulus.neullabs.com` DNS not yet live. Manual
  jar download path documented in `getting-started/install-cli.md` works
  meanwhile.

### Migration

Greenfield release. Users coming from the LangChain4j-only path (the
demoted `regulus-ai-llm` module): see
[Reference → Migration from LangChain4j](https://docs.neullabs.com/reference/migration-langchain4j/).

[Unreleased]: https://github.com/neul-labs/regulus/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/neul-labs/regulus/releases/tag/v0.1.0
