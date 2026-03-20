# RegulusVertexAiSessionService

## In one sentence

A drop-in replacement for ADK's `VertexAiSessionService` that validates
residency at construction time, tags every session with a CMEK key
reference, and refuses to start if the configured location is outside the
allowlist.

## What it does

Wraps the Vertex-managed `SessionService` and adds:

- **Residency check.** Constructor inspects the configured `location`; if
  it's not in the active profile's `ResidencyPolicy.allowedRegions()`, the
  service throws and the ADK `App` doesn't start.
- **CMEK enforcement.** If the profile requires CMEK and no
  `cmekKeyName` is configured, construction fails.
- **Audit envelope.** Every session create / get / delete emits an audit
  event with the session id, region, and key id (not the key material).

## When to use it

Whenever your active profile has any of `gdpr`, `uk-gdpr`, `dora`,
`fca-sysc`, `pra-*`, `nhs-dspt`, `ehds` — i.e. effectively always for
production agents.

## Code

```yaml
regulus:
  adk:
    session-service:
      kind: vertex-ai
      project-id: ${GOOGLE_CLOUD_PROJECT}
      location: europe-west2
      cmek-key-name: projects/.../cryptoKeys/regulus-sessions
```

In bare Java:

```java
RegulusVertexAiSessionService sessions = RegulusVertexAiSessionService.wrap(
    projectId, "europe-west2", cmekKeyName, profile.residency());
```

## Failure modes

- Wrong region → constructor throws with a clear citation of the
  allowlist.
- CMEK required but missing → constructor throws.
- Runtime drift (region changes after start) → next request blocked by
  `RegulusDataResidencyPlugin`.

## See also

- [`RegulusDataResidencyPlugin`](../plugins/data-residency.md)
- [Concepts → Data residency](../concepts/data-residency.md)
