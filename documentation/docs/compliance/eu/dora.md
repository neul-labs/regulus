# DORA

## In one sentence

The EU's Digital Operational Resilience Act: a prescriptive ICT-resilience
rulebook for EU financial entities, with explicit demands on ICT risk
management, incident reporting, third-party risk, and digital operational
resilience testing.

## Who does it apply to?

EU financial entities: banks, insurers, investment firms, payment
institutions, e-money institutions, trading venues, central counterparties,
central securities depositories, fund managers, crypto-asset service
providers, and others listed in Art. 2. Their critical ICT third-party
providers are also in scope.

If you're a UK firm with no EU establishment and no EU customers, you are
*not* in scope — but you may be a third party to an EU entity that is.

## The two-minute explainer

DORA (Regulation EU 2022/2554) was the EU's response to fragmented national
ICT rules in financial services. It applies from January 2025. Unlike the
older operational-resilience guidance, DORA is **direct, prescriptive, and
unified**: same rules for a German bank and a French insurer; specific
deliverables; specific timelines.

Five pillars:

1. **ICT risk management** (Arts. 5–10). Board-level governance of ICT
   risk, with a documented framework, controls, BCM/DR programme.
2. **ICT incident management** (Arts. 17–23). Detect, classify, report.
   Significant incidents trigger a regulator notification clock.
3. **Operational resilience testing** (Arts. 24–27). Scenario-based and
   threat-led penetration testing for significant entities.
4. **ICT third-party risk** (Arts. 28–30). Risk-assess third parties; pre-
   contractual due diligence; written contracts; sub-outsourcing
   disclosure; an EU register of contracts.
5. **Information sharing** (Art. 45). Voluntary sharing of cyber threat
   intelligence between in-scope entities.

For AI agents: the LLM provider is a critical ICT third party. The agent's
runtime is in-scope ICT. Incidents in the agent are reportable under Art. 17
once they meet the significance thresholds the Commission's Regulatory
Technical Standards define.

## What it actually requires of an engineer

- **ICT inventory and dependencies.** Maintain a current list of all ICT
  systems and their dependencies. The AI agent + LLM provider + cloud
  region is one entry.
- **Incident classification and reporting.** Detect, classify severity (RTS
  taxonomy), and report within tight timelines (initial: 4 hours from
  classification; intermediate: 72 hours; final: 1 month).
- **Resilience metrics.** RTO, RPO, recovery testing evidence per system.
- **Third-party contracts.** Documented; with audit and exit rights;
  sub-outsourcing surfaced.
- **Long retention.** ICT-related incident records retained ≥5 years for
  significant incidents.

## What Regulus does for you

- `RegulusAuditPlugin` emits `incident_severity`, `ict_third_party`,
  `rto_seconds`, `rpo_seconds` on every event when the `dora` profile is
  active. The audit pipeline can produce the DORA-RTS-shaped incident
  notification body from existing events.
- `RegulusRetentionEventCompactor` for the `dora` profile is set to 5
  years (raw) + 7 years (summary) with `SIGNED` immutability.
- `RegulusModelRiskPlugin` model registry doubles as the ICT third-party
  register entry for each LLM provider; sub-outsourcing of the underlying
  cloud is surfaced in metadata.
- `RegulusDataResidencyPlugin` enforces EU residency with CMEK by default.
- `RegulusKillSwitchPlugin` gives you the rapid-stop primitive incident
  response will lean on.

## Saves you ~

- Incident classification + DORA-RTS-shaped reporting: ~5 engineer-weeks.
- Third-party register integration + sub-outsourcing surfacing: ~3
  engineer-weeks.
- Signed-immutability audit pipeline: ~2 engineer-weeks.
- 5-year retention with summarisation: ~1.5 engineer-weeks.

Total: ~11.5 engineer-weeks, plus tracking the RTS as they update.

## Code: minimal

```yaml
regulus:
  compliance:
    profiles: [dora]
```

## Code: production

```yaml
regulus:
  compliance:
    profiles: [dora, eu-ai-act, gdpr]
  adk:
    audit:
      sink: kafka
      kafka-topic: audit.dora.v1
    residency:
      allowed-regions: [europe-west1, europe-west3, europe-west4]
      require-cmek: true
    model-risk:
      tenant-tier: REGULATED
    kill-switch:
      enabled: true
      dual-control: true
```

## How to verify

- Audit event sample: `incident_severity`, `ict_third_party`, `rto_seconds`,
  `rpo_seconds` populated; `immutability_hint=SIGNED`.
- Compaction retention check: confirm Kafka topic config matches the
  profile's 5y+7y.
- Synthetic incident drill: trigger a severity-tagged event; verify the
  pipeline produces an RTS-shaped notification within the 4-hour clock.

## What an auditor will ask

1. **"Show your ICT third-party register."** The model registry plus the
   audit linkage; sub-outsourcing fields.
2. **"Demonstrate incident classification and notification."** Drill.
3. **"What's your RTO/RPO for this agent?"** Stored per-agent; surfaced on
   audit events.
4. **"Show resilience testing evidence."** External programme; Regulus
   provides the audit substrate, not the test plan.

## What this doesn't cover

- **Threat-led penetration testing (Arts. 26–27).** External programme.
- **Contract negotiation with the LLM provider.** Operational.
- **The DORA Oversight Framework** for critical ICT third parties. Applies
  to your providers, not to you.

## Citations

- Regulation (EU) 2022/2554 — https://eur-lex.europa.eu/eli/reg/2022/2554/oj
- Arts. 5–10 — ICT risk management.
- Arts. 17–23 — ICT incident management and reporting.
- Arts. 28–30 — ICT third-party risk.
- Commission RTS — published periodically by ESAs.
