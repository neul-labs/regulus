# Dual control / 4-eyes

A primitive borrowed from banking. Some actions are too consequential for one
person to take alone — so the system requires **two distinct authorised
people** to agree. In banking it's how you authorise a large wire transfer.
In AI it's how you turn the kill switch back off.

## Where it comes from in AI regulation

- **EU AI Act Art. 14** — "human oversight" must include "the natural persons
  to whom human oversight is assigned" being able to "intervene on the
  operation of the high-risk AI system or interrupt the system through a 'stop'
  button or a similar procedure." For high-stakes deactivations, single-person
  control is widely read as inadequate.
- **PRA PS21/3** — algorithmic-trading systems require kill switches with
  documented authorisation. After the 2010 Flash Crash and similar incidents,
  "one person can both flip and unflip" became an explicit anti-pattern.
- **PRA SS1/23 §6** — "the firm should be able to switch off a model
  rapidly," with governance around when this is exercised.
- **FCA Consumer Duty** — when an agent could cause customer harm at scale,
  the firm needs to be able to halt it without exposing the halt mechanism
  itself to abuse.

## How it maps to ADK

ADK ships an official Human-in-the-Loop primitive: **`ToolConfirmation`**. A
tool inside an agent can call `toolContext.requestConfirmation()` to pause
execution until a human approves. This is the exact shape Regulus' dual
control needs.

So `RegulusKillSwitchPlugin` uses ADK's `ToolConfirmation` for the second
operator's confirmation. No special-case API: the same primitive ADK users
already know.

## The Regulus workflow

```
[Operator A]  ->  KILL  ->  store.activate(scope, "operator_A", reason)
                            -> all subsequent BeforeAgentCallback short-circuit
                            -> audit event { action: kill-switch-activate, actor: operator_A }

[Operator B]  ->  PROPOSE-DEACTIVATE
                            -> store.requestDeactivate(scope, "operator_B", reason)
                            -> ADK ToolConfirmation requested
                            -> audit event { action: kill-switch-deactivate-requested, actor: operator_B }

[Operator C]  ->  CONFIRM
                            -> store.confirmDeactivate(scope, "operator_C")
                            -> store enforces operator_B != operator_C
                            -> kill switch lifted
                            -> audit event { action: kill-switch-deactivated, requester: operator_B, confirmer: operator_C }
```

Important: **activation is single-control** (one operator can stop everything
fast); **deactivation is dual-control** (turning it back on requires two).
This asymmetry is deliberate and matches the regulator's expectation.

## Other places dual control shows up

- **Vulnerable-customer interactions** (FCA Consumer Duty FG22/5 §4):
  `RegulusPolicyPlugin` returns `PolicyDecision.RequireConfirmation` when the
  invocation context flags a vulnerable customer.
- **Automated decisions with legal effect** (GDPR / UK GDPR Art. 22): same —
  a confirmation is requested before the model is consulted.
- **High-risk computer actions** (form submit, payment confirm, file upload):
  `RegulusComplianceBaseComputer` requests confirmation before executing.

In each case the implementation is `toolContext.requestConfirmation()` —
Google's primitive, our policy.

## What you have to operate

Regulus enforces the technical primitive. You operate:

- Two distinct, identified, authorised humans for any deactivation.
- A runbook documenting when activation is acceptable, who can do it, and
  what happens next.
- A quarterly rehearsal so the runbook is real, not aspirational.

[Operations → Kill-switch playbook](../operations/kill-switch-playbook.md) is
a starter template.

## Next

- [Security architecture → Kill-switch authorization](../advanced/security-architecture.md#kill-switch-authorization) — the `KillSwitchAuthorizer` SPI and why approver-distinctness is on `Principal.id`, not strings.
- [Glossary](glossary.md)
- [Plugin reference → RegulusKillSwitchPlugin](../plugins/kill-switch.md)
