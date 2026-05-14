# Regulus

**Where Google ADK ends, regulated builds begin.**

Google ADK ships AI agents. Regulus ships AI agents your regulator
accepts.

---

## 60s · 5min · 15min

```
60s   regulus init my-agent --profiles=eu-ai-act,uk-gdpr,fca-sysc
                            --frameworks=nist-ai-rmf,iso-42001

5min  cd my-agent && gradle wrapper && ./gradlew bootRun

15min hit /chat → see policy + privacy + audit + GRC envelope
```

→ [**Install the CLI**](getting-started/install-cli.md) to make this real.

→ [**Why Regulus**](why-regulus.md) for the full story.

→ [**Show me — the diff**](show-me.md) for the audit-event proof.

---

## Choose your path

| You are… | Start here |
|---|---|
| **An engineer** new to Regulus | [Why Regulus](why-regulus.md) → [Show me](show-me.md) → [Install the CLI](getting-started/install-cli.md) |
| **A governance leader** (CISO / CAIO / CRO / 2L / 3L) | [Governance overview](governance/index.md) → [Three Lines of Defence](governance/three-lines/index.md) → [GRC integration](governance/grc/index.md) |
| **Preparing for ISO 42001 certification** | [ISO/IEC 42001](governance/frameworks/iso-42001.md) → [Audit walkthrough](compliance/audit-walkthrough.md) → [Program operating model](governance/program-operating-model.md) |
| **New to regulatory vocabulary** | [What is regtech?](concepts/regtech-intro.md) → [What is AI governance?](concepts/ai-governance-intro.md) → [Glossary](concepts/glossary.md) |

## What Regulus is — and isn't

**Is:** A `BasePlugin` suite + service extensions + compliance profiles +
governance frameworks + GRC adapters for ADK 1.x. Pure Java; Spring Boot
starter optional. Distributes via Maven Central, Gradle Plugin Portal,
and a single-binary CLI.

**Isn't:** A replacement for ADK, a no-code agent builder, an LLM
provider, a legal opinion, or a substitute for your DPO / SMF holder /
clinical safety officer.

## What it costs you *not* to use Regulus

A rough table for a senior backend engineer with no prior regtech, no
existing tooling. The point isn't the precision — it's the **visibility
of the unbuilt cost**.

| Control | Build it yourself | Regulus |
|---|---|---|
| PII redaction + tests + audit | ~3 engineer-weeks | One plugin |
| Dual-control kill switch | ~4 engineer-weeks | One plugin |
| Audit pipeline + retention + erasure | ~6 engineer-weeks | One plugin + compactor |
| Residency proof + fail-closed startup | ~2 engineer-weeks | One plugin |
| EU AI Act Annex III classification | ~5 engineer-weeks | One plugin |
| NIST AI RMF / 600-1 control mapping | ~6 program-weeks | One framework binding |
| ISO 42001 Statement of Applicability | ~3 program-weeks per cycle | Generated artefact |
| GRC tool integration (any one vendor) | ~4-8 engineer-weeks | One adapter |

Full breakdown at [Compliance → Time saved](compliance/time-saved.md).

## Where to read next

- New to regtech? [What is regtech?](concepts/regtech-intro.md)
- New to ADK? [ADK quickstart](getting-started/adk-quickstart.md)
- Want the regulation map? [EU vs UK landscape](concepts/eu-uk-landscape.md)
- Want the plugin reference? [Plugins overview](plugins/index.md)
- Want the regulation reference? [Compliance overview](compliance/index.md)
- Want the governance reference? [Governance overview](governance/index.md)
