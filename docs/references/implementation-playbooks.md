# Implementation Playbooks

These playbooks translate Regulus specifications into actionable steps for engineering, platform, and safety teams. Each section outlines prerequisites, adapter abstractions, and validation tasks to reach production readiness.

## 1. Model Inventory Registration

- **Objective**: Synchronise annotated agents (`@ModelArtefact`) with the enterprise model inventory.
- **Prerequisites**: Inventory API credentials, schema documentation, Vault path for secrets, contact in Model Risk.
- **Adapter**: Implement `ModelInventoryClient` (see `./integration-matrix.md`) with retry/backoff and dead-letter queue for failures.
- **Steps**:
  1. Map annotation fields (owner, purpose, risk tier, review cadence) to the inventory payload.
  2. Store OAuth client credentials in Vault; wire into Spring via `SecretProvider`.
  3. Emit registration events on startup and change events (policy updates, version bumps).
  4. Store inventory IDs returned from the enterprise model inventory locally for audit correlation and traceability.
- **Validation**: Perform dry-run in lower environment; confirm records appear with correct metadata; simulate API outage and ensure retry logic works.
- **Runbook**: Document how to re-sync an agent, rotate credentials, and reconcile discrepancies.

## 2. GRC Evidence Upload

- **Objective**: Deliver model cards, eval reports, and approvals to the GRC repository.
- **Prerequisites**: Agreement on transfer mechanism (REST/SFTP), file naming conventions, retention tags.
- **Adapter**: `GrceEvidenceUploader` invoked by CI Gradle tasks (`aiEvalCheck`, `regulusRiskSim`).
- **Steps**:
  1. Negotiate payload schema with GRC team; include model ID, change ticket, artefact type.
  2. Configure secure transport (mutual TLS or signed SFTP key) and verify firewall rules.
  3. Enhance Gradle tasks to stage artefacts locally, then hand off to uploader with retry.
  4. Emit success/failure metrics to OTEL for audit and troubleshooting.
- **Validation**: Run a CI pipeline in non-prod, confirm artefact ingestion; test malformed payload handling.
- **Runbook**: Provide manual fallback for urgent uploads and instructions for pulling acknowledgement receipts.

## 3. Kill Switch Control Plane

- **Objective**: Operate the `KillSwitchInterceptor` via enterprise config services.
- **Prerequisites**: ConfigHub topic or Vault KV namespace, ServiceNow change workflow, dual-control roster.
- **Adapter**: `KillSwitchConfigAdapter` supporting bus-driven refresh and polling fallback.
- **Steps**:
  1. Register Regulus services with ConfigHub listeners; set TTL and refresh intervals.
  2. Define toggle structure (`regulus/kill-switch/<agent|tool|connector>`), metadata fields (ticket, approver).
  3. Integrate ServiceNow API calls to log activation/deactivation with change IDs.
  4. Configure alerting (Prometheus/Splunk) when kill state changes.
- **Validation**: Execute sandbox activation; verify interceptor behaviour, audit trail, and alerting.
- **Runbook**: Step-by-step kill activation, approval sequence, rollback, and evidence capture.

## 4. Eval / Red-Team Service Integration

- **Objective**: Route prompts/responses to the Python container for automated gate checks.
- **Prerequisites**: Deployment of `ghcr.io/regulus/ai-evals`, network zoning approval, mutual TLS certificates.
- **Adapter**: `EvalClient` within the starter plus Gradle `aiEvalCheck` task configuration.
- **Steps**:
  1. Deploy container alongside ADK agents or in shared safety namespace; configure autoscaling.
  2. Define API contract (`/eval`, `/suite`) with payload redaction policy; enforce JSONPath masks before sending.
  3. Configure secrets (API keys, certs) via Vault; mount into both CI and runtime environments.
  4. Collect metrics (suite pass rate, latency) and surface in risk dashboards.
- **Validation**: Trigger eval run from sample agent; inspect results, ensure gating behaviour, test failure handling.
- **Runbook**: Update test suites, respond to eval failures, rotate certificates, restart container.

