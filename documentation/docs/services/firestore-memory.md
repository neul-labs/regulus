# RegulusFirestoreMemoryService

## In one sentence

A drop-in replacement for ADK's `FirestoreMemoryService` that triggers
retention-aware compaction on every write and exposes a GDPR Art. 17
erasure path.

## What it does

Wraps the Firestore-backed `MemoryService` and adds:

- **Region pinning** (same as the session service variant).
- **Erasure path.** Subject-keyed erasure produces a tombstone.
- **Retention awareness.** Every write checks the active profile's
  retention window; older memories are summarised on read via the
  configured `BaseEventSummarizer`.

## When to use it

When the agent needs long-term memory across sessions and you want the
storage-limitation principle (GDPR Art. 5(1)(e)) enforced automatically.

## Code

```yaml
regulus:
  adk:
    memory-service:
      kind: firestore
      project-id: ${GOOGLE_CLOUD_PROJECT}
      location: europe-west2
```

(Note: the example config schema can be extended; the Spring starter
exposes this set of fields by default.)

## Failure modes

- Region outside allowlist → constructor throws.
- Active profile doesn't permit erasure but a delete is requested → returns
  a `RetentionOverride` audit event instead of dropping the data.

## See also

- [`RegulusRetentionEventCompactor`](retention-event-compactor.md)
- [Concepts → Audit trails](../concepts/audit-trails.md)
