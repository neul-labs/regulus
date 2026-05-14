# adk-vertex-agent-engine-deploy — Regulus on Vertex AI Agent Engine

`adk deploy` an agent to Vertex AI Agent Engine with Regulus baked in. This is
the path most teams take to production after the quickstart.

The configuration here mirrors a regulated-financial-services deployment:
EU AI Act + UK GDPR + FCA SYSC + PRA SS1/23, residency pinned to
`europe-west2`, CMEK required, audit to Kafka, model risk capped at `REGULATED`
tier.

## Prerequisites

- Java 21
- GCP project with Vertex AI + Cloud KMS + (optionally) Pub/Sub or Managed
  Kafka enabled
- `gcloud auth application-default login`
- A CMEK key in `europe-west2` exposed via `REGULUS_CMEK_KEY`
- `ghcr.io` push credentials if you want to push the container image

## Build the container

```bash
./gradlew :examples:adk-vertex-agent-engine-deploy:jib
```

Jib pushes `ghcr.io/neul-labs/regulus-adk-demo:<version>`.

## Deploy with the ADK CLI

```bash
adk deploy --image ghcr.io/neul-labs/regulus-adk-demo:0.1.0 \
           --location europe-west2 \
           --project $GOOGLE_CLOUD_PROJECT
```

Regulus runs `regulusAdkDoctor` automatically (when invoked via Gradle) before
this step is reachable, so a misconfigured residency or missing CMEK fails the
build, not the deploy.

## What an auditor sees in Agent Engine logs

Every agent invocation produces a structured audit event:

```json
{
  "event_id": "01J6X4...",
  "occurred_at": "2026-05-14T11:23:09.123Z",
  "actor": "user:12345",
  "smf_holder": "SMF24:Jane Smith",
  "action": "model-call",
  "result": "allow",
  "model_id": "gemini-2.5-pro",
  "model_version": "2026-05-01",
  "ai_act_risk_tier": "limited",
  "consumer_duty_outcome": "support",
  "fca_lei": "213800ABC123",
  "redactions": ["NINO_1"]
}
```

These fields are the union of what EU AI Act Art. 12, GDPR Art. 30, FCA SYSC
9, and PRA SS1/23 each require. Regulus refuses to emit incomplete events so
the auditor never has to chase missing context.

## Where to read next

- [Operations — Vertex AI Agent Engine hardening](../../documentation/docs/operations/vertex-agent-engine-hardening.md)
- [Operations — audit log retention runbook](../../documentation/docs/operations/audit-retention-runbook.md)
- [Compliance — PRA SS1/23](../../documentation/docs/compliance/uk/pra-ss1-23.md)
