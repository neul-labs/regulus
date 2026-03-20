# NIS2

## In one sentence

The EU's second Network and Information Security Directive: cybersecurity
obligations for "essential" and "important" entities across critical
sectors, with mandatory incident reporting and supply-chain security
requirements.

## Who does it apply to?

Entities in sectors listed in NIS2 Annexes I (essential) and II (important):
energy, transport, banking, financial market infrastructure, health, drinking
water, waste water, digital infrastructure, ICT service management, public
administration, space, manufacturing of critical products, food, postal
services, waste management, chemicals, research, digital providers (search,
marketplaces, social networks).

Member states transpose NIS2 into national law (UK is not in scope; UK has
its own NIS Regulations 2018 + amendments).

## The two-minute explainer

NIS2 (Directive EU 2022/2555) broadened and deepened the EU's 2016 NIS
Directive. It applies from October 2024 via national transposition. It
fixes three big NIS-1 weaknesses: too few sectors covered, inconsistent
implementation between member states, and weak enforcement.

NIS2 demands a **cybersecurity risk-management framework** (Art. 21) and
**incident reporting** (Art. 23) with a 24-hour early warning + 72-hour
notification cadence. It pushes **supply-chain security** explicitly:
entities must consider the cyber posture of suppliers, especially direct
software and hardware providers.

For AI agents in NIS2 sectors: the agent's runtime is in scope; the LLM
provider is a supply-chain dependency to be assessed; incidents that affect
service availability or data integrity must be reported.

## What it actually requires of an engineer

- **Cyber risk-management programme.** Documented, with technical and
  organisational measures (Art. 21(2): risk analysis, incident handling,
  business continuity, supply-chain security, vuln management, access
  control, cryptography, MFA where appropriate, asset management, basic
  cyber hygiene training).
- **Incident reporting timeline.** Significant incident → 24h early
  warning → 72h notification → 1-month final report.
- **Supply-chain risk.** Documented assessment of direct suppliers.
- **Management body responsibility.** Board signs off; can be personally
  liable.

## What Regulus does for you

- `RegulusAuditPlugin` emits `incident_severity` and
  `essential_entity_indicator` on events. The audit pipeline can produce a
  NIS2-shaped early-warning body.
- The LLM provider lands in the model registry; supply-chain assessments
  cross-reference it.
- Residency + CMEK + signed audits are part of the cryptography + asset
  management story.

## Saves you ~

- NIS2-shaped incident pipeline: ~3 engineer-weeks (less than DORA's RTS
  because the schema is simpler).
- Supply-chain register integration: shared with DORA.
- Audit trail with attribution + immutability: shared with GDPR / DORA.

Net: ~3 engineer-weeks incremental over the GDPR + DORA baseline.

## Code: minimal

```yaml
regulus:
  compliance:
    profiles: [nis2]
```

## Code: production

```yaml
regulus:
  compliance:
    profiles: [nis2, gdpr, dora]    # most NIS2 entities are also DORA in fin-services
  adk:
    audit:
      sink: kafka
      kafka-topic: audit.nis2.v1
    residency:
      allowed-regions: [europe-west1, europe-west2, europe-west3, europe-west4]
    kill-switch:
      enabled: true
      dual-control: true
```

## How to verify

- Synthetic significant-incident drill within 24h notification window.
- Audit events show `incident_severity` and (where applicable)
  `essential_entity_indicator`.
- Supply-chain register entry for the LLM provider.

## What an auditor will ask

1. **"What's your cyber risk-management framework?"** Off-Regulus document.
2. **"Walk me through a recent significant-incident notification."** Drill.
3. **"How do you assess your supply chain?"** Model registry + linked
   third-party risk programme.

## What this doesn't cover

- **The board-level governance bits.** Operational and HR.
- **National-law variations.** NIS2 is transposed differently in each member
  state.
- **Sector-specific overlays** (e.g. EBA Guidelines for banking).

## Citations

- Directive (EU) 2022/2555 — https://eur-lex.europa.eu/eli/dir/2022/2555/oj
- Art. 21 — risk-management measures.
- Art. 23 — incident reporting.
- ENISA guidance — https://www.enisa.europa.eu
