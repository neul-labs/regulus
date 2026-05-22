# Regulus

[![Java 21](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/projects/jdk/21/)
[![ADK 1.2.0](https://img.shields.io/badge/Google%20ADK-1.2.0-blue)](https://github.com/google/adk-java)
[![Maven Central](https://img.shields.io/maven-central/v/com.neullabs/regulus-ai-adk-plugins.svg)](https://central.sonatype.com/namespace/com.neullabs)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/com.neullabs.compliance)](https://plugins.gradle.org/plugin/com.neullabs.compliance)
[![Docs](https://img.shields.io/badge/docs-docs.neullabs.com-blueviolet)](https://docs.neullabs.com)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

# Where Google ADK ends, regulated builds begin.

Google ADK ships AI agents. **Regulus ships AI agents your regulator
accepts.**

---

## 60s · 5min · 15min

```
┌─────────────────────────────────────────────────────────────────────┐
│                                                                     │
│   60s   regulus init my-agent --profiles=eu-ai-act,uk-gdpr,fca-sysc │
│                                --frameworks=nist-ai-rmf,iso-42001   │
│                                                                     │
│   5min  cd my-agent && gradle wrapper && ./gradlew bootRun          │
│                                                                     │
│   15min hit /chat → see policy + privacy + audit + GRC envelope     │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

That's the funnel. Three checkpoints, no slides.

## 60s — scaffold

```bash
# Install the CLI:
curl -fsSL https://raw.githubusercontent.com/neul-labs/regulus/main/install.sh | sh

# Scaffold a compliant ADK agent:
regulus init my-agent \
    --profiles=eu-ai-act,uk-gdpr,fca-sysc \
    --frameworks=nist-ai-rmf,iso-42001 \
    --grc-adapter=stdout
```

Output:

```
✓ created my-agent/ with 12 files
  build.gradle.kts · settings.gradle.kts · gradle.properties · .gitignore
  README.md · gradlew · gradlew.bat
  src/main/java/com/example/agent/{AgentApplication.java, ChatController.java}
  src/main/resources/{application.yaml, logback.xml}

Next: cd my-agent && gradle wrapper && ./gradlew bootRun
```

Don't want to install a CLI? Same thing through Gradle:

```bash
./gradlew initRegulusAgent -PagentName=my-agent \
    -Pprofiles=eu-ai-act,uk-gdpr,fca-sysc \
    -Pframeworks=nist-ai-rmf,iso-42001
```

## The gap, in one paragraph

ADK ships a capable AI agent runtime. **It doesn't ship the audit trail
your auditor demands, the retention schedule your DPO signs off on, the
kill switch your runbook exercises, the model-risk tier your second line
assesses, or the framework-mapped evidence your GRC tool catalogues.**
Writing those properly is a quarter of engineering time. Writing them
badly is worse than not doing it at all — a bad audit trail is a
discoverable artefact in an enforcement action. Regulus is the bridge.

[**→ Why Regulus** — the full version of this story](https://docs.neullabs.com/why-regulus/)

## Before / after

Plain ADK — works, but produces no audit trail:

```java
@SpringBootApplication
public class App {
    public static void main(String[] args) { SpringApplication.run(App.class, args); }
    LlmAgent rootAgent() {
        return LlmAgent.builder().name("greeter").model("gemini-2.5-flash").build();
    }
}
```

ADK + Regulus — same agent, with policy + privacy + audit + kill switch +
model risk + residency + framework-mapped GRC evidence:

```java
@SpringBootApplication
public class App {
    public static void main(String[] args) { SpringApplication.run(App.class, args); }
    // Regulus plugins auto-register via application.yaml. No additional code.
}
```

```yaml
regulus:
  compliance:
    profiles: [eu-ai-act, uk-gdpr, fca-sysc]
  governance:
    frameworks: [nist-ai-rmf, iso-42001]
  grc:
    stdout: true
  adk:
    residency: { allowed-regions: [europe-west2] }
    kill-switch: { enabled: true, dual-control: true }
    model-risk:  { tenant-tier: STANDARD }
```

[**→ Show me — the diff** with audit-event sample](https://docs.neullabs.com/show-me/)

## What the auditor sees

```json
{
  "event_id": "01J6X4ABCDEFG",
  "occurred_at": "2026-05-14T11:23:09.123Z",
  "actor": "user:42",
  "tenant_id": "acme-bank",
  "jurisdiction": "EU_UK",
  "identity_adapter": "oidc",
  "smf_holder": "SMF24:Jane Smith",
  "action": "model-call",
  "result": "allow",
  "model_id": "gemini-2.5-flash",
  "regulation_clause": "UK GDPR Art. 25",
  "framework_control_id": "A.7.3",
  "ai_act_risk_tier": "limited",
  "consumer_duty_outcome": "support",
  "redactions": ["NINO_1"],
  "chain_index": 1284,
  "prev_event_hash": "9f3e…",
  "event_hash": "1c87…"
}
```

That JSON has the regulation citation, the ISO 42001 control id, the
SMF attribution, the redactions, the outcome — **plus the tenant, the
jurisdiction, the IdP adapter that authenticated the caller, and the
hash chain that makes the trail tamper-evident** — all in one event.
Your 2L attests from it. Your 3L reproduces it. Your DPO answers their
SAR from it. Your security architect verifies the chain offline with
`regulus audit verify`. None of which works one hour ago.

## What you get

- **Canonical identity plane** — one `Principal` + `Claims` shape; OIDC adapter included, SAML / mTLS / service-account JWT via the `IdentityAdapter` SPI.
- **6 ADK `BasePlugin`s** — policy, privacy, audit, kill switch, model risk, residency. (Plus a leading `RegulusIdentityExpiryGuard` for token-expiry enforcement.)
- **6 ADK service extensions** — Vertex + Firestore sessions/memory, GCS artifact, retention compactor, computer-use, plus A2A envelope with RFC 9421 HTTP Message Signatures for cross-org calls.
- **Opt-in audit integrity** — SHA-256 hash chain over every event, optional per-event signature, offline verifier (`regulus audit verify <chain.jsonl>`).
- **10 regulation profiles** — EU AI Act, GDPR, UK GDPR, DORA, NIS2, FCA SYSC, PRA SS1/23 + SS2/21, NHS DSPT, EHDS.
- **6 governance frameworks** — NIST AI RMF + 600-1 GenAI Profile + planned Q4 2026 Agent Interop Profile, ISO/IEC 42001 (with SoA generator), ISO/IEC 23894, ISO/IEC 23053.
- **4 GRC adapters** — ServiceNow IRM, OneTrust AI Governance, MetricStream, generic HMAC-signed webhook.
- **CLI + Gradle plugin** — scaffold, doctor, compliance scan, coverage matrix, audit verify.

Full mapping (regulation × framework × control × ADK hook) at the
[coverage matrix](https://docs.neullabs.com/compliance/coverage-matrix/).

## Built for regulated enterprises

Every choice in the platform anticipates the questions a CISO, a head of
internal audit, or an external assessor will ask on day one.

- **Enterprise SSO from day one.** Your IdP — Okta, Auth0, Keycloak,
  ADFS, an in-house mTLS scheme — plugs in as an `IdentityAdapter` that
  mints a canonical `Identity`. OIDC ships out of the box; SAML and
  mTLS adapters are tens of lines. Regulus refuses to be your IdP — it
  consumes the result.
- **Multi-tenant + multi-jurisdiction by design.** `tenantId` and
  `jurisdiction` are first-class claims on every audit event and every
  policy decision. The same deployment handles EU-only traffic,
  UK-only traffic, and EU+UK composite tenants without code changes.
- **Tamper-evident audit trail.** Opt-in `regulus.ai.observability.audit.integrity.enabled=true`
  switches on a SHA-256 hash chain. Auditors verify the chain offline
  against a copy of the log; mutation, reorder, or gaps fail
  verification.
- **Signed cross-org A2A calls.** When agents from different
  organisations collaborate, outbound JSON-RPC envelopes are signed
  with RFC 9421 HTTP Message Signatures over method, target URI, body
  digest, tenant id, and correlation id. Replay protection via nonce
  + timestamp window. The inbound side reconstructs the caller's
  Identity from the verified envelope before any policy guard runs.
- **Identity-backed dual control.** Kill-switch activation and
  approval gate on `Identity` roles (`regulus.killswitch.requester /
  .approver / .emergency`), with approver-distinctness enforced on
  `Principal.id` so two distinct subjects are required — not two
  distinct typed names.
- **Clear security model + threat model.** What Regulus defends
  against, what it doesn't, where the trust boundaries are, what
  happens when each one breaks — all documented at
  [Security architecture](https://docs.neullabs.com/advanced/security-architecture/).

The architecture is one canonical primitive with replaceable adapters,
not a grab-bag of per-protocol code paths. That is what keeps the
compliance story coherent as the protocol mix shifts under you.

## Choose your path

| You are… | Start here |
|---|---|
| **An engineer** new to Regulus | [Why Regulus](https://docs.neullabs.com/why-regulus/) → [Show me](https://docs.neullabs.com/show-me/) → [Install the CLI](https://docs.neullabs.com/getting-started/install-cli/) |
| **A security architect / enterprise IT** | [Security model](https://docs.neullabs.com/concepts/security-model/) → [Security architecture](https://docs.neullabs.com/advanced/security-architecture/) → [Production hardening](https://docs.neullabs.com/advanced/production-hardening/) |
| **A governance leader** (CISO / CAIO / CRO / 2L / 3L) | [Governance overview](https://docs.neullabs.com/governance/) → [Three Lines of Defence](https://docs.neullabs.com/governance/three-lines/) → [GRC integration](https://docs.neullabs.com/governance/grc/) |
| **Preparing for ISO 42001 certification** | [ISO/IEC 42001](https://docs.neullabs.com/governance/frameworks/iso-42001/) → [Audit walkthrough](https://docs.neullabs.com/compliance/audit-walkthrough/) → [Program operating model](https://docs.neullabs.com/governance/program-operating-model/) |
| **New to regulatory vocabulary** | [Concepts → What is regtech?](https://docs.neullabs.com/concepts/regtech-intro/) → [Concepts → What is AI governance?](https://docs.neullabs.com/concepts/ai-governance-intro/) → [Glossary](https://docs.neullabs.com/concepts/glossary/) |

## How it plugs into ADK

Every Regulus control is a `com.google.adk.plugins.BasePlugin`. Built on
ADK's official extension contract — not Spring AOP, not bytecode
rewriting:

| ADK seam | Regulus implementation |
|---|---|
| Inbound HTTP / Spring SecurityContext | `OidcSecurityContextFilter` → `IdentityAdapter` → `IdentityHolder` (canonical Identity bound before any callback fires) |
| `BeforeAgentCallback` | `RegulusKillSwitchPlugin`, `RegulusDataResidencyPlugin` |
| `BeforeModelCallback` | `RegulusIdentityExpiryGuard` (first), `RegulusPolicyPlugin`, `RegulusPrivacyPlugin` (mutating), `RegulusModelRiskPlugin` |
| `AfterModelCallback` | `RegulusPrivacyPlugin` (re-redact), `RegulusAuditPlugin` (chain-sealed when integrity enabled) |
| `BeforeToolCallback` | `RegulusPolicyPlugin`, `RegulusModelRiskPlugin` (for code executors) |
| `ToolConfirmation` | Kill-switch dual control (Identity-gated), vulnerable-customer HITL, Art. 22 safeguards |
| `EventCompactor` | `RegulusRetentionEventCompactor` (regulation-aware retention) |
| `SessionService` / `MemoryService` / `ArtifactService` | `Regulus*` variants with residency at construction |
| A2A `RemoteA2AAgent` / `AgentExecutor` | `regulus-ai-adk-a2a` envelope with `A2ARequestSigner` (RFC 9421) for cross-org calls |
| `BaseComputer` | `RegulusComplianceBaseComputer` (Google flagged as needs-impl) |

`ToolConfirmation` is Google's HITL primitive. Regulus' dual control uses
exactly that mechanism — same shape, no special-case API for users to
learn.

## Distribution

- **Maven Central** — `com.neullabs:*`.
- **Gradle Plugin Portal** — `com.neullabs.compliance`.
- **GitHub Releases** — `regulus-cli.jar`.
- **GitHub Container Registry** — `ghcr.io/neul-labs/regulus-adk-demo`.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). New controls ship as `BasePlugin`
implementations; compliance docs follow the
[regtech-explainer template](docs/decisions/ADR-009-regtech-as-product-docs.md).

## License

[MIT](LICENSE)

---

Built to ADK's official extension contract. Not endorsed by Google — we
picked the seams they ship.

Shipped 20 March 2026, ten days ahead of ADK Java 1.0 GA. Tracking ADK
releases since.
