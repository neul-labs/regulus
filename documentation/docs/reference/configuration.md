# YAML configuration

Every property under `regulus.*` consumed by the Spring Boot starter.

## `regulus.compliance.profiles`

List of profile ids to activate. The composite picks the *stricter* of
any conflicting setting.

Allowed values: `eu-ai-act`, `gdpr`, `uk-gdpr`, `dora`, `nis2`, `fca-sysc`,
`pra-ss1-23`, `pra-ss2-21`, `nhs-dspt`, `ehds`.

```yaml
regulus:
  compliance:
    profiles: [eu-ai-act, uk-gdpr, fca-sysc]
```

## `regulus.adk.name`

Default: `regulus-agent`. The agent name registered on the ADK `App`.

## `regulus.adk.session-service`

| Property | Default | Notes |
|---|---|---|
| `kind` | `in-memory` | `in-memory` \| `vertex-ai` \| `firestore` |
| `project-id` | — | Required for `vertex-ai` / `firestore` |
| `location` | — | Required; must be in residency allowlist |
| `cmek-key-name` | — | Required if profile requires CMEK |

## `regulus.adk.audit`

| Property | Default | Notes |
|---|---|---|
| `sink` | `stdout` | `stdout` \| `kafka` (or custom bean) |
| `kafka-topic` | `audit.regulus.v1` | Used when sink is `kafka` |

## `regulus.adk.kill-switch`

| Property | Default | Notes |
|---|---|---|
| `enabled` | `true` | |
| `dual-control` | `true` | |

## `regulus.adk.residency`

| Property | Default | Notes |
|---|---|---|
| `allowed-regions` | — | When empty, falls back to the composite profile's allowlist |
| `require-cmek` | `false` | OR'd with the profile setting |

## `regulus.adk.model-risk`

| Property | Default | Notes |
|---|---|---|
| `tenant-tier` | `STANDARD` | `EXPERIMENTAL` \| `STANDARD` \| `REGULATED` \| `HIGH_RISK` |

## `regulus.adk.dev-server`

| Property | Default | Notes |
|---|---|---|
| `enabled` | `false` | Enables the optional `google-adk-dev` UI |
| `port` | `8081` | |
