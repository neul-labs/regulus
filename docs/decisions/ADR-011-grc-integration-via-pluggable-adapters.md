# ADR-011: GRC integration via pluggable adapters

- Status: Accepted
- Date: 2026-05-13

## Context

Enterprise buyers run GRC programs on tools they already own:
ServiceNow IRM, OneTrust AI Governance, MetricStream, LogicGate,
Riskonnect, RSA Archer, IBM OpenPages. To be useful in those
deployments, Regulus must feed evidence *into* the tool — not require
the tool to read Regulus' Kafka topic.

The vendor REST surfaces are similar in shape (POST an evidence record)
but different in detail. Tenants customise the schemas inside each
vendor.

## Decision

Ship a **`GrcEvidenceAdapter`** SPI in the new
`regulus-ai-grc-adapters` module, plus six concrete adapters and a
canonical envelope:

```java
public interface GrcEvidenceAdapter {
    String vendorId();
    void emit(GrcEvidenceEnvelope envelope);
    default void healthCheck() { /* override to fail-loud at startup */ }
}
```

Shipped adapters:

- `ServiceNowIrmAdapter` — `sn_grc_control_evidence` table; OAuth2 or basic auth.
- `OneTrustAiGovernanceAdapter` — `/api/aigov/v1/evidence`; API key.
- `MetricStreamAdapter` — `/api/v1/intake/{app}/evidence`; bearer.
- `WebhookAdapter` — generic JSON + HMAC-SHA256 signature.
- `StdoutAdapter` — development.
- `KafkaAdapter` — Spring Boot starter wraps with a Spring-Kafka producer.

Each vendor adapter exposes a `fieldMappings` map (sensible defaults +
override) because tenant-customised vendor schemas are the rule, not
the exception.

A new ADK plugin, `RegulusGovernanceEvidencePlugin`, fans audit events
through the configured adapters, with one envelope per matching
framework binding. Adapter failures are caught and surfaced as their
own audit events (`grc-adapter-failure`) — the agent does NOT fail
because a downstream GRC tool is unreachable.

`AdapterHealthCheck` runs at startup and refuses to activate if any
configured adapter can't reach its target (same shape as ADR-008
residency by construction).

## Why pluggable rather than canonical

- **No canonical wins.** Trying to standardise on one GRC schema
  (OSCAL, OpenControl, custom JSON) means losing the vendor-specific
  workflow advantages each tool offers. Buyers won't migrate workflows
  to fit our schema; we migrate emission to fit theirs.
- **Tenant variance is the norm.** Even within one vendor, each
  enterprise tenant has customised fields, custom apps, and custom
  intake schemas. The `fieldMappings` override surface acknowledges
  this.
- **Webhook + HMAC catches the long tail.** LogicGate, Riskonnect, RSA
  Archer, IBM OpenPages, bespoke internal pipelines — the generic
  webhook plus HMAC signing covers them all without a dedicated
  adapter per vendor.

## Alternatives considered

1. **Single canonical format, customers write adapters.** Rejected —
   pushes integration cost to every buyer. We absorb it by shipping
   the four most-common adapters.
2. **No GRC integration; let customers consume the Kafka topic.**
   Rejected — GRC teams don't usually have Kafka skills, and the
   "tool sits next to the event stream" arrangement doesn't work for
   most enterprises.
3. **Fail-open on adapter errors.** Rejected at runtime (catch + audit),
   accepted at startup (health check). The runtime is operational; the
   startup is a configuration safety net.

## Consequences

Positive: drop-in for the four most-deployed GRC tools; clear extension
path for everything else.

Negative: vendor adapters are best-effort against documented public
surfaces; field mappings will need maintenance per release. We document
the override pattern prominently so adopters can compensate without
forking.
