# Audit trails

An audit trail is the thing an inspector looks at when they show up. If you
can't produce it, you have no story. If it's incomplete, you have a worse
story. Regulus' job is to make sure it's complete and that you didn't have
to think about it.

## What an auditor actually looks at

In rough order of "first question":

1. **Coverage.** Is there a record for every consequential action? Not just
   successes, not just failures, not just human actions — *every action*.
2. **Attribution.** Can you tell who did what? "The system did it" is rarely
   acceptable; auditors want a chain from the agent action back to a human
   (SMF holder, clinician's smartcard ID, end-user-with-purpose).
3. **Time.** Monotonic timestamps. UTC. Skew-resistant. Order matters when
   reconstructing an incident.
4. **Justification.** Why did the action happen? Purpose codes, lawful basis,
   consent reference, model decision rationale where available.
5. **Result.** What actually happened? Allow / block / require-confirmation /
   error.
6. **Immutability.** Can a malicious operator (or a panicked one) edit the
   trail after the fact? Append-only sinks (Kafka, immutable buckets) and /
   or signatures are how you answer "no."
7. **Retention.** Is the record still there when the auditor asks for it,
   N years after the fact? GDPR pushes for storage limitation; FCA / DORA /
   PRA push for long retention. Both can be true if you summarise the body
   after the privacy clock runs out.
8. **Linkability.** Can you reconstruct a customer's interaction history end
   to end? GDPR Art. 15 (subject access) and Art. 17 (erasure) both demand
   this.

## How Regulus structures events

A Regulus audit event is a JSON object. The required field set is the
**union** across active profiles — over-collect, not under-collect.

```json
{
  "event_id": "01J6X4...",
  "occurred_at": "2026-05-14T11:23:09.123Z",
  "actor": "user:12345",
  "smf_holder": "SMF24:Jane Smith",
  "action": "model-call",
  "result": "allow",
  "model_id": "gemini-2.5-pro",
  "model_version": "2026-05-01",
  "ai_act_risk_tier": "limited",
  "consumer_duty_outcome": "support",
  "fca_lei": "213800ABC123",
  "purpose_code": "claims-triage",
  "lawful_basis": "Art. 6(1)(b)",
  "data_categories": ["personal_data"],
  "subject_id": "01J6X4SUB...",
  "redactions": ["NINO_1"]
}
```

Field meanings:

- `event_id` — ULID; unique forever.
- `actor` — the principal that triggered the action. If a human did, their
  identity; if an agent acting on behalf of one, the human's identity with an
  `actor_kind: agent` flag.
- `smf_holder` — UK FCA: the Senior Management Function whose responsibility
  this falls under.
- `purpose_code` — required when the GDPR / UK GDPR profile is active. The
  agent cannot run without one.
- `lawful_basis` — GDPR Art. 6 or Art. 9 reference.
- `subject_id` — pseudonymous customer identifier. Lets you erase or surface
  one person's records without scanning the whole log.
- `redactions` — tokens that were redacted from the prompt or response.
  Lets a future researcher know "something was here," without revealing it.

The full schema per active profile is at
[Compliance → Coverage matrix](../compliance/coverage-matrix.md).

## Mapped to ADK hooks

| Event source | ADK hook | Regulus emitter |
|---|---|---|
| Agent invocation completed | `AfterAgentCallback` | `RegulusAuditPlugin` |
| Model call completed | `AfterModelCallback` | `RegulusAuditPlugin` |
| Tool call completed | `AfterToolCallback` | `RegulusAuditPlugin` |
| Policy violation | `BeforeModelCallback` / `BeforeToolCallback` short-circuit | `RegulusPolicyPlugin` (audit emitted by `RegulusAuditPlugin` listener) |
| Kill switch tripped | `BeforeAgentCallback` short-circuit | `RegulusKillSwitchPlugin` |
| A2A inbound / outbound | `RegulusAgentExecutor` / `RegulusRemoteA2AAgent` | `regulus-ai-adk-a2a` |

## Retention

Regulation-aware retention is implemented by `RegulusRetentionEventCompactor`
on top of ADK's `EventCompactor` extension. The composite of your active
profiles picks the **longest** required retention; older events are
summarised via ADK's `BaseEventSummarizer` rather than dropped, so a forensic
reconstruction is still possible after the storage-limitation clock has run.

| Profile | Full retention | Summary retention | Erasure honoured |
|---|---|---|---|
| `gdpr` / `uk-gdpr` | 1 year | 2 years | Yes |
| `eu-ai-act` | 180 days | 5 years | Yes |
| `fca-sysc` | 5 years | 7 years | No (regulator-side override) |
| `pra-ss1-23` | 5 years | 7 years | No |
| `dora` | 5 years | 7 years | Yes |
| `nhs-dspt` | 8 years | 25 years | No |
| `ehds` | 10 years | 30 years | Yes |

GDPR Art. 17 erasure runs **within** the retention window when the active
profile permits it; financial-services profiles often don't, because the
regulator's record-keeping mandate overrides the subject's erasure right.

## Next

- [Security model](security-model.md) — every audit event carries the canonical `Principal` that the call ran under.
- [Security architecture → Audit integrity](../advanced/security-architecture.md#audit-integrity) — the opt-in hash chain that makes the trail tamper-evident.
- [Data residency](data-residency.md)
- [Plugin reference → RegulusAuditPlugin](../plugins/audit.md)
- [Operations → Audit retention runbook](../operations/audit-retention-runbook.md)
