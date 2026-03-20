# Kill Switch Design

UK financial services regulations ([PRA PS21/3](https://www.bankofengland.co.uk/prudential-regulation/publication/2021/march/operational-resilience-impact-tolerances-for-important-business-services), [PRA SS1/23](https://www.bankofengland.co.uk/prudential-regulation/publication/2023/may/model-risk-management-principles-for-banks-ss)) expect rapid, controlled disablement of AI-powered services. Regulus provides a standard kill switch pattern aligned with common bank practices (e.g., Lloyds Banking Group, NatWest, Barclays).

## Control Plane

- **State Store**: Use the bank's authoritative configuration/toggle service (e.g., ConfigHub, Spring Cloud Config backed by HashiCorp Vault, or a hardened feature flag platform). Values are stored in a privileged namespace (`regulus/kill-switch/<agent|tool>`).
- **Access Control**: RBAC enforced via the bank's IAM (Azure AD/ADFS) with privileged roles (`AI-Ops-KillSwitch`, `Risk-KillSwitch`). Dual-control approvals handled through ServiceNow or the bank's change-management workflow before toggles move to active state.
- **Audit Trail**: Every change emits an immutable event to the governance/audit bus (Kafka → Splunk) capturing who, when, why, and related incident/change ticket IDs.

## Runtime Behaviour

- **Interceptors**: The starters inject a `KillSwitchInterceptor` around ADK planners and tools. When a kill flag is on, the interceptor short-circuits execution and returns a controlled failure object with remediation guidance.
- **Scope Levels**:
  - *Global*: disable all AI agent processing across the platform.
  - *Agent*: disable a specific agent.
  - *Connector*: disable individual MCP/A2A endpoints or external LLM providers.
  - *Tool*: disable a single tool invocation (e.g., payments initiation) while leaving other tools active.
- **Telemetry**: Kill events annotate OTEL traces and Micrometer metrics (`regulus.killSwitch.enabled=1`); alerts fire via the bank's monitoring pipeline (e.g., Prometheus → PagerDuty).

---

## Dual-Control (4-Eyes Principle)

UK financial services governance requires critical operations to have approval from two independent parties. The `DualControlKillSwitch` implements this pattern.

### How It Works

```
┌─────────────────────────────────────────────────────────────────┐
│                    DUAL-CONTROL WORKFLOW                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. Request        2. First Approval    3. Second Approval      │
│  ─────────────     ────────────────     ─────────────────────   │
│                                                                 │
│  Operator A        Operator A           Operator B              │
│  requests kill     (auto-approved       approves                │
│  switch            as requester)        ───────┐                │
│       │                  │                     │                │
│       ▼                  ▼                     ▼                │
│  ┌─────────┐       ┌──────────┐         ┌──────────┐           │
│  │ PENDING │ ───▶  │ 1/2      │  ───▶   │ EXECUTED │           │
│  │ REQUEST │       │ APPROVED │         │          │           │
│  └─────────┘       └──────────┘         └──────────┘           │
│                                                                 │
│  Request expires after 4 hours if not fully approved            │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Configuration

```yaml
regulus:
  ai:
    safety:
      kill-switch:
        enabled: true
        provider: vault  # in-memory | config-hub | vault
        dual-control:
          enabled: true
          required-approvers: 2
          allow-emergency-bypass: true   # Allow immediate activation in emergencies
          allow-self-approval: false     # Requester cannot be sole approver
          authorized-approvers:
            - risk-team@bank.com
            - ai-ops@bank.com
            - model-risk@bank.com
            - security-ops@bank.com
```

### Java API

```java
@Autowired
private DualControlKillSwitch dualControlKillSwitch;

// Request activation (returns request ID for tracking)
String requestId = dualControlKillSwitch.requestGlobalActivation(
    "Model drift detected - exceeds threshold",  // reason
    "operator-a@bank.com",                       // requestedBy
    false                                        // emergency bypass
);
// Returns: "KS-A1B2C3D4"

// Second approver approves the request
boolean executed = dualControlKillSwitch.approve(
    requestId,
    "operator-b@bank.com"  // Different person
);
// Returns: true (kill switch now active)

// For scoped activation (specific agent/tool)
String scopedRequestId = dualControlKillSwitch.requestScopedActivation(
    KillSwitchState.Scope.AGENT,
    "mortgage-adviser-agent",
    "Compliance issue identified",
    "risk-team@bank.com",
    false
);

// Emergency bypass (immediate activation, no second approval needed)
dualControlKillSwitch.requestGlobalActivation(
    "CRITICAL: Data breach detected",
    "security-ops@bank.com",
    true  // EMERGENCY - bypasses dual control
);
```

### Pending Request Management

```java
// Get all pending requests awaiting approval
List<PendingRequest> pending = dualControlKillSwitch.getPendingRequests();

for (PendingRequest request : pending) {
    System.out.println("Request: " + request.getRequestId());
    System.out.println("  Reason: " + request.getReason());
    System.out.println("  Requested by: " + request.getRequestedBy());
    System.out.println("  Approvals: " + request.getApprovals().size() + "/" + request.getRequiredApprovals());
    System.out.println("  Remaining: " + request.remainingApprovals());
}

// Reject a request
dualControlKillSwitch.reject(
    requestId,
    "compliance-officer@bank.com",
    "False positive - drift within acceptable range"
);
```

### Audit Trail

Every dual-control action is recorded for compliance:

```java
// Get complete audit log
List<DualControlAuditEntry> auditLog = dualControlKillSwitch.getAuditLog();

// Get audit trail for specific request
List<DualControlAuditEntry> requestAudit =
    dualControlKillSwitch.getAuditLogForRequest(requestId);

// Audit entry contains:
// - auditId: Unique audit record ID
// - requestId: Kill switch request ID
// - action: REQUEST_ACTIVATION | ACTIVATE | DEACTIVATE | EMERGENCY_ACTIVATE | REJECTED | EXPIRED
// - scope: GLOBAL | AGENT | CONNECTOR | TOOL
// - targetId: Specific target (for scoped operations)
// - reason: Human-readable reason
// - requestedBy: User who initiated
// - approvers: Comma-separated list of approvers
// - executed: Whether the action was executed
// - timestamp: When the action occurred
```

### Action Types

| Action | Description | Requires Dual Control |
|--------|-------------|----------------------|
| `REQUEST_ACTIVATION` | Initial activation request created | N/A (starts workflow) |
| `ACTIVATE` | Kill switch activated after approvals | Yes (unless emergency) |
| `DEACTIVATE` | Kill switch deactivated | Yes |
| `EMERGENCY_ACTIVATE` | Immediate activation bypassing dual control | No (emergency only) |
| `REJECTED` | Request rejected by approver | No |
| `EXPIRED` | Request expired (default: 4 hours) | N/A |
| `UNAUTHORIZED_APPROVAL` | Approval attempted by unauthorised user | N/A (logged only) |

---

## Operational Workflow

### Standard Activation (Dual-Control)

1. Incident/Risk detects an issue; opens a ServiceNow change/incident.
2. Authorised operator requests kill via API/CLI, referencing the ticket.
3. Request enters pending state; first approval auto-recorded for requester.
4. Second authorised approver reviews and approves.
5. Kill switch activates; config change propagates to services.
6. Observability dashboards show kill state; consumers receive controlled error responses.
7. Recovery follows the same dual-control workflow.

### Emergency Activation (Bypass)

1. Critical incident detected (e.g., data breach, security compromise).
2. Authorised operator activates emergency bypass.
3. Kill switch activates **immediately** without second approval.
4. Emergency activation is logged with full audit trail.
5. Post-incident review validates emergency use was appropriate.

```java
// Emergency activation - use only for critical incidents
dualControlKillSwitch.requestGlobalActivation(
    "EMERGENCY: Active data exfiltration detected - INC0012345",
    "security-ops@bank.com",
    true  // Emergency bypass
);
```

---

## Compliance Alignment

| Regulation | Requirement | Regulus Control |
|------------|-------------|-----------------|
| [PRA PS21/3](https://www.bankofengland.co.uk/prudential-regulation/publication/2021/march/operational-resilience-impact-tolerances-for-important-business-services) | Ability to prevent harm and maintain continuity | Kill switch with multiple scope levels |
| [PRA SS1/23](https://www.bankofengland.co.uk/prudential-regulation/publication/2023/may/model-risk-management-principles-for-banks-ss) | Model risk controls with change rationale | Full audit trail with reason tracking |
| [FCA SYSC 13.9](https://www.handbook.fca.org.uk/handbook/SYSC/13/9.html) | Outsourcing operational controls | Dual-control prevents single point of failure |
| ICO/UK GDPR | Audit trail for investigations | Immutable audit log with timestamps |
| Consumer Duty | Prevent customer harm | Emergency bypass for critical issues |

---

## Implementation Checklist

Use this checklist when implementing the kill switch for your Regulus deployment:

### Infrastructure Setup
- [ ] Configure state store provider (Vault, ConfigHub, or in-memory for dev)
- [ ] Define toggle hierarchy (global, agent, connector, tool scopes)
- [ ] Set up RBAC roles (`AI-Ops-KillSwitch`, `Risk-KillSwitch`)
- [ ] Configure authorized approvers list

### Dual-Control Configuration
- [ ] Enable dual-control for production environments
- [ ] Set required approvers count (minimum 2 for UK FS)
- [ ] Configure emergency bypass policy (who can use, when)
- [ ] Disable self-approval for production
- [ ] Set request expiry timeout (default: 4 hours)

### Integration
- [ ] Wire `KillSwitchInterceptor` to ADK planners and tools
- [ ] Implement controlled response payloads with remediation guidance
- [ ] Configure Kafka audit event publishing
- [ ] Set up alerting triggers (Prometheus/Splunk)

### Operations
- [ ] Document runbooks (activation, approval, recovery workflows)
- [ ] Train operations staff on dual-control procedures
- [ ] Establish emergency bypass protocols and authorization
- [ ] Schedule quarterly resilience drills

### Compliance
- [ ] Test kill scenarios during chaos exercises
- [ ] Record evidence for PS21/3 compliance
- [ ] Verify audit trail captures all required fields
- [ ] Confirm ServiceNow/GRC integration for change tickets

---

## Related Documentation

- [Safety Starter Configuration](../guides/starters.md#regulus-ai-safety-starter)
- [Governance & Security](./governance-security.md)
- [Risk Control Matrix](./risk-control-matrix.md)
- [Implementation Playbooks](../references/implementation-playbooks.md)

