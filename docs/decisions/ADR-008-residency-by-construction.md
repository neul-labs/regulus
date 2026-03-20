# ADR-008: Residency by construction

- Status: Accepted
- Date: 2026-04-09

## Context

Data residency is a legal requirement under GDPR / UK GDPR / FCA SYSC 13
/ PRA SS2/21 / NHS DSPT / EHDS. Three implementation shapes are common:

1. **Runtime warning.** Detect drift, log a warning, keep going. Easy;
   useless when the auditor turns up.
2. **Runtime block.** Detect drift, throw at the misrouted call.
   Probabilistic — depends on every code path going through the check.
3. **Fail-closed at startup.** Validate the wired services' configured
   regions *before* the `App` activates. Refuse to start if anything is
   wrong.

## Decision

Implement residency **by construction**: every Regulus service variant
(`RegulusVertexAiSessionService`, `RegulusFirestoreSessionService`,
`RegulusFirestoreMemoryService`, `RegulusGcsArtifactService`) validates
its configured region in its constructor. `RegulusDataResidencyPlugin`
re-checks at startup and per call as defence in depth.

If validation fails, the constructor throws — the ADK `App` never
activates.

## Why fail-closed at startup

- Probabilistic checks fail when the auditor cares most.
- Operators don't read warnings.
- Configuration drift is the most common source of residency
  violations; catching it at startup means production never has a
  misconfigured running instance.
- `gcloud config set project us-central1-project` then re-deploy is the
  exact path that produces incidents. Fail-closed kills the path.

## Consequences

Positive: residency violations cannot reach production. Auditor
demonstration is a single command (run the app with a forced misconfig;
show the refusal).

Negative: startup is brittle in the sense that the cluster has to have
the right config to start at all. Mitigated by the build-time
`regulusAdkDoctor` task surfacing the same checks in CI.

## See also

- [Concepts → Data residency](../../documentation/docs/concepts/data-residency.md)
- [`RegulusDataResidencyPlugin`](../../documentation/docs/plugins/data-residency.md)
