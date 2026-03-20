# RegulusAuditPlugin

## In one sentence

Emits a structured immutable audit event for every consequential agent
action, with retention windows picked by the active compliance profile.

## Who does it apply to?

Every agent. The audit plugin is the foundation every regulator's question
eventually reaches.

## The two-minute explainer

Two responsibilities:

1. **Emit events** on `AfterAgentCallback`, `AfterModelCallback`,
   `AfterToolCallback`, plus on policy / kill-switch / residency events.
   The event schema is the union of fields required by the active profiles
   (see [Concepts → Audit trails](../concepts/audit-trails.md)).
2. **Compact events** via a custom `EventCompactor`
   (`RegulusRetentionEventCompactor`) registered on the `App`. Old raw
   events are summarised via `BaseEventSummarizer`; the summary retains
   audit-relevant fields but drops user-content payloads when storage
   limitation requires it.

The plugin refuses to emit events missing required fields. This is
deliberate fail-loud behaviour: a half-event in the trail is worse than no
event, because it makes the trail look complete when it isn't.

## What it actually requires of an engineer

- Pick an audit sink: `stdout` for dev, `kafka` for prod, custom for
  anything else.
- Populate invocation context with the profile-required fields (the Spring
  starter handles the common ones; add tenant-specific ones via a
  `PolicyContext` or response decorator).
- Plan storage for the retention window — see
  [Concepts → Audit trails](../concepts/audit-trails.md).

## What Regulus does for you

- Schema enforcement: missing required field → fail-loud, not silent.
- Composition: a request that triggers GDPR + AI Act + FCA SYSC fields
  emits one event with the union, not three events.
- Retention: regulation-aware. `RegulusRetentionEventCompactor` picks the
  longest window across active profiles; older events are summarised.
- Erasure path: where the active profile allows it, GDPR Art. 17 erasure
  produces a tombstone, not a hole.

## Saves you ~

- ~6 engineer-weeks for the pipeline + retention + erasure + monotonic
  immutability + tests.
- Ongoing: retention adjustment as regulations shift.

## Code: minimal

```java
RegulusAuditPlugin audit = RegulusAuditPlugin
    .forProfile(profile)
    .toSink(AuditSink.stdout())
    .build();
```

## Code: production

```yaml
regulus:
  adk:
    audit:
      sink: kafka
      kafka-topic: audit.regulus.v1
```

Plus the Spring starter wires a `KafkaTemplate`-backed sink that emits
JSON. For custom destinations (BigQuery, S3 Object Lock, etc.) provide an
`AuditSink` bean:

```java
@Bean
AuditSink bigQueryAuditSink(BigQuery bq) {
    return event -> bq.insertAll(InsertAllRequest.newBuilder("audit", "events")
        .addRow(event).build());
}
```

## How to verify

- Trigger a request → expect events for `before-agent`, `before-model`,
  `after-model`, `after-agent` (at minimum).
- Trigger a policy block → expect a `policy.block` event with
  `clause_citation` populated.
- Run the compaction task → events older than the raw-retention window are
  summarised.

## What an auditor will ask

1. **"Where do these go?"** Sink configuration; topic/bucket access policy.
2. **"How are they protected from modification?"** Kafka topic compaction
   off / Object Lock / signing — depending on the immutability hint of the
   profile.
3. **"How long do you keep them?"** Per-profile retention; compactor task
   logs.
4. **"How do you action an Art. 15 SAR?"** Subject-linked query +
   redaction-aware output.

## What this doesn't cover

- **Operating the audit storage.** Capacity, access, archival to glacier
  tiers — yours.
- **External SIEM integration.** Connect the sink to your SIEM via the
  usual Kafka / log-forwarding path.

## Citations

See [Concepts → Audit trails](../concepts/audit-trails.md) and the
regulation pages.
