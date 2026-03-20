# ADR-001: Dual-Control Kill Switch

## Status
Accepted

## Date
2025-01-15

## Context

UK financial services firms operating AI agents face regulatory requirements for operational resilience and the ability to rapidly halt AI systems when issues arise:

1. **PRA PS21/3** requires firms to demonstrate they can remain within impact tolerances during disruptions
2. **FCA Consumer Duty** requires avoiding foreseeable harm to customers
3. **SS1/23** mandates controls over model lifecycle including the ability to decommission models rapidly
4. **DORA** (effective Jan 2025) requires ICT risk management including incident response

A single-operator kill switch poses risks:
- **Accidental activation**: Fat-finger errors could halt production systems unnecessarily
- **Malicious activation**: A compromised or rogue operator could weaponize the kill switch
- **Insufficient oversight**: Critical decisions should involve multiple parties (4-eyes principle)
- **Audit compliance**: Regulators expect evidence of controlled decision-making

The platform needs a kill switch mechanism that balances rapid response capability with appropriate controls.

## Decision

We will implement a **dual-control (4-eyes) kill switch** with the following characteristics:

### Core Design

1. **Two-approver requirement**: Both activation and deactivation require approval from two separate authorized operators
2. **Configurable scope**: Kill switch operates at global, agent-type, or individual agent levels
3. **Emergency override**: Single-operator emergency activation with mandatory post-incident review
4. **Time-bounded requests**: Pending requests expire after configurable period (default: 4 hours)
5. **Full audit trail**: Every action logged with operator identity, timestamp, and reason

### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    DualControlKillSwitch                    │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐    ┌──────────────────┐               │
│  │ Request Queue   │───▶│ Approval Engine  │               │
│  │ (Time-bounded)  │    │ (4-eyes check)   │               │
│  └─────────────────┘    └────────┬─────────┘               │
│                                  │                          │
│                                  ▼                          │
│                    ┌──────────────────────┐                │
│                    │   KillSwitchManager  │                │
│                    │   (Execution Layer)  │                │
│                    └──────────┬───────────┘                │
│                               │                             │
│              ┌────────────────┼────────────────┐           │
│              ▼                ▼                ▼           │
│      ┌───────────┐    ┌───────────┐    ┌───────────┐      │
│      │  Global   │    │Agent-Type │    │  Agent    │      │
│      │  Scope    │    │  Scope    │    │  Scope    │      │
│      └───────────┘    └───────────┘    └───────────┘      │
└─────────────────────────────────────────────────────────────┘
```

### API Design

```java
public interface DualControlKillSwitch {
    // Request operations (first approver)
    String requestGlobalActivation(String reason, String requestedBy, boolean emergency);
    String requestAgentTypeActivation(String agentType, String reason, String requestedBy);
    String requestAgentActivation(String agentId, String reason, String requestedBy);

    // Approval operations (second approver)
    boolean approve(String requestId, String approvedBy);
    boolean reject(String requestId, String rejectedBy, String reason);

    // Deactivation (also requires dual control)
    String requestDeactivation(Scope scope, String target, String requestedBy);

    // Query operations
    List<PendingRequest> getPendingRequests();
    List<AuditEntry> getAuditLog(Instant from, Instant to);
}
```

### Emergency Override

For genuine emergencies where the 4-hour approval window is unacceptable:

```java
// Emergency activation - immediate effect, single operator
killSwitch.requestGlobalActivation(reason, operator, true); // emergency=true

// Results in:
// 1. Immediate activation
// 2. Automatic incident ticket creation
// 3. Mandatory 24-hour review requirement
// 4. Board notification if >1 hour duration
```

## Consequences

### Positive

1. **Regulatory alignment**: Demonstrates 4-eyes principle required by UK regulators
2. **Reduced risk**: Prevents accidental or malicious single-operator actions
3. **Audit evidence**: Complete trail for regulatory examination
4. **Flexible scope**: Can halt specific agents without global disruption
5. **Emergency capability**: Retains rapid response for genuine emergencies

### Negative

1. **Response latency**: Normal (non-emergency) activation requires second approver
2. **Operational overhead**: Requires 24/7 availability of multiple authorized operators
3. **Complexity**: More complex than simple on/off switch
4. **Coordination requirement**: Teams must establish approval workflows

### Mitigations

| Concern | Mitigation |
|---------|------------|
| Approval latency | Emergency override for genuine emergencies |
| Operator availability | Configure sufficient authorized approvers across time zones |
| Complexity | Provide clear API and dashboard UI |
| Workflow coordination | Document runbook procedures |

## Alternatives Considered

### 1. Single-Operator Kill Switch

**Pros**: Faster response, simpler implementation
**Cons**: No protection against accidental/malicious activation, fails regulatory expectations
**Decision**: Rejected - insufficient control for regulated environment

### 2. Automated Kill Switch Only

**Pros**: Fastest possible response, no human latency
**Cons**: False positives could halt production unnecessarily, no human judgment
**Decision**: Rejected for primary control - retained as circuit breaker for technical failures

### 3. Committee Approval (3+ approvers)

**Pros**: Maximum control and oversight
**Cons**: Excessive latency even for planned deactivation
**Decision**: Rejected - 2 approvers provides sufficient control without excessive overhead

### 4. Time-Delayed Activation

**Pros**: Allows review period before activation
**Cons**: Defeats purpose of kill switch for rapid response
**Decision**: Rejected - inappropriate for emergency control mechanism

## References

- [PRA PS21/3 - Operational Resilience](https://www.bankofengland.co.uk/prudential-regulation/publication/2021/march/operational-resilience-impact-tolerances-for-important-business-services)
- [FCA Consumer Duty - Avoiding Foreseeable Harm](https://www.fca.org.uk/firms/consumer-duty)
- [Kill Switch Design Document](../governance/kill-switch.md)
- [Risk Control Matrix](../governance/risk-control-matrix.md)
