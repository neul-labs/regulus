# Kill Switch Procedures

Operational procedures for emergency AI agent shutdown.

## Overview

The kill switch provides immediate shutdown capability for AI agents as required by PRA PS21/3 operational resilience requirements. This document covers operational procedures for using the kill switch.

## When to Activate

### Mandatory Activation Scenarios

Activate the kill switch immediately when:

1. **Security Incident** - Suspected compromise or unauthorized access
2. **Data Breach** - PII exposure or data leakage detected
3. **Model Misbehavior** - Agent producing harmful or incorrect outputs
4. **Regulatory Request** - Directed by regulator to cease operations
5. **System Anomaly** - Unexpected behavior patterns detected

### Discretionary Activation

Consider activation when:

- Unusually high error rates observed
- Performance degradation affecting customers
- Upstream dependency failures
- Planned maintenance requiring agent shutdown

## Activation Process

### Step 1: Assess the Situation

Before activating, quickly assess:

- **Scope**: Which agents are affected?
- **Impact**: What is the customer impact?
- **Urgency**: Is immediate action required?

### Step 2: Initiate Activation (First Approver)

```bash
# Via CLI
curl -X POST https://regulus.internal/api/admin/kill-switch/activate \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "scope": "customer-support-agent",
    "reason": "Anomalous behavior detected - ticket INC123456",
    "severity": "HIGH"
  }'
```

**Response:**
```json
{
  "requestId": "ks-req-abc123",
  "status": "PENDING_APPROVAL",
  "initiator": "john.smith@bank.com",
  "scope": "customer-support-agent",
  "reason": "Anomalous behavior detected - ticket INC123456",
  "expiresAt": "2024-01-15T14:35:00Z"
}
```

### Step 3: Approve Activation (Second Approver)

A different authorized user must approve:

```bash
curl -X POST https://regulus.internal/api/admin/kill-switch/approve/ks-req-abc123 \
  -H "Authorization: Bearer $APPROVER_TOKEN"
```

**Response:**
```json
{
  "requestId": "ks-req-abc123",
  "status": "ACTIVATED",
  "activatedAt": "2024-01-15T14:32:15Z",
  "activators": ["john.smith@bank.com", "jane.doe@bank.com"],
  "scope": "customer-support-agent"
}
```

### Step 4: Verify Activation

```bash
curl https://regulus.internal/api/admin/kill-switch/status
```

**Response:**
```json
{
  "global": false,
  "scopes": {
    "customer-support-agent": {
      "active": true,
      "activatedAt": "2024-01-15T14:32:15Z",
      "reason": "Anomalous behavior detected - ticket INC123456",
      "activators": ["john.smith@bank.com", "jane.doe@bank.com"]
    }
  }
}
```

### Step 5: Notify Stakeholders

After activation, notify:

- [ ] Incident Manager
- [ ] Business Owner
- [ ] Technology Lead
- [ ] Compliance (for regulatory-triggered activations)
- [ ] Customer Service (to prepare for increased manual handling)

## Deactivation Process

### Prerequisites for Deactivation

Before deactivating:

- [ ] Root cause identified and documented
- [ ] Fix implemented and tested
- [ ] Approval from incident manager
- [ ] Business owner sign-off

### Step 1: Initiate Deactivation

```bash
curl -X POST https://regulus.internal/api/admin/kill-switch/deactivate \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "scope": "customer-support-agent",
    "reason": "Issue resolved - root cause was X, fix deployed in release Y",
    "incidentTicket": "INC123456"
  }'
```

### Step 2: Approve Deactivation

Second approver confirms:

```bash
curl -X POST https://regulus.internal/api/admin/kill-switch/approve-deactivation/ks-req-def456 \
  -H "Authorization: Bearer $APPROVER_TOKEN"
```

### Step 3: Monitor Restoration

After deactivation:

1. Monitor error rates for 30 minutes
2. Check customer feedback channels
3. Verify metrics return to normal
4. Close incident ticket

