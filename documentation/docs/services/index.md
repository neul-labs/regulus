# Services

Service-level extensions on top of ADK's official `SessionService`,
`MemoryService`, `ArtifactService`, `EventCompactor`, and `BaseComputer`
interfaces. Each wraps the Google-shipped implementation with:

- Fail-closed residency validation at construction.
- CMEK enforcement where the profile requires it.
- Retention-aware compaction.
- An audit envelope on every lifecycle event.

## At a glance

| Class | Wraps / extends | Adds |
|---|---|---|
| [`RegulusVertexAiSessionService`](vertex-ai-session.md) | `VertexAiSessionService` | Residency check at construction; CMEK key tagging; per-session audit |
| [`RegulusFirestoreSessionService`](firestore-session.md) | `FirestoreSessionService` | Region pinning; field-level encryption hooks; GDPR Art. 17 erasure path |
| [`RegulusFirestoreMemoryService`](firestore-memory.md) | `FirestoreMemoryService` | Retention-aware compaction; erasure path |
| [`RegulusGcsArtifactService`](gcs-artifact.md) | `GcsArtifactService` | Bucket residency + CMEK; sensitive-artifact tagging |
| [`RegulusRetentionEventCompactor`](retention-event-compactor.md) | implements `EventCompactor` | Picks retention window from active profile; runs summarisation past horizon |
| [`RegulusComplianceBaseComputer`](compliance-base-computer.md) | implements `BaseComputer` | Domain allowlist; PII redaction on screenshots; HITL on high-risk actions |
| [`regulus-ai-adk-a2a`](a2a.md) | Wraps `AgentExecutor` + `RemoteA2AAgent` | Applies the Regulus envelope to A2A hops |

## Why this matters

ADK ships extension points; Regulus ships compliant implementations on
those exact extension points. You can mix and match — use Regulus' Vertex
session service with Google's GCS artifact service, or vice versa — without
the integration friction of running two parallel object models.
