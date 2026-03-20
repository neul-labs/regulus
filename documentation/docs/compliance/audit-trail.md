# Audit Trail

Comprehensive audit logging for regulatory compliance.

## Overview

Regulus provides extensive audit logging to support regulatory requirements including:

- SS1/23 model risk management
- UK GDPR data protection
- FCA record-keeping requirements
- Internal audit and compliance reviews

## Configuration

### Basic Configuration

```yaml title="application.yml"
regulus:
  audit:
    enabled: true
    destination: kafka
    retention-days: 2555  # 7 years
```

### Full Configuration

```yaml title="application.yml"
regulus:
  audit:
    enabled: true

    # Destinations
    destinations:
      - type: kafka
        topic: regulus.audit.events
        bootstrap-servers: ${KAFKA_BROKERS}
      - type: database
        table: audit_events
      - type: file
        path: /var/log/regulus/audit.log
        rotation: daily
        max-files: 365

    # What to log
    events:
      llm-calls: true
      policy-enforcements: true
      policy-violations: true
      pii-detections: true
      kill-switch-operations: true
      authentication: true
      authorization: true

    # Enrichment
    enrichment:
      include-request-id: true
      include-session-id: true
      include-user-id: true
      include-client-ip: false  # Privacy consideration
      include-user-agent: false

    # Retention
    retention:
      default-days: 2555  # 7 years
      pii-detection-days: 365  # 1 year
      authentication-days: 90

    # Encryption
    encryption:
      enabled: true
      key-id: ${AUDIT_ENCRYPTION_KEY_ID}
```

## Audit Event Types

### LLM Interactions

```java
public record LlmAuditEvent(
    String eventId,
    Instant timestamp,
    String eventType,  // "LLM_REQUEST", "LLM_RESPONSE"

    // Context
    String sessionId,
    String userId,
    String agentId,

    // Request details (never log actual content)
    String provider,
    String model,
    int inputTokens,
    int outputTokens,
    long latencyMs,

    // Outcome
    String status,  // "SUCCESS", "ERROR", "TIMEOUT"
    String errorCode
) {}
```

### Policy Events

```java
public record PolicyAuditEvent(
    String eventId,
    Instant timestamp,
    String eventType,  // "POLICY_ENFORCEMENT", "POLICY_VIOLATION"

    // Context
    String sessionId,
    String userId,

    // Policy details
    String policyId,
    String purposeCode,
    boolean consentProvided,

    // Outcome
    boolean allowed,
    String violationReason
) {}
```

### PII Detection Events

```java
public record PiiAuditEvent(
    String eventId,
    Instant timestamp,
    String eventType,  // "PII_DETECTED", "PII_REDACTED"

    // Context
    String sessionId,

    // Detection details (never log actual PII)
    String piiType,  // "NINO", "SORT_CODE", etc.
    int position,
    String direction  // "INPUT", "OUTPUT"
) {}
```

### Kill Switch Events

```java
public record KillSwitchAuditEvent(
    String eventId,
    Instant timestamp,
    String eventType,  // "ACTIVATION_INITIATED", "ACTIVATION_APPROVED", "DEACTIVATED"

    // Context
    String initiatorId,
    String approverId,

    // Details
    String scope,
    String reason,
    boolean dualControlSatisfied,
    boolean emergencyBypass
) {}
```

## Implementation

### Audit Logger Service

```java
@Service
public class AuditLogger {

    private final List<AuditDestination> destinations;
    private final AuditEnricher enricher;

    public void log(AuditEvent event) {
        AuditEvent enrichedEvent = enricher.enrich(event);

        destinations.forEach(destination ->
            destination.write(enrichedEvent)
                .doOnError(e -> log.error("Failed to write audit event", e))
                .subscribe()
        );
    }

    public void logLlmCall(LlmCallContext context) {
        log(LlmAuditEvent.builder()
            .eventType("LLM_REQUEST")
            .sessionId(context.sessionId())
            .userId(context.userId())
            .provider(context.provider())
            .model(context.model())
            .inputTokens(context.inputTokens())
            .outputTokens(context.outputTokens())
            .latencyMs(context.latencyMs())
            .status(context.status())
            .build());
    }

    public void logPolicyViolation(PolicyContext context, String reason) {
        log(PolicyAuditEvent.builder()
            .eventType("POLICY_VIOLATION")
            .sessionId(context.sessionId())
            .userId(context.userId())
            .policyId(context.policyId())
            .purposeCode(context.purposeCode())
            .allowed(false)
            .violationReason(reason)
            .build());
    }
}
```

### Automatic Logging with AOP

```java
@Aspect
@Component
public class AuditAspect {

    private final AuditLogger auditLogger;

    @Around("@annotation(Audited)")
    public Object auditMethod(ProceedingJoinPoint pjp) throws Throwable {
        Audited annotation = getAnnotation(pjp);
        String eventType = annotation.eventType();

        try {
            Object result = pjp.proceed();
            auditLogger.log(createSuccessEvent(pjp, eventType, result));
            return result;
        } catch (Exception e) {
            auditLogger.log(createErrorEvent(pjp, eventType, e));
            throw e;
        }
    }
}

// Usage
@Audited(eventType = "CUSTOMER_LOOKUP")
public Customer lookupCustomer(String customerId) {
    return customerService.findById(customerId);
}
```

## Kafka Audit Stream

### Producer Configuration

```java
@Configuration
public class KafkaAuditConfig {

    @Bean
    public KafkaTemplate<String, AuditEvent> auditKafkaTemplate(
            ProducerFactory<String, AuditEvent> factory) {
        return new KafkaTemplate<>(factory);
    }

    @Bean
    public ProducerFactory<String, AuditEvent> auditProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        return new DefaultKafkaProducerFactory<>(config);
    }
}
```

