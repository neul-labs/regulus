# Kill Switch

Emergency shutdown control for AI agents with dual-control (4-eyes principle) enforcement.

## Overview

The kill switch provides immediate shutdown capability for AI agents when:

- Anomalous behavior is detected
- Security incident occurs
- Regulatory requirement demands cessation
- Model produces harmful outputs

## Key Features

- **Dual-control approval** - Requires two authorized users (4-eyes principle)
- **Hierarchical scopes** - Global, service, or agent-level control
- **Audit trail** - All activations logged for regulatory evidence
- **Graceful degradation** - Configurable fallback behavior

## Configuration

### Basic Configuration

```yaml title="application.yml"
regulus:
  kill-switch:
    enabled: true
    check-interval: 30s
    backend: config  # config, redis, or consul
```

### Full Configuration

```yaml title="application.yml"
regulus:
  kill-switch:
    enabled: true
    check-interval: 30s

    # Backend for state storage
    backend: redis
    redis:
      host: localhost
      port: 6379
      key-prefix: "regulus:killswitch:"

    # Dual-control settings
    dual-control:
      enabled: true
      required-approvers: 2
      approval-timeout: 5m
      allowed-roles:
        - KILL_SWITCH_OPERATOR
        - PLATFORM_ADMIN

    # Scopes
    scopes:
      - id: global
        description: "All AI agents"
      - id: customer-support
        description: "Customer support agents"
      - id: payment-agent
        description: "Payment processing agent"

    # Fallback behavior
    fallback:
      enabled: true
      message: "Service temporarily unavailable. Please try again later."
      redirect-url: null

    # Audit
    audit:
      enabled: true
      include-activator: true
      include-reason: true
```

## Using the Kill Switch

### Checking Kill Switch Status

```java
@Service
public class AgentService {

    private final KillSwitch killSwitch;
    private final LlmClient llmClient;

    public Mono<String> process(String input) {
        // Check kill switch before processing
        if (killSwitch.isActive()) {
            return Mono.error(new ServiceUnavailableException(
                "Service temporarily unavailable"
            ));
        }

        return llmClient.chat(input)
            .map(ChatResponse::content);
    }
}
```

### With Scoped Checking

```java
public Mono<String> processWithScope(String input, String agentId) {
    // Check specific scope
    if (killSwitch.isActive(agentId)) {
        return Mono.error(new ServiceUnavailableException(
            "Agent " + agentId + " is currently unavailable"
        ));
    }

    // Also check global scope
    if (killSwitch.isActive("global")) {
        return Mono.error(new ServiceUnavailableException(
            "All AI services are currently unavailable"
        ));
    }

    return llmClient.chat(input);
}
```

### Reactive Checking

```java
public Mono<String> processReactive(String input) {
    return killSwitch.check()
        .flatMap(active -> {
            if (active) {
                return Mono.error(new ServiceUnavailableException(
                    killSwitch.getFallbackMessage()
                ));
            }
            return llmClient.chat(input);
        });
}
```

## Dual-Control Activation

### Initiating Activation

```java
@RestController
@RequestMapping("/api/admin/kill-switch")
public class KillSwitchController {

    private final KillSwitch killSwitch;

    @PostMapping("/activate")
    @PreAuthorize("hasRole('KILL_SWITCH_OPERATOR')")
    public Mono<ActivationRequest> initiateActivation(
            @RequestBody ActivationRequest request,
            @AuthenticationPrincipal User user) {

        return killSwitch.initiateActivation(
            request.scope(),
            request.reason(),
            user.getId()
        );
    }
}
```

### Approving Activation

```java
@PostMapping("/approve/{requestId}")
@PreAuthorize("hasRole('KILL_SWITCH_OPERATOR')")
public Mono<ActivationResult> approveActivation(
        @PathVariable String requestId,
        @AuthenticationPrincipal User user) {

    return killSwitch.approveActivation(requestId, user.getId())
        .map(result -> {
            if (result.isActivated()) {
                auditLogger.logKillSwitchActivation(
                    result.scope(),
                    result.reason(),
                    result.activators()
                );
            }
            return result;
        });
}
```

### Deactivating

