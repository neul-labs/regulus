# Deploy to Vertex AI Agent Engine

Push your Regulus-wrapped ADK agent to Vertex AI Agent Engine as a managed
runtime, with audit going to Kafka and residency pinned to UK or EU.

## Prerequisites

- Vertex AI Agent Engine enabled on your GCP project.
- Cloud KMS key in the same region as your Vertex resources, exposed via
  `REGULUS_CMEK_KEY`.
- A Kafka cluster (Confluent / Managed Kafka) reachable from Agent Engine.
- A container image — we use Jib.

## 1. Production configuration

```yaml
regulus:
  compliance:
    profiles: [eu-ai-act, uk-gdpr, fca-sysc, pra-ss1-23]
  adk:
    name: production-agent
    session-service:
      kind: vertex-ai
      project-id: ${GOOGLE_CLOUD_PROJECT}
      location: europe-west2
      cmek-key-name: ${REGULUS_CMEK_KEY}
    audit:
      sink: kafka
      kafka-topic: audit.regulus.v1
    kill-switch:
      enabled: true
      dual-control: true
    residency:
      allowed-regions: [europe-west2]
      require-cmek: true
    model-risk:
      tenant-tier: REGULATED
```

## 2. Build the container

```bash
./gradlew :examples:adk-vertex-agent-engine-deploy:jib
```

Jib pushes `ghcr.io/skelf-research/regulus-adk-demo:<version>` (or
wherever you configure).

## 3. Run the build-time doctor

```bash
./gradlew regulusAdkDoctor
```

This checks ADK + Regulus version compatibility, residency wiring,
signing config, and refuses to proceed on misconfiguration. Designed to
fail your CI before `adk deploy` ever runs against a misconfigured
service.

## 4. Deploy

```bash
adk deploy --image ghcr.io/skelf-research/regulus-adk-demo:0.1.0 \
           --location europe-west2 \
           --project $GOOGLE_CLOUD_PROJECT
```

## 5. Verify in Agent Engine

In the Vertex console:

- Confirm the agent's region is `europe-west2`.
- Inspect the Cloud Logging stream — Regulus audit events appear under
  the Kafka producer's path, plus startup logs from each plugin.
- Send a sample request via the Agent Engine REST API. Confirm the
  audit topic in Kafka receives the expected events.

## 6. Operate

- [Operations → Audit retention runbook](../operations/audit-retention-runbook.md)
- [Operations → Kill-switch playbook](../operations/kill-switch-playbook.md)
- [Operations → Vertex AI Agent Engine hardening](../operations/vertex-agent-engine-hardening.md)
