# PRA SS1/23 — Model Risk Management

## In one sentence

The PRA's Supervisory Statement 1/23 sets out model risk management
principles for UK banks: maintain a model inventory, tier models by
materiality, validate them independently, monitor them in production, and
be able to switch them off rapidly.

## Who does it apply to?

UK banks, building societies, and PRA-designated investment firms. Other
financial firms are encouraged to adopt SS1/23 as best practice even where
not directly bound.

If your firm is PRA-authorised and the AI agent influences a model-driven
decision (credit, capital, risk, pricing), you are in scope.

## The two-minute explainer

SS1/23 came into force in May 2024. It's not AI-specific — it covers any
quantitative model the bank uses, from regulatory capital to retail credit
scoring to AI agents. But because LLMs and agents are now embedded in
production model risk, SS1/23 applies to them directly.

The PRA's five principles:

1. **Model identification and inventory.** Comprehensive, current.
2. **Risk-tiering by materiality.** Higher tier → stricter controls.
3. **Independent validation.** Pre-deployment + periodic.
4. **Ongoing monitoring.** Drift, performance, fitness for purpose.
5. **Capability to switch off.** Rapid model deactivation when needed.

For an AI agent built on ADK: the agent is a model. The LLM under it is a
model. The chained tools and prompts are part of the model boundary. All of
this needs to be in the inventory, tiered, validated, monitored, and
deactivable.

## What it actually requires of an engineer

- **Model inventory entry** for the agent and each LLM model used. Stable
  identifier, version, owner, tier.
- **Tier-aware policy.** Tier 1 models get the strictest control envelope.
- **Validation evidence** persisted alongside the inventory entry. Pre-go-
  live + periodic. Auditable.
- **Monitoring metrics** emitted per model invocation (latency, error rate,
  output-quality proxies, drift signals).
- **Deactivation capability.** Kill switch wired to the model, with
  documented procedure.

## What Regulus does for you

- `RegulusModelRiskPlugin` tiers models per tenant and per model; rejects
  tier-exceeding invocations at `BeforeModelCallback`.
- `ModelRegistry` holds the per-model classification; persistent
  implementations (Postgres / Firestore) carry the validation evidence
  reference.
- `RegulusAuditPlugin` emits `model_id`, `model_version`, `model_risk_tier`,
  `validation_status` on every event.
- `RegulusKillSwitchPlugin` provides per-model + per-tenant deactivation;
  dual-control on reactivation.
- `RegulusRetentionEventCompactor` for the `pra-ss1-23` profile is set to 5
  years (raw) + 7 years (summary) with `SIGNED` immutability.

## Saves you ~

- Model inventory + tier classification: ~2 engineer-weeks.
- Validation evidence persistence + audit linkage: ~3 engineer-weeks.
- Tier-aware policy engine: ~2 engineer-weeks (shared with `RegulusModelRiskPlugin`).
- Rapid deactivation primitive: ~4 engineer-weeks (shared with kill switch).

Total incremental: ~11 engineer-weeks above the GDPR baseline.

## Code: minimal

```yaml
regulus:
  compliance:
    profiles: [pra-ss1-23]
  adk:
    model-risk:
      tenant-tier: REGULATED
```

## Code: production

```yaml
regulus:
  compliance:
    profiles: [pra-ss1-23, fca-sysc, uk-gdpr]
  adk:
    audit:
      sink: kafka
      kafka-topic: audit.pra.v1
    kill-switch:
      enabled: true
      dual-control: true
    model-risk:
      tenant-tier: REGULATED
    residency:
      allowed-regions: [europe-west2]
      require-cmek: true
```

## How to verify

- Try a `gemini-2.5-pro` call (REGULATED tier) from a STANDARD-tier tenant:
  `BeforeModelCallback` blocks with citation `SS1/23 §3`.
- Pull a sample event: `model_id`, `model_version`, `model_risk_tier`,
  `validation_status` present.
- Trip the kill switch on a specific model id: subsequent calls to that
  model rejected; other models still serve.

## What an auditor will ask

1. **"Show your model inventory."** Registry + linked validation evidence.
2. **"What tier is this model and why?"** Tier classification rationale per
   model; audit linkage.
3. **"Demonstrate validation."** Off-Regulus; we audit-link.
4. **"What monitoring is in place?"** Observability module integration.
5. **"Demonstrate rapid model deactivation."** Kill-switch drill scoped to
   one model id.

## What this doesn't cover

- **The validation activity itself.** That's your model risk function.
- **Regulatory capital model approval.** PRA waterfall + Pillar 2; separate
  workstream.
- **Internal model committee paperwork.** Operational.

## Framework mapping

PRA SS1/23 is the UK fin-services analogue of NIST AI RMF for model
risk; the mapping is therefore very close:

- **NIST AI RMF** — SS1/23 §2 (inventory) ↔ `MAP-4.1`. §3 (tiering) ↔
  `MAP-4.1`. §4 (validation) ↔ `MEASURE-1.1`. §5 (monitoring) ↔
  `MANAGE-4.1`. §6 (kill switch) ↔ `GOVERN-4.1`.
- **ISO/IEC 42001** — SS1/23 §2-4 ↔ `A.5.2` + `A.6.2.3`. §5 ↔
  `A.6.2.5` (operation and monitoring). §6 ↔ `A.6.2.8` (change
  management).

## Citations

- PRA Supervisory Statement 1/23 — https://www.bankofengland.co.uk/prudential-regulation/publication/2023/may/model-risk-management-principles-for-banks-ss
- SS1/23 §2 — model identification.
- SS1/23 §3 — model risk tiering.
- SS1/23 §4 — model validation.
- SS1/23 §5 — model monitoring.
- SS1/23 §6 — capability to deactivate.