```java
@PostMapping("/deactivate")
@PreAuthorize("hasRole('KILL_SWITCH_OPERATOR')")
public Mono<Void> deactivate(
        @RequestBody DeactivationRequest request,
        @AuthenticationPrincipal User user) {

    // Deactivation also requires dual control
    return killSwitch.initiateDeactivation(
        request.scope(),
        request.reason(),
        user.getId()
    );
}
```

## Backend Configuration

### Redis Backend

```yaml
regulus:
  kill-switch:
    backend: redis
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      ssl: true
      key-prefix: "regulus:killswitch:"
```

### Consul Backend

```yaml
regulus:
  kill-switch:
    backend: consul
    consul:
      host: ${CONSUL_HOST:localhost}
      port: ${CONSUL_PORT:8500}
      token: ${CONSUL_TOKEN:}
      key-prefix: "regulus/killswitch/"
```

### Config Server Backend

```yaml
regulus:
  kill-switch:
    backend: config
    config:
      refresh-interval: 10s
```

## Monitoring

### Health Check

```java
@Component
public class KillSwitchHealthIndicator implements HealthIndicator {

    private final KillSwitch killSwitch;

    @Override
    public Health health() {
        if (killSwitch.isActive()) {
            return Health.down()
                .withDetail("reason", killSwitch.getActivationReason())
                .withDetail("activatedAt", killSwitch.getActivatedAt())
                .build();
        }
        return Health.up().build();
    }
}
```

### Metrics

Available metrics:

- `regulus.killswitch.status` - Current status (0=inactive, 1=active)
- `regulus.killswitch.activations.total` - Total activations
- `regulus.killswitch.check.latency` - Check latency

```promql
# Alert on kill switch activation
regulus_killswitch_status == 1

# Activation history
increase(regulus_killswitch_activations_total[24h])
```

### Alerting

```yaml
# Prometheus alert rule
groups:
  - name: regulus-killswitch
    rules:
      - alert: KillSwitchActivated
        expr: regulus_killswitch_status == 1
        for: 0m
        labels:
          severity: critical
        annotations:
          summary: "Kill switch activated"
          description: "AI agent kill switch has been activated"
```

## Audit Trail

All kill switch operations are logged:

```java
@EventListener
public void onKillSwitchEvent(KillSwitchEvent event) {
    auditLogger.log(AuditEvent.builder()
        .type(event.getType()) // ACTIVATION, DEACTIVATION, CHECK
        .timestamp(Instant.now())
        .details(Map.of(
            "scope", event.getScope(),
            "reason", event.getReason(),
            "initiator", event.getInitiator(),
            "approver", event.getApprover()
        ))
        .build());
}
```

## Emergency Bypass

For extreme emergencies, a bypass mechanism exists:

```yaml
regulus:
  kill-switch:
    emergency-bypass:
      enabled: true
      secret: ${EMERGENCY_BYPASS_SECRET}  # Stored securely
      audit-always: true
```

!!! danger "Emergency Bypass"
    The emergency bypass should only be used when the dual-control process cannot be completed and immediate action is required. All bypass uses are logged and should be reviewed.

## Testing

```java
@SpringBootTest
class KillSwitchTest {

    @Autowired
    private KillSwitch killSwitch;

    @Test
    void shouldBlockWhenActive() {
        killSwitch.activate("test-scope", "Test activation");

        assertThat(killSwitch.isActive("test-scope")).isTrue();

        killSwitch.deactivate("test-scope");
    }

    @Test
    void shouldRequireDualControl() {
        // First approver
        ActivationRequest request = killSwitch
            .initiateActivation("scope", "reason", "user1");

        assertThat(killSwitch.isActive("scope")).isFalse();

        // Second approver
        killSwitch.approveActivation(request.id(), "user2");

        assertThat(killSwitch.isActive("scope")).isTrue();
    }
}
```

## Best Practices

1. **Test regularly** - Include kill switch activation in disaster recovery drills
2. **Define clear scopes** - Granular scopes allow targeted intervention
3. **Document procedures** - Ensure operators know when and how to use
4. **Monitor check latency** - Kill switch checks should be fast (<10ms)
5. **Secure access** - Limit kill switch access to authorized personnel
6. **Review audit logs** - Regular review of kill switch usage
