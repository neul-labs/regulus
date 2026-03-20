# Audit retention runbook

Operating the audit pipeline so the trail is intact when an auditor asks.

## Components

- **Audit topic** in Kafka. Append-only, compacted off, retention set per
  active profile.
- **Compactor task** running `RegulusRetentionEventCompactor`. Sweeps on
  a cron; drops events past the summary window, summarises events past
  the raw window.
- **Object Lock bucket** (optional) for ultra-long-retention archive.

## Daily

- Watch the compactor task's success metric. Failures are rare but
  expensive — events stop summarising, storage grows.
- Check the audit topic lag. Lag means events aren't being consumed for
  summarisation.

## Weekly

- Spot-read a sample event. Required fields populated? `redactions[]`
  present where expected?

## Monthly

- Run `regulusComplianceMatrix` against production config and compare to
  the checked-in matrix. Drift means someone added a profile or a control
  without doc updates.

## Quarterly

- Subject access request drill: pick a synthetic `subject_id`, run the
  audit query, verify output is complete and redaction-aware.
- Erasure drill: pick a synthetic subject under a profile that permits
  erasure (e.g. `gdpr`-only tenant), execute the path, verify tombstone.

## Annually

- Reconfirm retention windows against current regulation text.
- Reconfirm storage cost forecast vs. actual.

## On incident

- Audit topic frozen at incident window: tag the events, snapshot to
  Object Lock for any regulator response.
- Post-incident: run a reconstruction from the audit log; verify it
  produces a complete story.
