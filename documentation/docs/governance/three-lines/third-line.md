# 3L — Internal audit line

The independent assurance function. Reports to the audit committee /
board. In a Regulus deployment, 3L tests the design and operation of
the controls 1L and 2L run.

## What this line owns

- Independent assurance to the board that risk is being managed
  effectively.
- Audits of 1L and 2L's work — including the AI governance program.
- Reporting findings, tracking remediation.

## What Regulus gives them

**An immutable, reproducible evidence trail that survives the audit
window.**

| Need | How Regulus delivers |
|---|---|
| Tamper-evident audit | `Immutability.SIGNED` profile setting + retention windows enforced by the compactor |
| Reproducible coverage matrix | `regulusComplianceMatrix` deterministic from active profiles + frameworks |
| Walk-throughs | Audit walkthrough doc + replayable event sequences |
| Sample-based control testing | Subject-linked queries on the audit topic |
| Evidence preservation past storage limitation | Summary tier of the compactor + optional Object Lock archive |

## Daily life on this line

- **Plan audits** based on the active framework profile (typically ISO
  42001 + applicable regulations).
- **Sample audit events** for a given control id, verify the control
  fired as expected, verify the `framework_control_id` aligns to the
  control inventory.
- **Test the kill switch.** Drill rotates operators so the dual-control
  mechanism is actually independent.
- **Review SoA refresh cycles** for drift between declared and actual
  implementation.
- **Report findings** to the audit committee with audit-event-backed
  evidence.

## What 3L does *not* own

- Designing the controls. 2L's job.
- Implementing the controls. 1L's job.
- Setting policy. 2L's job (with legal).

## Common audit findings Regulus pre-empts

- **"We can't find the evidence."** Regulus' audit topic + GRC adapters
  put evidence in the canonical location.
- **"The control existed but didn't fire."** Regulus' plugins are
  inline, not advisory — a missing audit event is itself a finding.
- **"Two people both signed as the second eye."** Regulus refuses
  same-operator dual control at the store level.
- **"Residency drifted in production."** Regulus' fail-closed startup
  makes this impossible.

## What 3L tests that Regulus *can't* prove

- Whether 1L's interpretation of policy is correct.
- Whether 2L's controls are appropriate for the risk.
- Whether the right people are notified at the right severities.

Those are organisational questions that need human judgment.
