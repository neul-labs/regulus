# External Systems

Integration patterns for connecting Regulus agents with enterprise systems.

## Overview

Regulus provides integration adapters for common enterprise systems in UK financial services:

- **GRC Systems** - ServiceNow, Archer
- **IAM Systems** - Azure AD, Okta
- **Secret Management** - HashiCorp Vault, Azure Key Vault
- **Configuration** - Spring Cloud Config, Consul
- **Messaging** - Kafka, RabbitMQ

## ServiceNow Integration

### Configuration

```yaml
regulus:
  integrations:
    servicenow:
      enabled: true
      instance: ${SERVICENOW_INSTANCE}
      auth:
        type: oauth2
        client-id: ${SERVICENOW_CLIENT_ID}
        client-secret: ${SERVICENOW_CLIENT_SECRET}
```

### GRC Workflows

```java
@Service
public class GrcWorkflowService {

    private final ServiceNowClient serviceNow;

    public Mono<String> createApprovalRequest(ApprovalRequest request) {
        return serviceNow.createRecord("sc_request", Map.of(
            "short_description", request.description(),
            "requested_for", request.requestedFor(),
            "category", "AI_MODEL_APPROVAL",
            "priority", request.priority()
        )).map(Record::getSysId);
    }

    public Mono<ApprovalStatus> checkApprovalStatus(String requestId) {
        return serviceNow.getRecord("sc_request", requestId)
            .map(record -> ApprovalStatus.valueOf(record.get("approval")));
    }
}
```

### Model Registry Sync

```java
@Scheduled(fixedRate = 3600000) // Hourly
public void syncModelRegistry() {
    modelRegistry.getAllModels()
        .flatMap(model -> serviceNow.upsertRecord("u_ai_model_inventory", Map.of(
            "u_model_id", model.id(),
            "u_model_name", model.name(),
            "u_risk_tier", model.riskTier(),
            "u_owner", model.owner(),
            "u_last_review", model.lastReviewDate()
        )))
        .subscribe();
}
```

## HashiCorp Vault

### Configuration

```yaml
regulus:
  integrations:
    vault:
      enabled: true
      uri: ${VAULT_ADDR}
      authentication: kubernetes  # token, kubernetes, or approle
      kubernetes:
        role: regulus-agent
        service-account-token-file: /var/run/secrets/kubernetes.io/serviceaccount/token
```

### Secret Retrieval

```java
@Service
public class SecretService {

    private final VaultTemplate vault;

    public String getLlmApiKey(String provider) {
        VaultResponse response = vault.read("secret/data/llm/" + provider);
        return response.getData().get("api-key").toString();
    }

    public DatabaseCredentials getDatabaseCredentials(String database) {
        VaultResponse response = vault.read("database/creds/" + database);
        return new DatabaseCredentials(
            response.getData().get("username").toString(),
            response.getData().get("password").toString()
        );
    }
}
```

### Dynamic Secrets

```java
@Configuration
public class DynamicSecretsConfig {

    @Bean
    public LlmClient llmClient(SecretService secretService) {
        return LlmClient.builder()
            .apiKeyProvider(() -> secretService.getLlmApiKey("openai"))
            .build();
    }
}
```

## Kafka Audit Streaming

### Configuration

```yaml
regulus:
  integrations:
    kafka:
      enabled: true
      bootstrap-servers: ${KAFKA_BROKERS}
      topics:
        audit: regulus.audit.events
        metrics: regulus.metrics
      producer:
        acks: all
        retries: 3
```

### Audit Event Publishing

```java
@Service
public class KafkaAuditPublisher implements AuditPublisher {

    private final KafkaTemplate<String, AuditEvent> kafka;

    @Override
    public void publish(AuditEvent event) {
        kafka.send("regulus.audit.events", event.sessionId(), event)
            .whenComplete((result, error) -> {
                if (error != null) {
                    log.error("Failed to publish audit event", error);
                    fallbackPublisher.publish(event);
                }
            });
    }
}
```

### Event Schema

```java
public record AuditEvent(
    String eventId,
    String eventType,
    Instant timestamp,
    String sessionId,
    String userId,
    String agentId,
    String action,
    Map<String, Object> details,
    String outcome
) {}
```

## Azure AD Integration

### Configuration

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://login.microsoftonline.com/${AZURE_TENANT_ID}/v2.0

regulus:
  integrations:
    azure-ad:
      enabled: true
      tenant-id: ${AZURE_TENANT_ID}
      client-id: ${AZURE_CLIENT_ID}
      roles-claim: roles
```

### Role-Based Access

```java
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        return http
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter()))
            )
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/api/admin/**").hasRole("ADMIN")
                .pathMatchers("/api/agent/**").hasRole("USER")
                .anyExchange().authenticated()
            )
            .build();
    }

    private Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtConverter() {
        return jwt -> {
            List<String> roles = jwt.getClaimAsStringList("roles");
            List<GrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
            return Mono.just(new JwtAuthenticationToken(jwt, authorities));
        };
    }
}
```

## Spring Cloud Config

### Configuration

```yaml
spring:
  config:
    import: "configserver:http://config-server:8888"
  cloud:
    config:
      label: main
      profile: ${SPRING_PROFILES_ACTIVE:prod}
```

### Kill Switch Backend

```java
@Service
public class ConfigServerKillSwitch implements KillSwitchBackend {

    private final ConfigClientProperties configClient;

    @Scheduled(fixedRateString = "${regulus.kill-switch.check-interval}")
    public void refresh() {
        // Trigger config refresh
        applicationContext.publishEvent(new RefreshRemoteApplicationEvent(
            this, configClient.getName(), null
        ));
    }

    @Value("${regulus.kill-switch.active:false}")
    private boolean active;

    @Override
    public boolean isActive(String scope) {
        return active;
    }
}
```

## Database Integration

### Connection Pooling

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:5432/regulus
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
```

### Audit Persistence

```java
@Repository
public class AuditRepository {

    private final R2dbcEntityTemplate template;

    public Mono<Void> save(AuditEvent event) {
        return template.insert(AuditEventEntity.class)
            .using(AuditEventEntity.from(event))
            .then();
    }

    public Flux<AuditEvent> findBySessionId(String sessionId) {
        return template.select(AuditEventEntity.class)
            .matching(Query.query(Criteria.where("session_id").is(sessionId)))
            .all()
            .map(AuditEventEntity::toEvent);
    }
}
```

## Integration Testing

### Testcontainers Setup

```java
@SpringBootTest
@Testcontainers
class IntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    );

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        DockerImageName.parse("postgres:15")
    );

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

## Best Practices

1. **Use circuit breakers** - Protect against external system failures
2. **Implement retries** - Handle transient failures gracefully
3. **Monitor connections** - Track connection pool health
4. **Secure credentials** - Use Vault or similar for secrets
5. **Log integration calls** - Maintain audit trail for debugging
6. **Test integrations** - Use Testcontainers for integration tests
