# Regulus

[![Java 21](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/projects/jdk/21/)
[![ADK 1.2.0](https://img.shields.io/badge/Google%20ADK-1.2.0-blue)](https://github.com/google/adk-java)
[![Maven Central](https://img.shields.io/maven-central/v/com.regulus.platform/regulus-ai-adk-plugins.svg)](https://central.sonatype.com/namespace/com.regulus.platform)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/com.regulus.compliance)](https://plugins.gradle.org/plugin/com.regulus.compliance)
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
curl -fsSL https://regulus.neullabs.com/install.sh | sh

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
  "smf_holder": "SMF24:Jane Smith",
  "action": "model-call",
  "result": "allow",
  "model_id": "gemini-2.5-flash",
  "regulation_clause": "UK GDPR Art. 25",
  "framework_control_id": "A.7.3",
  "ai_act_risk_tier": "limited",
  "consumer_duty_outcome": "support",
  "redactions": ["NINO_1"]
}
```

That JSON has the regulation citation, the ISO 42001 control id, the
SMF attribution, the redactions, and the outcome — all in one event. Your
2L attests from it. Your 3L reproduces it. Your DPO answers their SAR
from it. None of which works one hour ago.

## What you get

- **6 ADK `BasePlugin`s** — policy, privacy, audit, kill switch, model risk, residency.
- **6 ADK service extensions** — Vertex + Firestore sessions/memory, GCS artifact, retention compactor, computer-use, plus A2A envelope.
- **10 regulation profiles** — EU AI Act, GDPR, UK GDPR, DORA, NIS2, FCA SYSC, PRA SS1/23 + SS2/21, NHS DSPT, EHDS.
- **6 governance frameworks** — NIST AI RMF + 600-1 GenAI Profile + planned Q4 2026 Agent Interop Profile, ISO/IEC 42001 (with SoA generator), ISO/IEC 23894, ISO/IEC 23053.
- **4 GRC adapters** — ServiceNow IRM, OneTrust AI Governance, MetricStream, generic HMAC-signed webhook.
- **CLI + Gradle plugin** — scaffold, doctor, compliance scan, coverage matrix.

Full mapping (regulation × framework × control × ADK hook) at the
[coverage matrix](https://docs.neullabs.com/compliance/coverage-matrix/).

## Choose your path

| You are… | Start here |
|---|---|
| **An engineer** new to Regulus | [Why Regulus](https://docs.neullabs.com/why-regulus/) → [Show me](https://docs.neullabs.com/show-me/) → [Install the CLI](https://docs.neullabs.com/getting-started/install-cli/) |
| **A governance leader** (CISO / CAIO / CRO / 2L / 3L) | [Governance overview](https://docs.neullabs.com/governance/) → [Three Lines of Defence](https://docs.neullabs.com/governance/three-lines/) → [GRC integration](https://docs.neullabs.com/governance/grc/) |
| **Preparing for ISO 42001 certification** | [ISO/IEC 42001](https://docs.neullabs.com/governance/frameworks/iso-42001/) → [Audit walkthrough](https://docs.neullabs.com/compliance/audit-walkthrough/) → [Program operating model](https://docs.neullabs.com/governance/program-operating-model/) |
| **New to regulatory vocabulary** | [Concepts → What is regtech?](https://docs.neullabs.com/concepts/regtech-intro/) → [Concepts → What is AI governance?](https://docs.neullabs.com/concepts/ai-governance-intro/) → [Glossary](https://docs.neullabs.com/concepts/glossary/) |

## How it plugs into ADK

Every Regulus control is a `com.google.adk.plugins.BasePlugin`. Built on
ADK's official extension contract — not Spring AOP, not bytecode
rewriting:

| ADK seam | Regulus implementation |
|---|---|
| `BeforeAgentCallback` | `RegulusKillSwitchPlugin`, `RegulusDataResidencyPlugin` |
| `BeforeModelCallback` | `RegulusPolicyPlugin`, `RegulusPrivacyPlugin` (mutating), `RegulusModelRiskPlugin` |
| `AfterModelCallback` | `RegulusPrivacyPlugin` (re-redact), `RegulusAuditPlugin` |
| `BeforeToolCallback` | `RegulusPolicyPlugin`, `RegulusModelRiskPlugin` (for code executors) |
| `ToolConfirmation` | Kill-switch dual control, vulnerable-customer HITL, Art. 22 safeguards |
| `EventCompactor` | `RegulusRetentionEventCompactor` (regulation-aware retention) |
| `SessionService` / `MemoryService` / `ArtifactService` | `Regulus*` variants with residency at construction |
| A2A `RemoteA2AAgent` / `AgentExecutor` | `regulus-ai-adk-a2a` envelope |
| `BaseComputer` | `RegulusComplianceBaseComputer` (Google flagged as needs-impl) |

`ToolConfirmation` is Google's HITL primitive. Regulus' dual control uses
exactly that mechanism — same shape, no special-case API for users to
learn.

## Distribution

- **Maven Central** — `com.regulus.platform:*`.
- **Gradle Plugin Portal** — `com.regulus.compliance`.
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
