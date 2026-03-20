# RegulusRetentionEventCompactor

## In one sentence

Implements ADK's `EventCompactor` contract with a retention window picked
from the active compliance profile, summarising older events through
ADK's `BaseEventSummarizer` rather than dropping them.

## What it does

ADK's context-engineering subsystem calls the registered `EventCompactor`
periodically. Regulus' implementation:

- **Drops** events older than the profile's `summaryRetention` window.
- **Summarises** events between `fullEventRetention` and
  `summaryRetention` via the configured `BaseEventSummarizer`.
- **Honours erasure** during the retention window when the active profile
  permits it (GDPR / UK GDPR / EHDS / DORA / EU AI Act); refuses erasure
  when the profile overrides it (FCA SYSC, PRA SS1/23, PRA SS2/21, NHS
  DSPT).

## When to use it

Always, under any profile that has a retention requirement — which is all
of them. The Spring starter wires it automatically when at least one
profile is active.

## Code

```java
RegulusRetentionEventCompactor compactor =
    new RegulusRetentionEventCompactor(profile);
```

Register on the `App`:

```java
App app = App.builder("my-agent", rootAgent)
    .eventCompactor(compactor)
    // ... plugins ...
    .build();
```

## Picking the window

For each active profile the compactor uses
`EventCompactionPolicy.fullEventRetention()` and `summaryRetention()`.
With multiple profiles, the composite picks the *longest* window — so a
GDPR + DORA agent retains the full 5 years DORA demands.

## See also

- [Concepts → Audit trails — Retention](../concepts/audit-trails.md#retention)
- [`RegulusAuditPlugin`](../plugins/audit.md)