## Emergency Bypass

!!! danger "Emergency Use Only"
    The emergency bypass should only be used when dual-control approval is impossible and immediate action is required to protect customers or the firm.

### Conditions for Emergency Bypass

- Both approvers unavailable
- Imminent customer harm
- Regulatory mandate with immediate deadline

### Emergency Bypass Process

```bash
curl -X POST https://regulus.internal/api/admin/kill-switch/emergency-activate \
  -H "Authorization: Bearer $EMERGENCY_TOKEN" \
  -H "X-Emergency-Secret: $EMERGENCY_SECRET" \
  -d '{
    "scope": "global",
    "reason": "Emergency activation - both approvers unavailable",
    "justification": "Customer data exposure in progress"
  }'
```

### Post-Emergency Actions

After emergency bypass:

1. **Immediately notify** senior management
2. **Document** the circumstances requiring bypass
3. **Review** with compliance within 24 hours
4. **Update procedures** if process gaps identified

## Scope Reference

### Available Scopes

| Scope | Description | Impact |
|-------|-------------|--------|
| `global` | All AI agents | Complete AI shutdown |
| `customer-support-agent` | Customer support | Manual support only |
| `payment-agent` | Payment processing | Manual payment processing |
| `fraud-detection-agent` | Fraud detection | Rules-based fallback |

### Scope Hierarchy

```
global
├── customer-support-agent
├── payment-agent
└── fraud-detection-agent
```

Activating `global` disables all child scopes.

## Communication Templates

### Internal Notification

```
Subject: [URGENT] AI Agent Kill Switch Activated - {SCOPE}

Activation Time: {TIMESTAMP}
Scope: {SCOPE}
Reason: {REASON}
Activated By: {ACTIVATORS}

Impact:
- {IMPACT_DESCRIPTION}

Actions Required:
- {REQUIRED_ACTIONS}

Updates will be provided every 30 minutes until resolution.
```

### Customer-Facing Message

```
We're currently experiencing technical difficulties with our automated
support system. Our team is working to resolve this as quickly as possible.

In the meantime, please:
- Call us at {PHONE_NUMBER} for urgent matters
- Use our web form for non-urgent queries
- Expect longer than usual response times

We apologize for any inconvenience.
```

## Testing

### Scheduled Test Procedure

Conduct kill switch testing quarterly:

1. **Announce** test to all stakeholders
2. **Activate** kill switch on test scope
3. **Verify** fallback behavior
4. **Measure** activation time
5. **Deactivate** and restore service
6. **Document** results and gaps

### Test Checklist

- [ ] Activation completes within 60 seconds
- [ ] Fallback messages display correctly
- [ ] Audit logs capture all events
- [ ] Alerts trigger as expected
- [ ] Deactivation restores normal operation

## Metrics and SLOs

| Metric | Target | Alert Threshold |
|--------|--------|-----------------|
| Activation Time | < 60s | > 120s |
| Deactivation Time | < 60s | > 120s |
| False Activations | 0/month | > 0 |
| Test Success Rate | 100% | < 100% |

## Audit Requirements

All kill switch operations are logged:

```json
{
  "eventType": "KILL_SWITCH_ACTIVATION",
  "timestamp": "2024-01-15T14:32:15Z",
  "scope": "customer-support-agent",
  "reason": "Anomalous behavior detected",
  "initiator": "john.smith@bank.com",
  "approver": "jane.doe@bank.com",
  "dualControlSatisfied": true
}
```

Retain logs for minimum 7 years per regulatory requirements.

## Contacts

### Kill Switch Operators

| Role | Contact | Backup |
|------|---------|--------|
| Primary Operator | oncall@bank.com | +44 xxx xxx xxxx |
| Backup Operator | oncall-backup@bank.com | +44 xxx xxx xxxx |
| Incident Manager | incident-manager@bank.com | +44 xxx xxx xxxx |

### Escalation Path

1. On-call Engineer
2. Engineering Manager
3. VP Engineering
4. CTO