## 5. Safety Model Deployment (On-Prem SLM/Classifiers)

- **Objective**: Host internal models that emit risk signals (vulnerability, mis-sell, tone) consumed by policy guards.
- **Prerequisites**: Model artefacts (ONNX/GGUF), training documentation, bias evaluation, container/runtime plan (DJL, TensorFlow Serving).
- **Adapter**: Implement `SafetyClassifierTool` (ADK tool) or expose via MCP server if shared across services.
- **Steps**:
  1. Package the model with DJL or preferred serving stack; include health probes and metrics.
  2. Register the tool in the DSL with declared outputs (e.g., `vulnerabilityFlag`, `misSellScore`) and map to policy rules.
  3. Wire logging and evals to monitor classifier precision/recall; feed drift metrics into safety dashboards.
  4. Synchronise model metadata with the enterprise model inventory, tagging training data lineage and retraining cadence.
- **Validation**: Run targeted scenario catalog entries; confirm flags trigger policy outcomes (escalation, denial).
- **Runbook**: Retraining workflow, model promotion checklist, rollback plan if precision drops below threshold.

## 6. Vendor / Outsourcing Registry Sync

- **Objective**: Record MCP/A2A/LLM endpoints, due diligence, and exit tests with Third-Party Risk.
- **Prerequisites**: Registry schema, ownership in vendor management team, data classification for submissions.
- **Adapter**: `VendorRegistryPublisher` invoked during connector configuration and change management.
- **Steps**:
  1. Identify required fields (region, subprocessors, contract ID, exit-test date); map to configuration sources.
  2. Implement push (API) or scheduled export aligned to registry ingestion cadence.
  3. Include reconciliation report so discrepancies are visible to risk analysts.
  4. Tie submissions to ServiceNow records for audit traceability.
- **Validation**: Submit test connector; verify registry entry, reconcile diff report.
- **Runbook**: Update vendor entries, handle failed sync, trigger exit test documentation.

## 7. Interop Security (OAuth/mTLS)

- **Objective**: Secure MCP/A2A traffic with bank-standard authentication and RBAC.
- **Prerequisites**: PKI integration, OAuth scope definitions, certificate issuance workflow.
- **Adapter**: `InteropSecurityConfig` that configures OAuth clients, mTLS trust stores, and scope enforcement.
- **Steps**:
  1. Register clients in IAM; obtain client IDs, secret rotation policy, scope catalogue.
  2. Automate certificate issuance/renewal; store in Vault and load via Spring TLS config.
  3. Implement scope checks at inbound controllers and outbound client wrappers.
  4. Test failure modes (expired cert, invalid scope) and ensure graceful degradation with audit logging.
- **Validation**: Conduct mTLS handshake tests, OAuth token retrieval in lower environments; run penetration test if mandated.
- **Runbook**: Certificate rotation calendar, scope update process, incident response for auth failures.

## 8. Observability & Audit Hooks

- **Objective**: Stream metrics, traces, and audit events to OTEL/Kafka/Splunk stacks.
- **Prerequisites**: Topic provisioning, dashboard owners, retention policies.
- **Adapter**: Auto-configured exporters plus `AuditEventPublisher`.
- **Steps**:
  1. Set environment-specific endpoints for OTLP, Kafka brokers, and Splunk HTTP Event Collector.
  2. Define event schema (kill switch, eval gate, policy violation) and ensure context IDs align with ServiceNow tickets.
  3. Configure sampling and retention according to regulatory requirements.
  4. Validate dashboards and alerts with Observability team before production cutover.
- **Validation**: Run synthetic transactions; confirm end-to-end trace visibility and audit event persistence.
- **Runbook**: Handle exporter outages, update dashboards, rotate HEC tokens.

> Tailor each playbook to bank-specific platforms before implementation begins. Maintain versioned copies aligned with sprint deliveries so stakeholders know when integrations are production-ready.
