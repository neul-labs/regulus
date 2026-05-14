# PRA SS2/21 — Outsourcing & Third-Party Risk

## In one sentence

The PRA's expectations for how UK banks and insurers govern outsourcing
and material third-party arrangements: register them, assess them, contract
properly, monitor them, and be able to exit cleanly.

## Who does it apply to?

UK banks, building societies, insurance firms, and PRA-designated
investment firms with material outsourcing or third-party arrangements.

For an AI agent: the LLM provider (and its sub-providers) are third
parties; the cloud provider is a third party; any external tool the agent
calls is potentially a third party.

## The two-minute explainer

SS2/21 came into force in 2022. It applies the PRA's view of outsourcing
risk to a world where firms increasingly depend on cloud and SaaS. Its
core demands:

1. **Register of outsourcing and material third-party arrangements.** With
   classification by criticality.
2. **Pre-contract due diligence and risk assessment.**
3. **Written contracts with required terms** (audit rights, sub-outsourcing
   disclosure, data location, exit, business continuity).
4. **Ongoing monitoring** with documented controls.
5. **Documented exit plan** — including in stressed scenarios.

The PRA explicitly addresses cloud and AI here: an LLM provider is a
critical third party in many production AI agent deployments.

## What it actually requires of an engineer

- **Register entry per material third party.** Includes the LLM provider,
  the cloud provider, and any third-party tool the agent uses.
- **Data location captured.** Region pinning is part of the register and
  contracts.
- **Exit capability proof.** Can you switch providers? In how long? At what
  cost?
- **Audit rights exercised.** Show evidence of audits — even where
  performed via SOC 2 / ISO 27001 reports rather than direct.
- **Long retention** of records (5+ years).

## What Regulus does for you

- `RegulusAuditPlugin` emits `third_party_id`, `third_party_criticality`,
  `exit_plan_ref` on every event when the `pra-ss2-21` profile is active.
- `ModelRegistry` maintains the LLM provider entries; metadata exposes sub-
  outsourcing.
- `RegulusDataResidencyPlugin` enforces the data-location commitments made
  in the contract.
- `RegulusKillSwitchPlugin` is the technical exit primitive: if a provider
  fails, the firm can cut traffic.

## Saves you ~

- Third-party register integration: ~3 engineer-weeks.
- Audit-linkage to register entries: ~1 engineer-week.
- Exit-plan substantiation evidence: ~2 engineer-weeks.

Net: ~6 engineer-weeks incremental.

## Code: minimal

```yaml
regulus:
  compliance:
    profiles: [pra-ss2-21]
```

## Code: production

Usually paired with SS1/23 + FCA SYSC:

```yaml
regulus:
  compliance:
    profiles: [pra-ss2-21, pra-ss1-23, fca-sysc, uk-gdpr]
  adk:
    audit:
      sink: kafka
      kafka-topic: audit.pra-out.v1
    residency:
      allowed-regions: [europe-west2]
      require-cmek: true
```

## How to verify

- Audit events have `third_party_id` + `third_party_criticality` populated.
- A configured exit-plan reference is present.
- Region drift is impossible because of the residency plugin's fail-closed
  startup.

## What an auditor will ask

1. **"Show me your outsourcing register entry for the LLM provider."**
   Linked from audit events.
2. **"What does your exit plan look like?"** Off-Regulus document; we
   surface its reference.
3. **"How do you exercise audit rights?"** SOC 2 / ISO 27001 evidence;
   incidents in audit log.
4. **"Demonstrate provider failover."** Operational drill.

## What this doesn't cover

- **Negotiating contracts.** Operational / legal.
- **The exit plan itself.** Document outside Regulus; we link to it.
- **Cross-border outsourcing law beyond residency** (e.g. tax, FX, employment).

## Framework mapping

- **NIST AI RMF** — SS2/21 §3 (register) ↔ `GOVERN-6.1`. §6 (residency) ↔
  `MEASURE-2.7`. §10 (exit) ↔ `MANAGE-2.2`.
- **ISO/IEC 42001** — SS2/21 §3 ↔ `A.10.3`. §6 ↔ `A.6.2.4`. §7 (audit
  rights) ↔ `A.10.2`.

## Citations

- PRA Supervisory Statement 2/21 — https://www.bankofengland.co.uk/prudential-regulation/publication/2021/march/outsourcing-and-third-party-risk-management-ss
- SS2/21 §3 — register and classification.
- SS2/21 §6 — data residency and location.
- SS2/21 §7 — audit rights.
- SS2/21 §10 — exit plans.
