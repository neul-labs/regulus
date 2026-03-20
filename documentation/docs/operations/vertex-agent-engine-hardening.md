# Vertex AI Agent Engine hardening

Production-readiness checklist for Regulus-on-ADK agents deployed to
Vertex AI Agent Engine.

## Pre-deploy

- `./gradlew regulusAdkDoctor regulusComplianceScan` passes.
- Profiles match the regulatory scope agreed with legal / compliance.
- Residency allowlist explicit; no fallback to the empty case.
- CMEK key exists in the same region.
- Audit Kafka topic configured with retention matching the profile.
- Kill-switch persistent store provisioned (not in-memory).

## Identity and access

- Service account for Agent Engine has *only* the necessary IAM roles.
- Operator identities for the kill-switch admin endpoint are SSO-backed.
- Cloud Audit Logs enabled at admin / read / write level for Vertex AI
  resources.

## Network

- Egress restricted via VPC Service Controls where possible.
- Outbound DNS resolution for the LLM provider's endpoints — and only
  those — allowed.

## Observability

- Audit topic monitored (lag + dead-letter rate).
- Prometheus scraping `/actuator/prometheus`.
- Tracing (OpenTelemetry) wired through to your APM.
- Synthetic transaction every N minutes; alert if it fails.

## Disaster recovery

- Kill-switch store has a documented recovery time.
- Audit topic replicated to a second region (or compacted to Object Lock
  for archive).
- Runbook for "Vertex AI Agent Engine is down" — what happens to in-flight
  requests, what the customer experience is, how recovery proceeds.

## Compliance evidence

- Coverage matrix regenerated and reviewed at each release.
- Quarterly access review on the audit topic + kill-switch store.
- Annual rehearsal of the SAR + erasure + DORA / NIS2 notification paths.
