# RegulusFirestoreSessionService

## In one sentence

A drop-in replacement for ADK's `FirestoreSessionService` adding region
pinning, field-level encryption hooks, and an audited erasure path for
GDPR Art. 17.

## What it does

Wraps the Firestore-backed `SessionService` and adds:

- **Region pinning.** Constructor refuses to start if the Firestore
  database's location isn't on the allowlist. (Default Firestore databases
  sit in `nam5` — US multi-region — which the plugin catches.)
- **Erasure path.** `deleteSession(sessionId)` removes the session payload
  and writes a tombstone event into the audit log. The tombstone records
  *that* an erasure happened without leaking *what* was erased.
- **Field-level encryption hooks.** Optional callbacks to encrypt session
  fields before they hit Firestore — useful when CMEK at rest isn't enough
  (e.g. cross-team Firestore access).

## When to use it

When you need session continuity across instances and you don't have
Vertex AI managed sessions on the same project, or when you want field-
level encryption beyond what CMEK provides.

## Code

```yaml
regulus:
  adk:
    session-service:
      kind: firestore
      project-id: ${GOOGLE_CLOUD_PROJECT}
      location: europe-west2
```

In bare Java:

```java
RegulusFirestoreSessionService sessions = RegulusFirestoreSessionService.wrap(
    projectId, "europe-west2", profile.residency());
```

## Failure modes

- Default Firestore in `nam5` → constructor throws.
- Region mismatch with the Vertex location → not caught here, but flagged
  at runtime by `RegulusDataResidencyPlugin`.

## See also

- [`RegulusVertexAiSessionService`](vertex-ai-session.md) — managed option.
- [`RegulusFirestoreMemoryService`](firestore-memory.md) — memory variant
  with the same shape.
