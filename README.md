# Regulus

[![Java 21](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/projects/jdk/21/)
[![ADK 1.2.0](https://img.shields.io/badge/Google%20ADK-1.2.0-blue)](https://github.com/google/adk-java)
[![Spring Boot 3.3](https://img.shields.io/badge/Spring%20Boot-3.3-green)](https://spring.io/projects/spring-boot)
[![Maven Central](https://img.shields.io/maven-central/v/com.regulus.platform/regulus-ai-adk-plugins.svg)](https://central.sonatype.com/namespace/com.regulus.platform)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/com.regulus.compliance)](https://plugins.gradle.org/plugin/com.regulus.compliance)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**The EU & UK compliance plane for Google ADK.**

Drop-in plugins and service extensions for policy guards, PII redaction,
dual-control HITL, regulation-aware retention, residency-pinned sessions /
memory / artifacts, and immutable audit. Built on ADK 1.x's official
`BasePlugin` and service interfaces — so the Regulus surface is the same shape
as the rest of your ADK code.

> Shipped 20 March 2026, ten days ahead of ADK Java 1.0 GA, and tracking ADK
> releases since.

## What it is, in one paragraph

If your AI agent touches an EU or UK customer, **at least one** of GDPR, UK
GDPR, the EU AI Act, DORA, NIS2, FCA SYSC, PRA SS1/23, PRA SS2/21, Consumer
Duty, NHS DSPT, or EHDS applies to you. Each comes with concrete engineering
demands — purpose binding, PII handling, audit retention, residency, model
risk tiering, human oversight, kill switches. Regulus turns each into a YAML
profile and a `BasePlugin` you add to your ADK `App`. You write the agent; we
keep the regulator's hand off the keyboard.

## Quick start

`build.gradle.kts`:

```kotlin
dependencies {
    implementation(platform("com.regulus.platform:regulus-ai-bom:0.1.0"))
    implementation("com.google.adk:google-adk:1.2.0")
    implementation("com.regulus.platform:regulus-ai-adk-spring-boot-starter")
}
```

`application.yaml`:

```yaml
regulus:
  compliance:
    profiles: [eu-ai-act, uk-gdpr, fca-sysc]
  adk:
    name: my-agent
    session-service:
      kind: vertex-ai
      project-id: ${GOOGLE_CLOUD_PROJECT}
      location: europe-west2
    audit:
      sink: kafka
      kafka-topic: audit.regulus.v1
    residency:
      allowed-regions: [europe-west2]
    model-risk:
      tenant-tier: STANDARD
```

That's it. Your ADK agent now redacts PII before it reaches the model,
enforces purpose binding and consent on every request, requires dual-control
to deactivate the kill switch, blocks any service that isn't in `europe-west2`
**at startup**, and emits a structured audit event auditors actually accept.
No interceptors, no Spring AOP, no surprises.

Prefer to wire plugins directly without Spring? Same six plugins, same shape:

```java
App app = App.builder("my-agent", rootAgent)
    .plugin(RegulusPolicyPlugin.fromProfile(profile))
    .plugin(RegulusPrivacyPlugin.withPatterns(NINO, IBAN, BIC, SORT_CODE).build())
    .plugin(RegulusKillSwitchPlugin.dualControl())
    .plugin(RegulusAuditPlugin.forProfile(profile).toKafka("audit.regulus.v1").build())
    .plugin(RegulusDataResidencyPlugin.allow("europe-west2"))
    .plugin(RegulusModelRiskPlugin.tier(Tier.STANDARD))
    .build();
```

## What Regulus saves you

Honest estimates, senior backend engineer, no prior regtech, no existing
tooling. Full table at
[`time-saved.md`](documentation/docs/compliance/time-saved.md).

| Control | Build it yourself | Regulus |
|---|---|---|
| PII redaction (NINO/IBAN/BIC/SORT_CODE + tests + audit hook) | ~3 engineer-weeks | One plugin |
| Dual-control kill switch (state + 4-eyes UI + audit) | ~4 engineer-weeks | One plugin |
| Audit pipeline + regulation-aware retention + erasure | ~6 engineer-weeks | One plugin + one compactor |
| Residency proof (allowlist + startup fail-closed + evidence export) | ~2 engineer-weeks | One plugin |
| EU AI Act Annex III classification + risk-tier registry | ~5 engineer-weeks | One plugin |

The number isn't the point — the *visibility of the unbuilt cost* is. Plus we
keep these patched as regulations evolve, so a "this stayed compliant for two
years without my team touching it" is the actual product.

## Compliance coverage

| Regulation | Jurisdiction | Profile id | Key controls |
|---|---|---|---|
| EU AI Act | EU | `eu-ai-act` | Risk tiering, logging, human oversight, transparency |
| GDPR | EU | `gdpr` | Purpose binding, redaction, audit retention, residency |
| UK GDPR + DPA 2018 | UK | `uk-gdpr` | Same as GDPR with ICO incident notification |
| DORA | EU | `dora` | ICT risk, incident classification, third-party register |
| NIS2 | EU | `nis2` | Cyber risk management, 24h/72h incident reporting |
| FCA SYSC + Consumer Duty | UK | `fca-sysc` | SMF attribution, outsourcing, 5y records, four outcomes |
| PRA SS1/23 | UK | `pra-ss1-23` | Model inventory, tiering, validation, kill-switch readiness |
| PRA SS2/21 | UK | `pra-ss2-21` | Third-party register, residency, exit plan, audit rights |
| NHS DSPT | UK | `nhs-dspt` | Personal data, staff identity, incident management |
| EHDS | EU | `ehds` | Primary/secondary use, permits, quality labels |

Full mapping (regulation × control × ADK hook × test fixture) lives at
[`coverage-matrix.md`](documentation/docs/compliance/coverage-matrix.md), and
is regenerated from `ComplianceProfile.controls()` by
`./gradlew regulusComplianceMatrix`.

## How it plugs into ADK

Every Regulus control is a `com.google.adk.plugins.BasePlugin`. We extend
ADK's official seams — not Spring AOP, not bytecode rewriting, not a parallel
runtime:

| Concern | ADK hook | Regulus class |
|---|---|---|
| Policy guards | `BeforeModelCallback`, `BeforeToolCallback` | `RegulusPolicyPlugin` |
| PII redaction | `BeforeModelCallback` (mutates), `AfterModelCallback` | `RegulusPrivacyPlugin` |
| Audit + retention | `After*Callback` + `EventCompactor` | `RegulusAuditPlugin` + `RegulusRetentionEventCompactor` |
| Kill switch / dual control | `BeforeAgentCallback` + `ToolConfirmation` | `RegulusKillSwitchPlugin` |
| Model risk tiering | `BeforeModelCallback`, `BeforeToolCallback` | `RegulusModelRiskPlugin` |
| Data residency | Startup + `BeforeAgentCallback` | `RegulusDataResidencyPlugin` |
| Compliant session/memory/artifact | Extends `*SessionService`, `*MemoryService`, `*ArtifactService` | `Regulus*` in `regulus-ai-adk-services` |
| A2A envelope | Wraps `AgentExecutor` + `RemoteA2AAgent` | `regulus-ai-adk-a2a` |
| Compliant computer use | Implements `BaseComputer` | `RegulusComplianceBaseComputer` |

`ToolConfirmation` (Google's official HITL primitive) is the same mechanism
our dual-control kill switch uses. Same shape, no special-case API for users
to learn.

## Where to start

| You are… | Start here |
|---|---|
| New to ADK | [`adk-quickstart`](examples/adk-quickstart/README.md) — 10 minutes from zero |
| New to regtech | [Concepts → What is regtech?](documentation/docs/concepts/regtech-intro.md) and the [Glossary](documentation/docs/concepts/glossary.md) |
| Architect picking controls | [Plugin reference](documentation/docs/plugins/) and the [Coverage matrix](documentation/docs/compliance/coverage-matrix.md) |
| Deploying to Vertex AI Agent Engine | [`adk-vertex-agent-engine-deploy`](examples/adk-vertex-agent-engine-deploy/README.md) |
| Bringing your own compliance profile | [Concepts → Risk tiers](documentation/docs/concepts/risk-tiers.md) + [Operations → Custom profiles](documentation/docs/operations/custom-profiles.md) |

## Project layout

```
platform/
  regulus-ai-bom/                                BOM with ADK 1.2.0 + Regulus modules
  core/
    regulus-ai-compliance/                       Profile interface + 10 regulations
    regulus-ai-adk-plugins/                      6 BasePlugin implementations
    regulus-ai-adk-services/                     6 ADK service-interface extensions
    regulus-ai-adk-a2a/                          A2A envelope
    regulus-ai-policy/ -privacy/ -kill-switch/   Mechanisms used by the plugins
    regulus-ai-llm/                              Alternative runtime (LangChain4j) — opt-in
  starters/
    regulus-ai-adk-spring-boot-starter/          Optional Spring auto-config
  gradle-plugin/
    regulus-compliance-gradle-plugin/            Build-time scan, matrix, doctor
examples/
  adk-quickstart/                                ADK + Regulus in 10 minutes
  adk-multi-agent-a2a/                           A2A envelope across two agents
  adk-vertex-agent-engine-deploy/                adk deploy to Vertex AI Agent Engine
documentation/                                   MkDocs site (docs.skelfresearch.com)
docs/                                            Internal: ADRs, architecture, agent work
```

## Distribution

- **Maven Central** — primary; `com.regulus.platform:regulus-ai-adk-plugins`,
  `regulus-ai-adk-services`, `regulus-ai-adk-a2a`,
  `regulus-ai-adk-spring-boot-starter`, `regulus-ai-compliance`,
  `regulus-ai-bom`.
- **Gradle Plugin Portal** — `com.regulus.compliance` Gradle plugin.
- **GitHub Container Registry** — reference container image for the Vertex
  Agent Engine deploy example at `ghcr.io/skelf-research/regulus-adk-demo`.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). New controls ship as `BasePlugin`
implementations; new compliance pages follow the
[regtech-explainer template](docs/decisions/ADR-009-regtech-as-product-docs.md).

## License

[MIT](LICENSE)

---

Regulus is built to ADK's official extension contract. It is not endorsed by
Google, and we make no claim to that effect — only that we picked the seams
they ship.
