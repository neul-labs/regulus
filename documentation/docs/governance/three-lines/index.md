# Three Lines of Defence

The dominant operating model for risk management in regulated firms.
Adopted across financial services, public sector, and increasingly
healthcare; reported at 88% adoption in EU financial services with a
measurable benefit on incident rates.

## The three lines

| Line | Who | Role |
|---|---|---|
| **1L** | Engineering, data scientists, business unit owners | Owns the risk at source. Builds and runs the AI system. |
| **2L** | Risk, compliance, model risk, privacy, security functions | Independent oversight. Sets policy, monitors implementation, escalates. |
| **3L** | Internal audit | Independent assurance. Tests the design and operation of controls. |

Some firms add a "**0L**" — board / executive committee oversight — and a
"**4L**" — external regulator or auditor — but these are layers around
the model rather than inside it.

## How Regulus serves each

| Line | What they need | What Regulus provides |
|---|---|---|
| 1L | Runtime guardrails | Plugins on the ADK `App` — `RegulusPolicyPlugin`, `RegulusPrivacyPlugin`, etc. Inline enforcement. |
| 2L | Independent evidence, gap analysis, policy enforcement records | Audit event stream + governance evidence plugin + GRC adapters; coverage matrix; ISO 42001 SoA generator; gap analysis from `GovernanceProgramState` |
| 3L | Immutable assurance trail; control walkthroughs | Signed audit events (under profiles with `Immutability.SIGNED`); reproducible coverage matrix; tamper-evident retention windows |

Same substrate, three views. That's the engineering value.

## Pages in this section

- [1L — Engineering](first-line.md)
- [2L — Risk & compliance](second-line.md)
- [3L — Internal audit](third-line.md)

## Why the asymmetry matters

The lines aren't interchangeable, and Regulus respects that:

- **1L** has *power to act*. They write code, deploy agents, flip kill
  switches. Regulus enforces controls *for* them but doesn't constrain
  their authority.
- **2L** has *power to escalate*. They can't deploy; they can stop, audit,
  and report. Regulus' evidence is what they escalate *on*.
- **3L** has *power to find*. They can't deploy or stop; they audit the
  design. Regulus' immutable trail is what they audit.

Conflating the lines is what regulators call out as a governance
weakness. Regulus' design intentionally distinguishes runtime mechanisms
(1L surface) from evidence emission (2L surface) from immutability
guarantees (3L surface).

## Citations

- The Institute of Internal Auditors — Three Lines Model (2020 update).
- ICAEW: "Prepare for 2026 — how to manage cyber and AI risk."
- Yields.io / various: Three Lines of Defence in Model Risk Management.
