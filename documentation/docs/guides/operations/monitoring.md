# Monitoring

Observability setup for Regulus agents including metrics, logging, and tracing.

## Overview

Regulus provides comprehensive observability through:

- **Metrics** - Micrometer with Prometheus export
- **Logging** - Structured JSON logging
- **Tracing** - OpenTelemetry distributed tracing
- **Audit** - Compliance-focused audit events

## Metrics

### Configuration

```yaml title="application.yml"
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    tags:
      application: regulus-agent
      environment: ${ENVIRONMENT:dev}
    distribution:
      percentiles-histogram:
        http.server.requests: true
        regulus.llm.latency: true
```

### Available Metrics

#### LLM Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `regulus.llm.requests.total` | Counter | Total LLM requests |
| `regulus.llm.latency` | Timer | Request latency |
| `regulus.llm.tokens.input` | Counter | Input tokens consumed |
| `regulus.llm.tokens.output` | Counter | Output tokens generated |
| `regulus.llm.errors.total` | Counter | Errors by type |

#### Policy Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `regulus.policy.enforcements.total` | Counter | Policy enforcement attempts |
| `regulus.policy.violations.total` | Counter | Policy violations |
| `regulus.policy.latency` | Timer | Enforcement latency |

#### Privacy Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `regulus.privacy.redactions.total` | Counter | PII redactions by type |
| `regulus.privacy.detections.total` | Counter | PII detections |

#### Kill Switch Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `regulus.killswitch.status` | Gauge | Current status (0/1) |
| `regulus.killswitch.activations.total` | Counter | Activations |
| `regulus.killswitch.check.latency` | Timer | Check latency |

### Custom Metrics

```java
@Component
public class BusinessMetrics {

    private final MeterRegistry registry;
    private final Counter requestCounter;
    private final Timer processingTimer;

    public BusinessMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.requestCounter = Counter.builder("regulus.business.requests")
            .description("Business requests processed")
            .tag("type", "agent")
            .register(registry);
        this.processingTimer = Timer.builder("regulus.business.processing")
            .description("Request processing time")
            .register(registry);
    }

    public void recordRequest(String outcome) {
        requestCounter.increment();
        registry.counter("regulus.business.outcomes", "outcome", outcome)
            .increment();
    }

    public Timer.Sample startTimer() {
        return Timer.start(registry);
    }

    public void stopTimer(Timer.Sample sample) {
        sample.stop(processingTimer);
    }
}
```

### Prometheus Configuration

```yaml title="prometheus.yml"
scrape_configs:
  - job_name: 'regulus-agents'
    kubernetes_sd_configs:
      - role: pod
    relabel_configs:
      - source_labels: [__meta_kubernetes_pod_label_app]
        regex: regulus-agent
        action: keep
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
        regex: true
        action: keep
    metrics_path: /actuator/prometheus
```

### Grafana Dashboard

```json
{
  "title": "Regulus Agent Dashboard",
  "panels": [
    {
      "title": "LLM Request Rate",
      "type": "graph",
      "targets": [
        {
          "expr": "rate(regulus_llm_requests_total[5m])",
          "legendFormat": "{{provider}}"
        }
      ]
    },
    {
      "title": "LLM Latency P99",
      "type": "graph",
      "targets": [
        {
          "expr": "histogram_quantile(0.99, rate(regulus_llm_latency_bucket[5m]))",
          "legendFormat": "p99"
        }
      ]
    },
    {
      "title": "Policy Violations",
      "type": "stat",
      "targets": [
        {
          "expr": "increase(regulus_policy_violations_total[24h])"
        }
      ]
    },
    {
      "title": "Kill Switch Status",
      "type": "stat",
      "targets": [
        {
          "expr": "regulus_killswitch_status"
        }
      ]
    }
  ]
}
```

## Logging

### Configuration

```yaml title="application.yml"
logging:
  level:
    root: INFO
    com.regulus: DEBUG
  pattern:
    console: "%d{ISO8601} %highlight(%-5level) [%thread] %cyan(%logger{36}) - %msg%n"
```

### JSON Logging for Production

```xml title="logback-spring.xml"
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <springProfile name="prod">
        <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="net.logstash.logback.encoder.LogstashEncoder">
                <includeMdcKeyName>sessionId</includeMdcKeyName>
                <includeMdcKeyName>userId</includeMdcKeyName>
                <includeMdcKeyName>traceId</includeMdcKeyName>
                <customFields>{"service":"regulus-agent"}</customFields>
            </encoder>
        </appender>
        <root level="INFO">
            <appender-ref ref="JSON"/>
        </root>
    </springProfile>

    <springProfile name="!prod">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>
        <root level="DEBUG">
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>
</configuration>
```

### MDC Context

