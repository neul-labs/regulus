# Program operating model

A pragmatic operating model for an AI governance program running on
Regulus. Not the only way; one that works.

## RACI

R = Responsible, A = Accountable, C = Consulted, I = Informed.

| Activity | 1L Engineering | 2L Risk / compliance | 3L Internal audit | CAIO | Legal |
|---|---|---|---|---|---|
| Build agent (Regulus plugins active) | **R/A** | C | I | I | I |
| Author policy text | I | **R** | I | C | **A** |
| Activate compliance profile | **R** | C | I | C | C |
| Activate governance framework | C | **R/A** | I | C | I |
| Quarterly control-testing cycle | R | **R/A** | I | I | I |
| Annual ISO 42001 SoA refresh | I | **R/A** | C | C | C |
| Incident response (Reg-class) | **R** | C | I | I | C |
| Incident escalation (>= significant) | I | **R** | I | **A** | C |
| Kill-switch activation drill | R | **R/A** | C | I | I |
| External audit / regulator engagement | I | C | **R** | **A** | C |
| New regulation in scope | I | **R** | I | **A** | **A** |

## Cadence

| Cadence | Activity |
|---|---|
| Real-time | Audit events flow continuously to GRC tool |
| Daily | 1L watches dashboard; 2L reviews high-severity events |
| Weekly | 2L spot-checks policy-enforcement events |
| Monthly | 2L runs gap analysis; coverage-matrix drift review |
| Quarterly | Kill-switch drill (rotating operators); 1L+2L incident tabletop |
| Annually | ISO 42001 SoA refresh; framework binding review; 3L audit cycle |
| On-demand | Subject access request / erasure; regulator request |

## Tooling stack

A working stack — Regulus + a GRC tool + supporting infrastructure — looks
roughly like:

```
Identity (IDP / SSO)           — operator + SMF identities
Secrets management             — credentials for GRC adapters
Kafka / equivalent             — audit topic + retention sink
Object Lock bucket             — archival of evidence past retention horizon
Observability (Prometheus / Grafana / Datadog) — metrics from the agents
GRC tool (ServiceNow IRM /     — policy + risk + control library
  OneTrust / MetricStream)       testing workflow + evidence repository
Regulus on ADK App             — runtime enforcement + evidence emission
```

## A 90-day starting plan

If you're starting from zero:

**Days 1–30** — pick profiles + frameworks; activate Regulus in dev;
get audit events flowing; baseline gap analysis.

**Days 31–60** — pick one GRC adapter (usually ServiceNow IRM or whatever
the firm already pays for); wire it; do a single control-test cycle end
to end.

**Days 61–90** — kill-switch drill; subject access drill; document
operating model; brief 2L and 3L; first quarterly review.

After 90 days, the program is operational. ISO 42001 certification
preparation typically runs in parallel to all of the above, on a 9–18
month timeline depending on existing maturity.

## Anti-patterns

- **Skipping the framework layer.** Profiles satisfy regulators; if
  buyers / boards ask "what framework do you align to?", "we satisfy
  GDPR" isn't the answer.
- **Wiring more than one GRC adapter without justification.** Each
  vendor relationship is its own maintenance burden.
- **Letting 1L flip the kill switch without 2L oversight.** Defeats the
  dual-control purpose. Build the runbook to involve 2L on every
  activation.
- **Letting the coverage matrix drift.** Run `regulusComplianceMatrix`
  in CI and fail the build on diff.
