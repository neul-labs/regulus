# ADR-012: Three Lines of Defence as the operating model

- Status: Accepted
- Date: 2026-05-13

## Context

Regulus serves at least three distinct audiences from the same evidence
substrate: engineers building agents, risk/compliance functions
overseeing them, and internal auditors providing independent assurance.
Without naming the operating model explicitly, the same artefacts get
described differently in each context — which is exactly the
misalignment the "Three Lines of Defence" model exists to fix.

The model is widely adopted: 88% of EU financial-services firms run
some variant of it, with measurable benefit on incident rates. ICAEW's
2026 guidance reinforces it for AI risk specifically.

## Decision

Name Three Lines of Defence (3LoD) **explicitly** in the Regulus
documentation as the operating model the framework is designed to
serve. Document concretely how Regulus' surfaces map to each line:

| Line | Audience | Regulus surface |
|---|---|---|
| 1L | Engineering, business unit owners | ADK plugins (inline enforcement) |
| 2L | Risk, compliance, model risk, privacy, security | Audit event stream → `RegulusGovernanceEvidencePlugin` → GRC adapters → GRC tool |
| 3L | Internal audit | Immutable (signed) audit topic + retention + reproducible coverage matrix + SoA snapshots |

A dedicated "Three Lines of Defence" subsection of the Governance
section in the docs covers each line with its own page, including
boundary signals (what to escalate when 1L bleeds into 2L territory,
etc.).

## Why name it explicitly

- **Vocabulary alignment.** The buyer's CISO, CRO, and Head of Internal
  Audit already think in 1L/2L/3L. Speaking their language reduces
  friction.
- **Asymmetric guarantees.** 1L surfaces (plugin enforcement) and 3L
  surfaces (immutability) make different promises. Conflating them
  weakens both stories.
- **Audit-defensible separation.** Regulators and external auditors
  expect to see the model evidenced; saying "Regulus serves 3L's
  independent-assurance needs by..." is far stronger than "the audit
  log is signed."

## Out of scope

- **Implementing the operating model for customers.** That's their
  governance team's responsibility; we provide the substrate.
- **Tooling for 3L's audit planning / workflow.** Off-Regulus; lives in
  the GRC tool or a dedicated internal-audit platform (e.g. Workiva,
  AuditBoard).
- **Defining "4L" (regulators / external auditors).** Existing audit
  walkthrough doc covers this; not a new line in the framework.

## Consequences

Positive: documentation that speaks to all three lines without needing
to be re-translated; clearer customer conversations.

Negative: more documentation surface. Mitigated by the regtech-
explainer template (ADR-009) which keeps page structure predictable.
