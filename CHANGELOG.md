# Changelog

All notable changes to Regulus land here. Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
versioning follows [SemVer](https://semver.org/spec/v2.0.0.html). Until `1.0.0`, the
API may change between minor versions.

## [Unreleased]

## [0.2.1] — 2026-05-26

Patch release fixing two CI-breakers discovered after the `v0.2.0` merge.

### Fixed

- **CLI audit verify** — `SealedAuditEvent` contains `Optional<String>` for the
  per-event signature slot; Jackson needs `jackson-datatype-jdk8` to
  (de)serialize it. Added the dependency to `regulus-cli` and
  `regulus-ai-observability`, and registered `Jdk8Module` in
  `AuditVerifyCommand` and its test.
- **OIDC starter test compilation** — `OidcIdentityEndToEndTest` called
  `assertThat(Object).contains(...)` on a map value; AssertJ has no such
  overload. Cast the value to `String` before the chain.

## [0.2.0] — 2026-05-23

The **enterprise security plane** release. Regulus now ships a canonical
identity model that OIDC, SAML, mTLS, and service-account JWTs plug into
as adapters; tamper-evident audit chains with an offline verifier; RFC
9421 HTTP Message Signing for cross-org A2A calls; and identity-backed
dual control. One substrate, regulator-shaped from the inbound JWT all
the way to the GRC sink.

### Added — Canonical identity plane

- **`regulus-ai-identity`** — new leaf module (zero Spring, zero AOP)
  with the canonical primitive every internal path consumes:
  - `Principal(id, displayName, type {HUMAN, SERVICE, AGENT})`
  - `Claims(tenantId, jurisdiction, purposeCodes, roles, lawfulBases, extensions)`
  - `Identity(principal, claims, Provenance(adapterId, mintedAt, tokenExpiry, tokenIssuer))`
  - `IdentityAdapter` SPI + `RequestContext` + `AuthenticationException`
  - `IdentityHolder` thread-local for request-scoped propagation
  - `KeyProvider` + `SigningKey` / `VerificationKey` sealed handles
    (shared with A2A signing and audit integrity)
- **`regulus-ai-identity-bridge`** — `PolicyContextBridge` derives both
  legacy `PolicyContext` shapes (`policy.model` + `adk.plugins`) from an
  `Identity`. Single chokepoint so the rest of the codebase reads one
  source of truth.
- **`regulus-ai-identity-oidc-spring-boot-starter`** — reference OIDC
  adapter. Maps Spring Security `JwtAuthenticationToken` → `Identity`
  (`sub`, `regulus.tenant` w/ `tid` fallback, `regulus.jurisdiction`,
  `regulus.purpose`, `regulus.lawful_basis`, `scope` ∪ `roles` ∪
  `realm_access.roles`, pass-through extensions). `OidcSecurityContextFilter`
  binds the Identity to `IdentityHolder` per request. Spring Security is
  `compileOnly` — non-OIDC tenants don't pay for it.

### Added — A2A request signing (RFC 9421)

- **`com.neullabs.regulus.adk.a2a.signing`** package: `A2ARequestSigner`
  SPI, `A2AEnvelope`, `SignedEnvelope`, `VerifiedCaller`,
  `SignatureException`.
- **`HttpMessageSignatureSigner`** — RFC 9421 skeleton with Ed25519
  over canonicalised `@method`, `@target-uri`, `content-digest`,
  `regulus-tenant`, `regulus-correlation-id`, `regulus-identity-adapter`.
  Replay protection: nonce + 5-minute timestamp window (configurable).
  The actual canonicalisation/sign body is the next milestone — the SPI
  contract is locked.
- `RegulusRemoteA2AAgent` and `RegulusAgentExecutor` now take
  `Optional<A2ARequestSigner>` instead of an unimplemented boolean
  `signRequests` flag. The inbound verification filter places the
  verified caller's `Identity` into `IdentityHolder` before any policy
  guard runs.

### Added — Opt-in audit integrity

- **`audit.integrity`** package in `regulus-ai-observability`:
  `AuditChain` SPI, `SealedAuditEvent(event, chainIndex,
  previousEventHash, eventHash, signature, keyId)`, default
  `HashChainAuditChain` (SHA-256 over canonical JSON with sorted map
  keys; thread-safe atomic-ref to the previous hash). Per-event
  signature slot ready for the `KeyProvider` integration.
- `AuditLogger` accepts an optional `AuditChain`; when present, every
  event is sealed before sink fan-out. `AuditSink` gains a default
  `writeSealed(SealedAuditEvent)` so existing sinks keep working
  unchanged. The default logging sink writes the sealed wrapper so the
  log line itself is the verifiable record.
- `ObservabilityProperties.AuditConfig.IntegrityConfig` —
  `regulus.ai.observability.audit.integrity.enabled` and `keyId`.
- Auto-configuration registers the `AuditChain` bean when the flag is
  set and wires it into the existing `AuditLogger` bean.

### Added — Identity-backed kill-switch authorization

- **`killswitch.authz`** package: `KillSwitchAuthorizer` SPI +
  `RoleBasedKillSwitchAuthorizer` with canonical default roles
  (`regulus.killswitch.requester`, `.approver`, `.emergency`) and
  constructor for tenant-configurable names.
- `DualControlKillSwitch` gains `Identity` overloads for
  `requestGlobalActivation` and `approve`. Approver-distinctness is now
  enforced on `Principal.id`, not on opaque strings — same human under
  two typed display names cannot defeat 4-eyes. The String overloads
  remain functional but are `@Deprecated`.

### Added — Token expiry enforcement

- **`RegulusIdentityExpiryGuard`** — ADK `BasePlugin` (BeforeModel),
  registered first in the chain. Reads `IdentityHolder`; returns
  `PolicyDecision.Block` with code `regulus.identity.token_expired` when
  `Provenance.tokenExpiry` is past. Absent-Identity is permitted so
  non-policy callers (audit-log enrichment reading historical Identity)
  are not broken. Injectable `Clock` for tests.

### Added — CLI: `regulus audit verify`

- `regulus audit verify <chain.jsonl>` — auditor-facing offline
  verifier. Parses a JSONL file of `SealedAuditEvent` records and
  validates contiguous chain indexes, predecessor hashes, and
  recomputed event hashes. Exit codes: `0` intact, `1` tampered, `2`
  I/O / parse error. Pulls in `regulus-ai-observability` with Spring
  transitives excluded to keep the shadow jar small.

### Added — Documentation

- **`concepts/security-model.md`** — narrative-first entrance ramp on
  Regulus' canonical identity primitive, adapter pattern, trust
  boundaries, and what Regulus refuses to do. Cites GDPR Art. 5(1)(b),
  Art. 30, EU AI Act Art. 12 for the regulatory anchor.
- **`advanced/security-architecture.md`** — full threat model, identity
  contract with field-by-field regulatory-anchor table, `IdentityAdapter`
  SPI walkthrough with SAML adapter example, OIDC claim-mapping table,
  Mermaid trust-boundary diagram (Caller → Spring Boot → ADK runtime →
  Plugins → {External IdP, GRC sinks, Remote A2A}), A2A signing scheme,
  audit integrity construction, kill-switch authorization, failure-modes
  table, out-of-scope list.
- New top-level **Advanced** nav section in `mkdocs.yml` (also rescues
  four previously-orphaned `advanced/*.md` files: custom-policy-guards,
  custom-privacy-filters, dsl-configuration, production-hardening).
- Cross-references added on `concepts/audit-trails.md`,
  `concepts/dual-control.md`, `concepts/controller-processor-deployer.md`,
  `concepts/index.md`, `advanced/production-hardening.md`,
  `guides/integration/a2a-protocol.md`, `compliance/eu/eu-ai-act.md`
  (Art. 12 + Art. 15).

### Added — Enterprise messaging across the funnel

- **README** auditor JSON sample now shows `tenant_id`, `jurisdiction`,
  `identity_adapter`, `chain_index`, `prev_event_hash`, `event_hash`.
  New "Built for regulated enterprises" section covers SSO out-of-the-box,
  multi-tenant + multi-jurisdiction, tamper-evident audit, signed
  cross-org A2A, identity-backed dual control, threat-model link. ADK
  seam table gains the inbound HTTP / Spring SecurityContext row and the
  `RegulusIdentityExpiryGuard`. "Choose your path" gains a
  security-architect / enterprise-IT row.
- **`why-regulus.md`** leads its "What Google ADK leaves to you" list
  with the enterprise identity plane; A2A bullet mentions RFC 9421.
- **`show-me.md`** explains the JWT → Identity → PolicyContext flow on
  the inbound side and surfaces the new audit-event fields.
- **`docs/index.md`** cost-table adds three new lines (SSO → identity
  model, audit chain + offline verifier, RFC 9421 A2A signing) and the
  enterprise-IT audience row.

### Changed

- **Breaking — A2A constructors.** `RegulusRemoteA2AAgent` and
  `RegulusAgentExecutor` now take `Optional<A2ARequestSigner>` (or
  the no-signer convenience constructor) instead of the previous
  unimplemented `boolean signRequests` flag. Migration: replace
  `new RegulusRemoteA2AAgent(endpoint, sink, false)` with
  `new RegulusRemoteA2AAgent(endpoint, sink)`; replace `..., true`
  with `..., signer` where `signer` is your `A2ARequestSigner`.
- **`Jurisdiction` enum moved.** `com.neullabs.regulus.compliance.Jurisdiction`
  has been migrated to `com.neullabs.regulus.identity.Jurisdiction`.
  All ten shipped compliance profiles + `CompositeComplianceProfile`
  + `ComplianceProfile.jurisdiction()` updated. The compliance module
  now depends on `regulus-ai-identity`. Callers using the old import
  must update; no other behavioural change.
- **`com.neullabs.regulus.adk.plugins.PolicyContext` is now `@Deprecated`.**
  New code should build a `com.neullabs.regulus.policy.model.PolicyContext`
  from an `Identity` via `PolicyContextBridge`. The deprecated record
  remains functional through 0.2.x.

### Tests

- 33 new test methods across 9 test files: identity holder + expiry
  + claim-copy defensive, bridge mapping (both PolicyContext shapes),
  A2A signer stub round-trip, audit chain append/tamper/reorder/empty,
  kill-switch role gating + custom role names, expiry-guard
  absent/live/null-expiry/expired paths, OIDC claim mapping with
  missing-claim rejection + realm_access merge + extensions
  pass-through, CLI `audit verify` exit codes, end-to-end JWT →
  Identity → PolicyContext.

### Known limitations

- `HttpMessageSignatureSigner.sign()` / `.verify()` throw
  `UnsupportedOperationException` / `SignatureException` until the
  next milestone — the SPI surface and replay-protection plumbing are
  stable; the canonicalisation + Ed25519 body lands separately.
- Per-event audit signature population through `KeyProvider` is wired
  but not finalised. The chain's hash integrity is complete and
  verifiable today; per-event signatures are the next step.
- `RegulusIdentityExpiryGuard` is not yet auto-wired into
  `RegulusAdkAutoConfiguration` alongside the other plugins —
  callers must register it explicitly until the broader plugin
  auto-config refresh.
- A full Spring Boot integration test with an embedded OAuth2
  resource server is on the next-milestone list; the included
  end-to-end test exercises the JWT → Identity → PolicyContext path
  without booting a Spring context.

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

[Unreleased]: https://github.com/neul-labs/regulus/compare/v0.2.1...HEAD
[0.2.1]: https://github.com/neul-labs/regulus/releases/tag/v0.2.1
[0.2.0]: https://github.com/neul-labs/regulus/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/neul-labs/regulus/releases/tag/v0.1.0