### Kafka Destination

```java
@Component
public class KafkaAuditDestination implements AuditDestination {

    private final KafkaTemplate<String, AuditEvent> kafkaTemplate;
    private final String topic;

    @Override
    public Mono<Void> write(AuditEvent event) {
        return Mono.fromFuture(
            kafkaTemplate.send(topic, event.sessionId(), event)
                .toCompletableFuture()
        ).then();
    }
}
```

## Database Audit Trail

### Schema

```sql
CREATE TABLE audit_events (
    id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    event_type VARCHAR(50) NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    session_id VARCHAR(100),
    user_id VARCHAR(100),
    agent_id VARCHAR(100),
    details JSONB NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_audit_events_timestamp ON audit_events(timestamp);
CREATE INDEX idx_audit_events_session ON audit_events(session_id);
CREATE INDEX idx_audit_events_user ON audit_events(user_id);
CREATE INDEX idx_audit_events_type ON audit_events(event_type);
```

### Database Destination

```java
@Component
public class DatabaseAuditDestination implements AuditDestination {

    private final R2dbcEntityTemplate template;

    @Override
    public Mono<Void> write(AuditEvent event) {
        return template.insert(AuditEventEntity.from(event)).then();
    }
}
```

## Querying Audit Logs

### REST API

```java
@RestController
@RequestMapping("/api/audit")
@PreAuthorize("hasRole('AUDIT_VIEWER')")
public class AuditController {

    private final AuditQueryService queryService;

    @GetMapping("/events")
    public Flux<AuditEvent> queryEvents(
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String eventType,
            @RequestParam @DateTimeFormat(iso = ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "100") int limit) {

        return queryService.query(AuditQuery.builder()
            .sessionId(sessionId)
            .userId(userId)
            .eventType(eventType)
            .fromTimestamp(from)
            .toTimestamp(to)
            .limit(limit)
            .build());
    }

    @GetMapping("/sessions/{sessionId}")
    public Flux<AuditEvent> getSessionAuditTrail(
            @PathVariable String sessionId) {
        return queryService.getBySessionId(sessionId);
    }
}
```

### Example Queries

```bash
# Get all events for a session
curl "http://localhost:8080/api/audit/sessions/sess-abc123"

# Query policy violations in the last 24 hours
curl "http://localhost:8080/api/audit/events?eventType=POLICY_VIOLATION&from=2024-01-14T00:00:00Z&to=2024-01-15T00:00:00Z"

# Get events for a specific user
curl "http://localhost:8080/api/audit/events?userId=john.smith&from=2024-01-01T00:00:00Z&to=2024-01-15T00:00:00Z"
```

## Retention and Archival

### Retention Policy

```java
@Scheduled(cron = "0 0 2 * * *")  // 2 AM daily
public void applyRetentionPolicy() {
    Map<String, Integer> retentionDays = Map.of(
        "LLM_REQUEST", 2555,      // 7 years
        "POLICY_VIOLATION", 2555, // 7 years
        "PII_DETECTED", 365,      // 1 year
        "AUTHENTICATION", 90      // 90 days
    );

    retentionDays.forEach((eventType, days) -> {
        Instant cutoff = Instant.now().minus(Duration.ofDays(days));
        auditRepository.archiveOlderThan(eventType, cutoff);
    });
}
```

### Archival to Cold Storage

```java
@Service
public class AuditArchivalService {

    private final S3Client s3Client;

    public void archiveToS3(String eventType, Instant cutoff) {
        Flux<AuditEvent> events = auditRepository
            .findByTypeOlderThan(eventType, cutoff);

        String key = String.format("audit/%s/%s.json.gz",
            eventType, cutoff.toString());

        events
            .buffer(10000)
            .flatMap(batch -> uploadBatch(key, batch))
            .then(auditRepository.deleteByTypeOlderThan(eventType, cutoff))
            .subscribe();
    }
}
```

## Compliance Reports

### Generating Audit Reports

```java
@Service
public class AuditReportService {

    public AuditReport generateReport(String modelId, DateRange period) {
        return AuditReport.builder()
            .modelId(modelId)
            .period(period)
            .totalInteractions(countInteractions(modelId, period))
            .policyViolations(getViolations(modelId, period))
            .piiDetections(getPiiDetections(modelId, period))
            .errorRate(calculateErrorRate(modelId, period))
            .averageLatency(calculateAverageLatency(modelId, period))
            .build();
    }
}
```

## Security

### Audit Log Integrity

```java
@Service
public class AuditIntegrityService {

    private final String signingKey;

    public AuditEvent sign(AuditEvent event) {
        String signature = calculateHmac(event, signingKey);
        return event.withSignature(signature);
    }

    public boolean verify(AuditEvent event) {
        String expectedSignature = calculateHmac(event, signingKey);
        return expectedSignature.equals(event.signature());
    }
}
```

### Access Control

```java
@PreAuthorize("hasRole('AUDIT_VIEWER')")
public Flux<AuditEvent> queryEvents(AuditQuery query) {
    // ...
}

@PreAuthorize("hasRole('AUDIT_ADMIN')")
public Mono<Void> deleteEvents(String eventType, Instant olderThan) {
    // ...
}
```

## Best Practices

1. **Never log PII** - Log detection, not values
2. **Use structured events** - Enable easy querying
3. **Sign events** - Ensure integrity
4. **Retain appropriately** - Meet regulatory requirements
5. **Monitor audit system** - Alert on failures
6. **Test regularly** - Verify logs are complete