```java
@Component
public class LoggingContextFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String sessionId = exchange.getRequest().getHeaders()
            .getFirst("X-Session-ID");
        String userId = extractUserId(exchange);

        return chain.filter(exchange)
            .contextWrite(ctx -> ctx
                .put("sessionId", sessionId)
                .put("userId", userId));
    }
}
```

### Structured Logging

```java
@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    public Mono<String> process(AgentRequest request) {
        return Mono.deferContextual(ctx -> {
            MDC.put("sessionId", ctx.getOrDefault("sessionId", "unknown"));
            MDC.put("userId", ctx.getOrDefault("userId", "unknown"));

            log.info("Processing request", kv("action", "process_start"));

            return doProcess(request)
                .doOnSuccess(result ->
                    log.info("Request completed",
                        kv("action", "process_complete"),
                        kv("resultLength", result.length())))
                .doOnError(error ->
                    log.error("Request failed",
                        kv("action", "process_error"),
                        kv("error", error.getMessage())));
        });
    }
}
```

## Distributed Tracing

### OpenTelemetry Configuration

```yaml title="application.yml"
management:
  tracing:
    sampling:
      probability: 0.1  # 10% sampling in production

otel:
  exporter:
    otlp:
      endpoint: http://otel-collector:4317
  resource:
    attributes:
      service.name: regulus-agent
      service.version: ${app.version}
      deployment.environment: ${ENVIRONMENT}
```

### Dependencies

```kotlin title="build.gradle.kts"
dependencies {
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
}
```

### Custom Spans

```java
@Service
public class AgentService {

    private final Tracer tracer;

    public Mono<String> process(AgentRequest request) {
        return Mono.defer(() -> {
            Span span = tracer.spanBuilder("agent.process")
                .setAttribute("user.id", request.userId())
                .setAttribute("request.type", request.type())
                .startSpan();

            return doProcess(request)
                .doOnSuccess(result -> {
                    span.setAttribute("response.length", result.length());
                    span.setStatus(StatusCode.OK);
                })
                .doOnError(error -> {
                    span.setStatus(StatusCode.ERROR, error.getMessage());
                    span.recordException(error);
                })
                .doFinally(signal -> span.end());
        });
    }
}
```

### Trace Propagation

```java
@Component
public class TracePropagationFilter implements WebFilter {

    private final TextMapPropagator propagator;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        Context context = propagator.extract(
            Context.current(),
            exchange.getRequest().getHeaders(),
            new HeaderGetter()
        );

        return chain.filter(exchange)
            .contextWrite(ctx -> ctx.put(Context.class, context));
    }
}
```

## Alerting

### Prometheus Alerting Rules

```yaml title="alerts.yml"
groups:
  - name: regulus-alerts
    rules:
      - alert: HighErrorRate
        expr: |
          rate(regulus_llm_errors_total[5m]) /
          rate(regulus_llm_requests_total[5m]) > 0.05
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High LLM error rate"
          description: "Error rate is {{ $value | humanizePercentage }}"

      - alert: KillSwitchActivated
        expr: regulus_killswitch_status == 1
        for: 0m
        labels:
          severity: critical
        annotations:
          summary: "Kill switch activated"

      - alert: HighLatency
        expr: |
          histogram_quantile(0.99, rate(regulus_llm_latency_bucket[5m])) > 10
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High LLM latency"
          description: "P99 latency is {{ $value }}s"

      - alert: PolicyViolationSpike
        expr: |
          increase(regulus_policy_violations_total[1h]) > 100
        labels:
          severity: warning
        annotations:
          summary: "Policy violation spike"
```

## Audit Events

### Kafka Audit Stream

```java
@Service
public class AuditService {

    private final KafkaTemplate<String, AuditEvent> kafka;

    public void logLlmCall(LlmCallContext context) {
        AuditEvent event = AuditEvent.builder()
            .type("LLM_CALL")
            .timestamp(Instant.now())
            .sessionId(context.sessionId())
            .userId(context.userId())
            .details(Map.of(
                "provider", context.provider(),
                "model", context.model(),
                "inputTokens", context.inputTokens(),
                "outputTokens", context.outputTokens(),
                "latencyMs", context.latencyMs()
            ))
            .build();

        kafka.send("regulus.audit.events", event.sessionId(), event);
    }
}
```

### Audit Event Schema

```java
public record AuditEvent(
    String eventId,
    String type,
    Instant timestamp,
    String sessionId,
    String userId,
    String agentId,
    Map<String, Object> details,
    String outcome
) {
    public static AuditEventBuilder builder() {
        return new AuditEventBuilder();
    }
}
```

## Best Practices

1. **Use structured logging** - JSON format for easy parsing
2. **Include correlation IDs** - Trace requests across services
3. **Sample appropriately** - Balance detail vs cost
4. **Alert on SLOs** - Focus on user-impacting metrics
5. **Retain audit logs** - Meet regulatory retention requirements
6. **Dashboard per persona** - Different views for ops vs compliance
