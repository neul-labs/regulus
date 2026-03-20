# Regulus

**The EU & UK compliance plane for Google ADK.**

Drop-in plugins and service extensions for the controls your regulator, your
legal team, and your auditor want to see around an AI agent. Built on
[Google ADK 1.x](https://github.com/google/adk-java)'s official `BasePlugin`
and service interfaces.

## Five-minute orientation

If you're new to compliance / regtech terminology, **start in [Concepts](concepts/index.md)**.
The pages there teach the vocabulary (controller, processor, deployer, risk
tiers, audit trails, residency, dual control, glossary) — every page in the
Compliance section assumes you've skimmed them.

If you know Java + ADK and just want the code, **start with [Getting started → ADK quickstart](getting-started/adk-quickstart.md)**.

## What Regulus is — and isn't

**Is:** A `BasePlugin` suite + service extensions + compliance profiles for
ADK. Pure Java. Spring Boot starter optional. Distributes via Maven Central
and the Gradle Plugin Portal.

**Isn't:** A replacement for ADK, a no-code agent builder, an LLM provider, a
legal opinion, or a substitute for your DPO / SMF holder / clinical safety
officer.

## What it costs you not to use it

A rough table for senior backend engineers with no prior regtech, no existing
tooling. The point isn't the precise number — it's the **visibility of the
unbuilt cost**.

| Control | Build it yourself | Regulus |
|---|---|---|
| PII redaction patterns + tests + audit | ~3 engineer-weeks | One plugin |
| Dual-control kill switch + 4-eyes UI | ~4 engineer-weeks | One plugin |
| Audit pipeline + retention + erasure | ~6 engineer-weeks | One plugin + compactor |
| Residency proof + startup fail-closed | ~2 engineer-weeks | One plugin |
| EU AI Act Annex III classification | ~5 engineer-weeks | One plugin |

Full breakdown with assumptions at [Compliance → Time saved](compliance/time-saved.md).

## Where to read next

- New to regtech? [What is regtech?](concepts/regtech-intro.md)
- New to ADK? [ADK quickstart](getting-started/adk-quickstart.md)
- Want the regulation map? [EU vs UK landscape](concepts/eu-uk-landscape.md)
- Want the plugin reference? [Plugins overview](plugins/index.md)
- Want the regulation reference? [Compliance overview](compliance/index.md)
