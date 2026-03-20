# Operational Runbooks

Standard operating procedures for Regulus platform operations. These runbooks are designed for Operations teams, SREs, and on-call engineers.

---

## Quick Reference

| Scenario | Severity | MTTR Target | Runbook |
|----------|----------|-------------|---------|
| Kill Switch Activation | Critical | 5 min | [RB-001](#rb-001-kill-switch-activation) |
| Kill Switch Deactivation | High | 15 min | [RB-002](#rb-002-kill-switch-deactivation) |
| Data Residency Alert | Critical | 10 min | [RB-003](#rb-003-data-residency-violation) |
| LLM Provider Outage | High | 5 min | [RB-004](#rb-004-llm-provider-failover) |
| Agent Performance Degradation | Medium | 30 min | [RB-005](#rb-005-performance-degradation) |
| Model Drift Detection | High | 1 hour | [RB-006](#rb-006-model-drift-response) |
| Security Incident | Critical | Immediate | [RB-007](#rb-007-security-incident) |
| Compliance Audit Prep | Low | 1 week | [RB-008](#rb-008-audit-preparation) |

---

## RB-001: Kill Switch Activation

### Trigger

- Model drift exceeds threshold
- Security incident detected
- Compliance violation identified
- Unexpected agent behaviour reported
- Regulatory request

### Pre-Requisites

- Two authorized operators available (4-eyes principle)
- Access to kill switch admin panel or CLI
- Incident management system access

### Procedure

#### Step 1: Assess Situation (2 min)

```bash
# Check current agent status
curl -H "Authorization: Bearer $TOKEN" \
  https://api.yourbank.com/api/admin/kill-switch/status | jq

# Check recent errors
curl -H "Authorization: Bearer $TOKEN" \
  "https://api.yourbank.com/actuator/prometheus" | grep regulus_agent_errors
```

**Decision Matrix:**

| Situation | Scope | Emergency |
|-----------|-------|-----------|
| Single agent misbehaving | AGENT | No |
| Agent type showing issues | AGENT_TYPE | No |
| Platform-wide concern | GLOBAL | No |
| Active customer harm | GLOBAL | Yes |
| Regulatory directive | GLOBAL | Yes |

#### Step 2: Request Activation (1 min)

**Standard Activation:**

```bash
# Operator A requests activation
curl -X POST -H "Authorization: Bearer $OPERATOR_A_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "scope": "GLOBAL",
    "target": null,
    "reason": "Model drift detected - accuracy below 90%",
    "emergency": false
  }' \
  https://api.yourbank.com/api/admin/kill-switch/activate
```

**Response:**
```json
{
  "requestId": "KS-20250115-001",
  "status": "PENDING_APPROVAL",
  "expiresAt": "2025-01-15T14:30:00Z"
}
```

**Emergency Activation (immediate effect):**

```bash
curl -X POST -H "Authorization: Bearer $OPERATOR_A_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "scope": "GLOBAL",
    "reason": "Active customer data breach detected",
    "emergency": true
  }' \
  https://api.yourbank.com/api/admin/kill-switch/activate
```

#### Step 3: Approve Activation (1 min)

```bash
# Operator B approves (must be different person)
curl -X POST -H "Authorization: Bearer $OPERATOR_B_TOKEN" \
  https://api.yourbank.com/api/admin/kill-switch/approve/KS-20250115-001
```

#### Step 4: Verify Activation (1 min)

```bash
# Confirm kill switch is active
curl -H "Authorization: Bearer $TOKEN" \
  https://api.yourbank.com/api/admin/kill-switch/status | jq '.global.active'

# Verify agents are rejecting requests
curl -X POST -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"message": "test"}' \
  https://api.yourbank.com/api/agent/mortgage-adviser/chat

# Expected: 503 KILL_SWITCH_ACTIVE
```

#### Step 5: Notify Stakeholders

- [ ] Create incident ticket in ServiceNow
- [ ] Notify Risk Team via Teams/Slack
- [ ] Update status page
- [ ] For emergency: Notify senior management within 30 min

### Rollback

See [RB-002: Kill Switch Deactivation](#rb-002-kill-switch-deactivation)

### Post-Incident

- [ ] Complete incident report within 24 hours
- [ ] Root cause analysis within 72 hours
- [ ] Update runbook if gaps identified

---

## RB-002: Kill Switch Deactivation

### Trigger

- Root cause identified and resolved
- Risk assessment completed
- Approval from Risk/Compliance (if regulatory trigger)

### Pre-Requisites

- Two authorized operators available
- Evidence that original issue is resolved
- Risk sign-off (if applicable)

### Procedure

#### Step 1: Verify Issue Resolution

```bash
# Check model metrics are healthy
curl -H "Authorization: Bearer $TOKEN" \
  https://api.yourbank.com/api/admin/models/mortgage-adviser | jq '.performance'

# Verify no active alerts
curl -H "Authorization: Bearer $TOKEN" \
  https://api.yourbank.com/actuator/health | jq
```

#### Step 2: Request Deactivation

```bash
# Operator A requests deactivation
curl -X POST -H "Authorization: Bearer $OPERATOR_A_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "scope": "GLOBAL",
    "reason": "Issue resolved - model retrained and validated"
  }' \
  https://api.yourbank.com/api/admin/kill-switch/deactivate
```

#### Step 3: Approve Deactivation

```bash
# Operator B approves
curl -X POST -H "Authorization: Bearer $OPERATOR_B_TOKEN" \
  https://api.yourbank.com/api/admin/kill-switch/approve-deactivation/KS-DEACT-001
```

#### Step 4: Gradual Enablement (Optional)

For high-risk scenarios, enable gradually:

```bash
# Enable single agent first
curl -X POST ... -d '{"scope": "AGENT", "target": "mortgage-adviser-canary"}'

# Monitor for 15 minutes
watch -n 5 'curl -s .../actuator/prometheus | grep mortgage-adviser-canary'

# Enable agent type
curl -X POST ... -d '{"scope": "AGENT_TYPE", "target": "mortgage-adviser"}'

# Monitor for 30 minutes, then disable global
```

#### Step 5: Verify and Notify

```bash
# Confirm agents are operational
curl -X POST -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"message": "test"}' \
  https://api.yourbank.com/api/agent/mortgage-adviser/chat

# Expected: 200 OK with response
```

- [ ] Update incident ticket
- [ ] Notify stakeholders of resolution
- [ ] Update status page

---

## RB-003: Data Residency Violation

### Trigger

Alert: `DataResidencyViolation` fired in monitoring

### Severity

**CRITICAL** - Potential regulatory breach

### Procedure

#### Step 1: Assess (2 min)

```bash
# Get violation details
curl -H "Authorization: Bearer $TOKEN" \
  https://api.yourbank.com/api/admin/data-residency/violations | jq '.[0]'
```

**Check:**
- Which region was attempted?
- What data classification?
- Was it blocked or allowed?

#### Step 2: Immediate Actions (5 min)

**If violation was BLOCKED (normal case):**

```bash
# Verify blocking is working
curl -H "Authorization: Bearer $TOKEN" \
  https://api.yourbank.com/api/admin/data-residency/status | jq '.enforced'

# Check for configuration issues
grep -r "us-central1\|us-east-1\|asia" /app/config/
```

**If violation was ALLOWED (critical):**

```bash
# IMMEDIATE: Activate kill switch
curl -X POST -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "scope": "GLOBAL",
    "reason": "Data residency breach - data sent to non-UK region",
    "emergency": true
  }' \
  https://api.yourbank.com/api/admin/kill-switch/activate
```

#### Step 3: Investigation (30 min)

```bash
# Check endpoint configuration
curl -H "Authorization: Bearer $TOKEN" \
  https://api.yourbank.com/actuator/env | jq '.propertySources[] | select(.name | contains("application"))'

# Review audit logs
curl -H "Authorization: Bearer $TOKEN" \
  "https://api.yourbank.com/api/admin/audit?eventType=DATA_RESIDENCY&from=$(date -d '1 hour ago' -Iseconds)"
```

#### Step 4: Escalation

| Situation | Escalate To |
|-----------|-------------|
| Blocked violation | Platform team for config fix |
| Allowed violation | DPO + Risk + Legal + Senior Management |
| Repeat violations | Architecture review |

#### Step 5: Remediation

```yaml
# Fix configuration
regulus:
  ai:
    llm:
      gemini:
        location: europe-west2  # Ensure UK region
    safety:
      data-residency:
        allowed-regions:
          - europe-west2  # GCP London only
        allow-unknown-regions: false  # Block unknown
```

### Regulatory Notification

If PII was sent to non-adequate region:
- [ ] ICO breach notification within 72 hours (if applicable)
- [ ] Customer notification assessment
- [ ] Board notification

---

## RB-004: LLM Provider Failover

### Trigger

- Alert: `LlmProviderDown` or `LlmLatencyHigh`
- Provider status page indicates outage

### Procedure

#### Step 1: Verify Provider Status (1 min)

```bash
# Check health endpoint
curl -H "Authorization: Bearer $TOKEN" \
  https://api.yourbank.com/actuator/health/llm | jq

# Check latency metrics
curl https://api.yourbank.com/actuator/prometheus | \
  grep "regulus_llm_latency_seconds"
```

#### Step 2: Check Provider Status Pages

| Provider | Status Page |
|----------|-------------|
| Google Cloud | https://status.cloud.google.com |
| Azure | https://status.azure.com |
| AWS | https://health.aws.amazon.com |

#### Step 3: Initiate Failover (2 min)

**Automatic (if configured):**

Failover should happen automatically via circuit breaker. Verify:

```bash
# Check circuit breaker status
curl https://api.yourbank.com/actuator/circuitbreakers | jq
```

**Manual failover:**

```bash
# Update feature flag or config
kubectl set env deployment/regulus-agent \
  REGULUS_AI_LLM_PRIMARY_PROVIDER=azure-openai

# Or via config map
kubectl patch configmap regulus-config -p '{"data":{"primary-provider":"azure-openai"}}'

# Restart pods
kubectl rollout restart deployment/regulus-agent
```

#### Step 4: Verify Failover (2 min)

```bash
# Test agent response
curl -X POST -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"message": "test"}' \
  https://api.yourbank.com/api/agent/mortgage-adviser/chat

# Check which provider is active
curl https://api.yourbank.com/actuator/prometheus | \
  grep "regulus_llm_requests_total" | \
  grep -v "^#"
```

#### Step 5: Monitor

- Monitor error rates for 30 min
- Set reminder to check primary provider
- Plan failback when primary recovers

### Failback Procedure

```bash
# When primary provider recovers
kubectl set env deployment/regulus-agent \
  REGULUS_AI_LLM_PRIMARY_PROVIDER=gemini

kubectl rollout restart deployment/regulus-agent

# Monitor for 15 min
watch -n 10 'curl -s .../actuator/prometheus | grep regulus_llm'
```

---

## RB-005: Performance Degradation

### Trigger

- Alert: `AgentLatencyHigh` (P95 > 3s)
- Alert: `AgentErrorRateHigh` (> 5%)

### Procedure

#### Step 1: Triage (5 min)

```bash
# Check current metrics
curl https://api.yourbank.com/actuator/prometheus | grep -E "regulus_agent_(latency|errors)"

# Check which agents are affected
curl https://api.yourbank.com/actuator/prometheus | \
  grep "regulus_agent_latency_seconds{" | \
  awk -F'[{}]' '{print $2}' | sort | uniq -c | sort -rn
```

#### Step 2: Identify Root Cause

| Symptom | Likely Cause | Check |
|---------|--------------|-------|
| All agents slow | LLM provider | Provider status, latency metrics |
| Single agent slow | Agent-specific issue | Agent logs, tool latency |
| High error rate | Configuration/code | Error logs, recent deployments |
| Intermittent | Resource contention | Pod resources, DB connections |

```bash
# Check LLM latency
curl https://api.yourbank.com/actuator/prometheus | \
  grep "regulus_llm_latency_seconds"

# Check tool latency
curl https://api.yourbank.com/actuator/prometheus | \
  grep "regulus_mcp_tool_latency_seconds"

# Check pod resources
kubectl top pods -l app=regulus-agent

# Check recent deployments
kubectl rollout history deployment/regulus-agent
```

#### Step 3: Remediation

**LLM provider slow:**
- See [RB-004: LLM Provider Failover](#rb-004-llm-provider-failover)

**Resource contention:**
```bash
# Scale up
kubectl scale deployment/regulus-agent --replicas=10

# Or increase resources
kubectl set resources deployment/regulus-agent \
  --limits=memory=4Gi,cpu=2000m
```

**Tool slow:**
```bash
# Check external service health
curl -w "@curl-timing.txt" https://core-banking.internal/health

# Enable tool caching if available
kubectl set env deployment/regulus-agent \
  REGULUS_AI_MCP_TOOL_CACHE_ENABLED=true
```

**Recent deployment regression:**
```bash
# Rollback
kubectl rollout undo deployment/regulus-agent
```

---

## RB-006: Model Drift Response

### Trigger

- Alert: `ModelDriftHigh` (drift score > 0.1)
- Scheduled drift report shows degradation

### Procedure

#### Step 1: Assess Severity (10 min)

```bash
# Get drift metrics
curl -H "Authorization: Bearer $TOKEN" \
  https://api.yourbank.com/api/admin/models/mortgage-adviser | \
  jq '.performance.driftScore'

# Compare to baseline
curl -H "Authorization: Bearer $TOKEN" \
  https://api.yourbank.com/api/admin/models/mortgage-adviser/drift-history
```

**Severity Matrix:**

| Drift Score | Customer Impact | Action |
|-------------|-----------------|--------|
| 0.05 - 0.10 | Low | Monitor, schedule review |
| 0.10 - 0.20 | Medium | Alert model team, enhanced monitoring |
| > 0.20 | High | Consider kill switch, immediate review |

#### Step 2: Immediate Actions

**For drift > 0.20:**

```bash
# Consider activating kill switch for affected agent
curl -X POST -H "Authorization: Bearer $OPERATOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "scope": "AGENT",
    "target": "mortgage-adviser",
    "reason": "Model drift exceeds acceptable threshold",
    "emergency": false
  }' \
  https://api.yourbank.com/api/admin/kill-switch/activate
```

#### Step 3: Root Cause Analysis

Common drift causes:

| Cause | Investigation |
|-------|---------------|
| Input distribution shift | Compare recent vs training data distributions |
| Concept drift | Review business rule changes |
| Data quality issues | Check data pipeline health |
| External factors | Market changes, regulatory changes |

#### Step 4: Remediation

- [ ] Engage model validation team
- [ ] Prepare model retraining if needed
- [ ] Schedule validation review
- [ ] Update SS1/23 model inventory

---

## RB-007: Security Incident

### Trigger

- Alert: `SecurityEvent` (prompt injection, PII leakage, etc.)
- Security team notification
- External report

### Severity

**CRITICAL** - Follow Security Incident Response Plan

### Procedure

#### Step 1: Contain (Immediate)

```bash
# Activate kill switch immediately
curl -X POST -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "scope": "GLOBAL",
    "reason": "Security incident - containment",
    "emergency": true
  }' \
  https://api.yourbank.com/api/admin/kill-switch/activate
```

#### Step 2: Assess (15 min)

```bash
# Get security events
curl -H "Authorization: Bearer $TOKEN" \
  "https://api.yourbank.com/api/admin/audit?eventType=SECURITY&from=$(date -d '24 hours ago' -Iseconds)"

# Check for data exfiltration
curl -H "Authorization: Bearer $TOKEN" \
  "https://api.yourbank.com/api/admin/audit?eventType=PII_IN_RESPONSE"
```

#### Step 3: Escalate

| Event Type | Escalate To | Timeline |
|------------|-------------|----------|
| Prompt injection attempt | Security team | 1 hour |
| PII in response | DPO + Security | Immediate |
| Unauthorized access | CISO + Legal | Immediate |
| Data breach confirmed | Board + Regulators | Per breach policy |

#### Step 4: Preserve Evidence

```bash
# Export audit logs
curl -H "Authorization: Bearer $TOKEN" \
  "https://api.yourbank.com/api/admin/audit/export?from=...&to=..." \
  > incident-audit-$(date +%Y%m%d).json

# Snapshot affected systems
kubectl exec -it regulus-agent-pod -- tar czf /tmp/logs.tar.gz /app/logs
kubectl cp regulus-agent-pod:/tmp/logs.tar.gz ./incident-logs.tar.gz
```

#### Step 5: Remediate and Recover

- Follow Security Incident Response Plan
- Do not restore service until root cause identified
- Require security sign-off before kill switch deactivation

---

## RB-008: Audit Preparation

### Trigger

- Scheduled regulatory audit
- Internal audit request
- SS1/23 annual review

### Timeline

Start preparation 2 weeks before audit date.

### Procedure

#### Week -2: Generate Evidence

```bash
# Model inventory
curl -H "Authorization: Bearer $TOKEN" \
  https://api.yourbank.com/api/admin/models/export > model-inventory.json

# Kill switch audit log
curl -H "Authorization: Bearer $TOKEN" \
  "https://api.yourbank.com/api/admin/kill-switch/audit?from=$(date -d '1 year ago' -Iseconds)" \
  > kill-switch-audit.json

# Data residency compliance
curl -H "Authorization: Bearer $TOKEN" \
  https://api.yourbank.com/api/admin/data-residency/compliance-report \
  > data-residency-report.json

# Consumer Duty metrics
curl -H "Authorization: Bearer $TOKEN" \
  https://api.yourbank.com/api/admin/consumer-duty/report \
  > consumer-duty-report.json
```

#### Week -1: Validate Evidence

- [ ] Cross-check model inventory against SS1/23 requirements
- [ ] Verify all validation reports are current
- [ ] Confirm kill switch test evidence is available
- [ ] Review data residency violation log and resolutions
- [ ] Prepare Consumer Duty outcomes summary

#### Audit Day: Support

- Designate technical SME for auditor questions
- Have admin access ready for live demonstrations
- Prepare environment for auditor access if required

### Evidence Checklist

| Regulation | Evidence | Location |
|------------|----------|----------|
| SS1/23 | Model inventory | `/api/admin/models/export` |
| SS1/23 | Validation reports | Document management system |
| SS1/23 | Performance metrics | Prometheus/Grafana |
| PS21/3 | Kill switch test logs | `/api/admin/kill-switch/audit` |
| PS21/3 | DR test reports | Document management system |
| UK GDPR | Data residency compliance | `/api/admin/data-residency/compliance-report` |
| Consumer Duty | Suitability checks | Agent audit logs |
| Consumer Duty | Vulnerable customer handling | Escalation records |

---

## Contact List

| Role | Contact | Escalation Time |
|------|---------|-----------------|
| On-Call Engineer | PagerDuty | Immediate |
| Platform Lead | platform-lead@bank.com | 15 min |
| Security Team | security@bank.com | 5 min (incidents) |
| Risk Team | risk-team@bank.com | 30 min |
| DPO | dpo@bank.com | 1 hour (data breach) |
| CISO | ciso@bank.com | Immediate (major incident) |

---

## Related Documentation

- [Troubleshooting Guide](../guides/troubleshooting.md)
- [Security Hardening Guide](../guides/security-hardening.md)
- [Kill Switch Design](../governance/kill-switch.md)
- [Audit Evidence Templates](./audit-evidence-templates.md)
