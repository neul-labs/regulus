# GRC integration

How Regulus' evidence stream lands in your GRC tool.

## The model

`RegulusGovernanceEvidencePlugin` runs downstream of `RegulusAuditPlugin`.
For every audit event whose `mechanism` is bound to a control by the
active `GovernanceFramework`, the plugin builds a
`GrcEvidenceEnvelope` and dispatches it to every configured
`GrcEvidenceAdapter`.

```
audit event
   │
   ▼
RegulusAuditPlugin            → audit topic (always)
   │
   ▼
RegulusGovernanceEvidencePlugin
   │
   ├──► ServiceNowIrmAdapter      → sn_grc_control_evidence
   ├──► OneTrustAiGovernanceAdapter → /api/aigov/v1/evidence
   ├──► MetricStreamAdapter       → /api/v1/intake/<app>/evidence
   ├──► WebhookAdapter (HMAC)     → arbitrary endpoint
   └──► KafkaAdapter              → topic for any subscriber
```

Adapters are **opt-in**: no adapter is wired by default. Each is
enabled and configured per-tenant in YAML.

## Pages

- [ServiceNow IRM](servicenow-irm.md)
- [OneTrust AI Governance](onetrust.md)
- [MetricStream](metricstream.md)
- [Generic webhook](webhook.md)

## Why pluggable

Two reasons:

1. **Vendor diversity.** ServiceNow / OneTrust / MetricStream / LogicGate /
   Riskonnect / RSA Archer / IBM OpenPages all have valid customer bases.
   A single canonical format would lose half of them.
2. **Tenant schema variance.** Even within one vendor, customers have
   custom fields, custom apps, custom workflows. Every adapter exposes a
   `fieldMappings` override so a deploying team can bind to their tenant
   without forking Regulus.

See [ADR-011](https://github.com/neul-labs/regulus/blob/main/docs/decisions/ADR-011-grc-integration-via-pluggable-adapters.md)
for the rationale.

## Fail-loud at startup

`AdapterHealthCheck` runs before the ADK `App` activates. If any
configured adapter can't reach its target — bad URL, bad credentials,
firewall rule — the application refuses to start. Same shape as
[residency by construction](https://github.com/neul-labs/regulus/blob/main/docs/decisions/ADR-008-residency-by-construction.md).

## What an adapter does *not* do

- **Define the controls.** The GRC tool's control library does that.
  Adapters reference controls by id.
- **Determine whether a control "passed."** Regulus emits the audit
  fact (`result=pass`, `result=fail`, etc.). The GRC tool's testing
  workflow decides what to do with it.
- **Push policies into the agent.** Policy flow is the other direction.
  Regulus enforces policy in code; the GRC tool stores the canonical
  text.

## Authentication

| Adapter | Auth |
|---|---|
| ServiceNow IRM | Basic auth or OAuth2 bearer token |
| OneTrust AI Governance | API key (`X-OneTrust-API-Key`) |
| MetricStream | Bearer token |
| Webhook | HMAC-SHA256 signature in `X-Regulus-Signature` header |

Credentials are supplied via Spring properties (`regulus.grc.<vendor>.*`)
and should come from a secrets store (Vault, GSM, Secret Manager) — not
the YAML file itself. Resist the temptation.
