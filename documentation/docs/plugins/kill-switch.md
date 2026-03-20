# RegulusKillSwitchPlugin

## In one sentence

Per-tenant / per-agent kill switch with dual-control deactivation, layered
on ADK's `ToolConfirmation` primitive.

## Who does it apply to?

Mandatory in PRA PS21/3 (algorithmic trading), strongly recommended in PRA
SS1/23 §6 (model risk management), required in EU AI Act Art. 14 (human
oversight). Many other profiles cite it as part of incident-response
readiness.

## The two-minute explainer

The kill switch has three operations:

- **Activate.** Any authorised operator can flip the switch unilaterally.
  Once flipped, all subsequent agent invocations short-circuit with a
  `KillSwitchActive` event at `BeforeAgentCallback`.
- **Request deactivate.** A different operator proposes turning the switch
  back on. The proposal flows through ADK's `ToolConfirmation` and is
  audited but does not take effect on its own.
- **Confirm deactivate.** A *third* operator (or at minimum, someone other
  than the requester) confirms. The store enforces requester ≠ confirmer.
  The switch lifts; audit records all three operators and timestamps.

This asymmetry — single-control to activate, dual-control to deactivate —
matches what regulators expect. The fast-stop is more important than the
fast-restart.

## What it actually requires of an engineer

- Persist kill state somewhere durable. The built-in
  `InMemoryKillSwitchStore` is for dev / tests; production needs Postgres,
  Firestore, or similar.
- Wire your operator identity into the calls — `operator` strings should
  resolve to authenticated humans, not service accounts.
- Operate the runbook (rotation, drill cadence, post-incident review).

## What Regulus does for you

- The plugin (`BeforeAgentCallback`) does the check.
- The store contract (`KillSwitchStore`) defines what persistence has to
  guarantee.
- The dual-control workflow uses ADK's `ToolConfirmation` so it's the same
  shape as any other HITL in your stack.

## Saves you ~

- ~4 engineer-weeks for a working kill switch with state, 4-eyes,
  monotonic audit, and operator binding.

## Code: minimal

```java
RegulusKillSwitchPlugin killSwitch = RegulusKillSwitchPlugin.dualControl();
```

(Uses `InMemoryKillSwitchStore` — fine for tests, not for prod.)

## Code: production

Provide a persistent store:

```java
@Bean
KillSwitchStore killSwitchStore(JdbcTemplate jdbc) {
    return new PostgresKillSwitchStore(jdbc);
}

@Bean
RegulusKillSwitchPlugin killSwitch(KillSwitchStore store) {
    return RegulusKillSwitchPlugin.withStore(store);
}
```

Operate via your admin API:

```http
POST /admin/kill-switch
{
  "scope": "agents/mortgage-advice",
  "operator": "ops-A",
  "reason": "regression detected in eligibility-check tool"
}
```

## How to verify

- Activate → next request fails with `KillSwitchActive`.
- Same operator tries to confirm own deactivation → refused.
- Different operator confirms → switch lifts, audit trail records all
  parties.
- Drill quarterly with rotating operators (audit walkthrough recommends
  this — auditors look for it).

## What an auditor will ask

1. **"Demonstrate the kill switch."** Drill.
2. **"Show the audit trail of the most recent activation."** Audit events
   for activate / request-deactivate / refused-self-confirm / confirm.
3. **"Who can flip it?"** Operator IDP integration.
4. **"How often do you rehearse?"** Calendared task in the playbook.

## What this doesn't cover

- **Operator authentication.** Use your IDP / SSO.
- **Notification to stakeholders on activation.** Hook on the audit sink.
- **Recovery of in-flight requests.** They fail-fast; client retries.

## Citations

- See [Concepts → Dual control / 4-eyes](../concepts/dual-control.md).
- PRA PS21/3, PRA SS1/23 §6, EU AI Act Art. 14.
