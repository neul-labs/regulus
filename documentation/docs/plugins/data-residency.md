# RegulusDataResidencyPlugin

## In one sentence

Validates that every wired `SessionService` / `MemoryService` /
`ArtifactService` + model endpoint sits in an allowed region — at startup,
fail-closed — and re-checks at runtime.

## Who does it apply to?

Anyone covered by GDPR, UK GDPR, DORA, FCA SYSC 13, PRA SS2/21 §6, NHS
DSPT 8.x, EHDS Chapter III. Effectively, every regulated AI agent.

## The two-minute explainer

The plugin runs two checks:

- **Startup.** Triggered by the Regulus Spring starter (or by your own
  bootstrap if you wire plugins by hand). Inspects each wired service's
  configured region and refuses to activate the `App` if any sits outside
  the allowlist.
- **Per call.** A `BeforeAgentCallback` re-validates the wired services'
  current configuration. Defensive — catches the (rare) case of runtime
  rewiring.

The allowlist comes from two sources, with the YAML overriding the profile
default:

1. `ResidencyPolicy.allowedRegions()` on the active composite profile.
2. `regulus.adk.residency.allowed-regions` in YAML.

If both are empty, the plugin is permissive — useful for non-personal-data
internal agents. Most deployments populate one or the other.

## What it actually requires of an engineer

- Configure the regions your tenant is allowed to operate in.
- Ensure the Firestore / GCS / Vertex AI region matches before deploy.
- If CMEK is required by the profile (`dora`, `nhs-dspt`, `fca-sysc`,
  `pra-*`), supply a key name.

## What Regulus does for you

- Fail-closed startup. No "we'll warn in the logs" mode.
- Per-call re-check.
- Audit event on any blocked invocation.

## Saves you ~

- ~2 engineer-weeks for the allowlist + startup hook + evidence export.

## Code: minimal

```java
RegulusDataResidencyPlugin residency = RegulusDataResidencyPlugin
    .allow("europe-west2");
```

## Code: production

```yaml
regulus:
  adk:
    residency:
      allowed-regions: [europe-west2]
      require-cmek: true
    session-service:
      kind: vertex-ai
      project-id: ${GOOGLE_CLOUD_PROJECT}
      location: europe-west2
      cmek-key-name: projects/.../cryptoKeys/regulus-sessions
```

The plugin and `RegulusVertexAiSessionService` will each refuse to start
if `location` is outside `[europe-west2]`.

## How to verify

```bash
$ ./gradlew bootRun -Dregulus.adk.session-service.location=us-central1
RegulusVertexAiSessionService refused to start: location 'us-central1'
is not in the residency allowlist [europe-west2]
```

## What an auditor will ask

1. **"Show me the allowlist."** YAML / `ResidencyPolicy`.
2. **"Show me what happens when something is misconfigured."** Force a
   misconfiguration; show the startup refusal.
3. **"What about model endpoints?"** The Vertex AI client uses the same
   region; show its config.

## What this doesn't cover

- **Outbound network egress.** That's a network-layer control (VPC-SC,
  firewall rules) — orthogonal.
- **Cross-region replication of your data outside Regulus-aware
  services.** Audit your data flows separately.
- **The transfer-paperwork side** (SCCs / IDTA). Paper, not code.

## Citations

See [Concepts → Data residency](../concepts/data-residency.md), plus the
compliance pages for [GDPR](../compliance/eu/gdpr.md),
[UK GDPR](../compliance/uk/uk-gdpr.md),
[PRA SS2/21](../compliance/uk/pra-ss2-21.md).
