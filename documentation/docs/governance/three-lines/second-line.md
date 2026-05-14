# 2L — Risk & compliance line

The independent oversight function. In a Regulus deployment that's the
risk, compliance, model risk, privacy, and security teams who read what
1L built, decide whether it satisfies policy, and escalate when it
doesn't.

## What this line owns

- The policy (in coordination with legal).
- The risk register.
- Independent review of 1L's implementation.
- Control testing (often via the GRC tool).
- Escalation to the executive committee on material breaches.

## What Regulus gives them

**A continuous evidence stream they don't have to chase 1L for.** Plus
two artefacts on demand:

| Need | How Regulus delivers |
|---|---|
| Continuous evidence per control | `RegulusGovernanceEvidencePlugin` fanning audit events to the GRC tool |
| Coverage matrix (regulation × framework × control × evidence) | `./gradlew regulusComplianceMatrix` |
| ISO 42001 Statement of Applicability | `StatementOfApplicability` generator from `GovernanceProgramState` |
| Gap analysis | `GovernanceProgramState.gaps()` lists controls with `GAP` status |
| Policy enforcement records | Audit events with `result=block` or `result=require-confirmation` |
| Incident records | Audit events with `incident_severity` populated |
| Subject access / erasure response | Subject-linked audit queries + tombstones |

## Daily life on this line

- **Read the GRC tool's control-evidence view.** ServiceNow IRM /
  OneTrust / MetricStream show Regulus evidence per control id, with
  back-links to raw audit events.
- **Spot-check policy enforcement.** Sample `result=block` events,
  confirm citations are correct, confirm the policy decisioning matches
  intent.
- **Run quarterly coverage matrix drift checks.** Did 1L change a
  binding without 2L sign-off?
- **Drive the ISO 42001 SoA cycle.** Refresh implementation status,
  collect justifications, sign off.
- **Investigate adapter failures.** `grc-adapter-failure` events
  surface when a vendor adapter can't reach its target — a fixable
  pipeline issue, not an agent issue.

## What 2L does *not* own

- Building the agent. 1L's job.
- Independent assurance. 3L's job — they audit 2L's work.
- Authoring the regulation. Regulators do that.

## Boundary signals

- 2L writing code in the agent repo → that's 1L's job; document the
  policy upstream and let 1L implement it.
- 2L unable to produce evidence on demand → indicates GRC adapter
  configuration drift; the runbook is in
  [GRC integration](../grc/index.md).
- 2L unable to attest controls without contacting 1L → the evidence
  stream isn't delivering enough context; raise the relevant
  `framework_control_id` mapping.
