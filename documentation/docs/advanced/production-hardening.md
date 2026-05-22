# Production Hardening

Security and performance hardening for production deployments.

!!! note "Architecture vs. operations"
    This page is the **operational** checklist — TLS, secrets, RBAC, network
    boundaries. For the **architecture** (identity contract, IdentityAdapter
    SPI, threat model, A2A signing, audit integrity, kill-switch
    authorisation), see
    [Security architecture](security-architecture.md).

## Security Hardening

### Authentication & Authorization

```yaml
# application-prod.yml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://login.microsoftonline.com/${AZURE_TENANT_ID}/v2.0

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    health:
      show-details: when_authorized
```

### RBAC Configuration

```java
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health/**").permitAll()
                .requestMatchers("/api/v1/agent/**").hasRole("USER")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .build();
    }
}
```

### Secrets Management

```yaml
# Never store secrets in configuration files
regulus:
  llm:
    gemini:
      project-id: ${GOOGLE_CLOUD_PROJECT}
      # Use Workload Identity or service account

spring:
  cloud:
    vault:
      uri: ${VAULT_ADDR}
      authentication: kubernetes
      kubernetes:
        role: regulus-agent
```

### TLS Configuration

```yaml
server:
  ssl:
    enabled: true
    protocol: TLS
    enabled-protocols: TLSv1.3
    ciphers:
      - TLS_AES_256_GCM_SHA384
      - TLS_AES_128_GCM_SHA256
    key-store: ${SSL_KEYSTORE_PATH}
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
```

### HTTP Security Headers

```java
@Configuration
public class WebSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {
        return http
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'"))
                .frameOptions(FrameOptionsConfig::deny)
                .xssProtection(XssConfig::disable)  // Rely on CSP
                .contentTypeOptions(Customizer.withDefaults())
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000))
            )
            .build();
    }
}
```

## Input Validation

### Request Validation

```java
public record ChatRequest(
    @NotBlank
    @Size(max = 10000)
    String message,

    @NotNull
    @Valid
    ChatContext context
) {}

public record ChatContext(
    @NotBlank
    @Pattern(regexp = "^[A-Z0-9-]{1,50}$")
    String customerId,

    @NotBlank
    @Pattern(regexp = "^[A-Z_]{1,30}$")
    String purposeCode,

    boolean hasConsent,

    @Pattern(regexp = "^sess-[a-z0-9]{8,32}$")
    String sessionId
) {}
```

### Prompt Injection Prevention

```yaml
regulus:
  safety:
    prompt-injection:
      enabled: true
      detection-model: rule-based  # or ml-based
      block-on-detection: true
      log-attempts: true
```

```java
@Component
public class PromptInjectionFilter {

    private final List<Pattern> injectionPatterns = List.of(
        Pattern.compile("(?i)ignore\\s+(previous|above|all)\\s+instructions"),
        Pattern.compile("(?i)system\\s*:\\s*"),
        Pattern.compile("(?i)\\[INST\\]"),
        Pattern.compile("(?i)\\{\\{.*\\}\\}")
    );

    public boolean detectInjection(String input) {
        return injectionPatterns.stream()
            .anyMatch(pattern -> pattern.matcher(input).find());
    }
}
```

## Performance Hardening

### Connection Pooling

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000

  data:
    redis:
      lettuce:
        pool:
          max-active: 16
          max-idle: 8
          min-idle: 4
          max-wait: -1ms
```

### HTTP Client Configuration

```java
@Bean
public WebClient webClient() {
    HttpClient httpClient = HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
        .responseTimeout(Duration.ofSeconds(30))
        .doOnConnected(conn -> conn
            .addHandlerLast(new ReadTimeoutHandler(30))
            .addHandlerLast(new WriteTimeoutHandler(30)));

    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .codecs(configurer -> configurer
            .defaultCodecs()
            .maxInMemorySize(16 * 1024 * 1024))  // 16MB
        .build();
}
```

### Caching

```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 3600000  # 1 hour

regulus:
  cache:
    llm-responses:
      enabled: true
      ttl: 300s
      max-size: 10000
```

```java
@Service
public class CachedLlmService {

    @Cacheable(value = "llm-responses",
               key = "#request.hashCode()",
               condition = "#request.cacheable")
    public Mono<ChatResponse> chat(ChatRequest request) {
        return llmClient.chat(request);
    }
}
```

### Rate Limiting

```yaml
resilience4j:
  ratelimiter:
    instances:
      llm-calls:
        limitForPeriod: 100
        limitRefreshPeriod: 1m
        timeoutDuration: 0s
      admin-calls:
        limitForPeriod: 10
        limitRefreshPeriod: 1m
```

### Circuit Breaker

```yaml
resilience4j:
  circuitbreaker:
    instances:
      llm-service:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
        permittedNumberOfCallsInHalfOpenState: 3
        slowCallRateThreshold: 80
        slowCallDurationThreshold: 10s
```

## Resource Limits

### JVM Configuration

```dockerfile
ENTRYPOINT ["java", \
    "-XX:+UseG1GC", \
    "-XX:MaxGCPauseMillis=200", \
    "-XX:+UseStringDeduplication", \
    "-Xmx1g", \
    "-Xms512m", \
    "-XX:MaxMetaspaceSize=256m", \
    "-jar", "app.jar"]
```

### Kubernetes Resources

```yaml
resources:
  requests:
    memory: "512Mi"
    cpu: "250m"
  limits:
    memory: "1Gi"
    cpu: "1"
```

### Request Size Limits

```yaml
spring:
  codec:
    max-in-memory-size: 10MB

server:
  max-http-header-size: 16KB
  tomcat:
    max-http-post-size: 10MB
```

## Monitoring & Alerting

### Health Checks

```yaml
management:
  endpoint:
    health:
      show-details: when_authorized
      probes:
        enabled: true
      group:
        liveness:
          include: livenessState
        readiness:
          include:
            - readinessState
            - db
            - redis
            - llm
```

### Critical Alerts

```yaml
groups:
  - name: regulus-critical
    rules:
      - alert: HighErrorRate
        expr: |
          rate(regulus_llm_errors_total[5m]) /
          rate(regulus_llm_requests_total[5m]) > 0.1
        for: 5m
        labels:
          severity: critical

      - alert: KillSwitchActivated
        expr: regulus_killswitch_status == 1
        for: 0m
        labels:
          severity: critical

      - alert: HighLatency
        expr: |
          histogram_quantile(0.99, rate(regulus_llm_latency_bucket[5m])) > 30
        for: 5m
        labels:
          severity: warning

      - alert: ServiceDown
        expr: up{job="regulus-agent"} == 0
        for: 1m
        labels:
          severity: critical
```

## Disaster Recovery

### Backup Configuration

```yaml
regulus:
  backup:
    audit-logs:
      enabled: true
      destination: s3://regulus-backups/audit/
      schedule: "0 0 * * *"  # Daily
      retention-days: 2555   # 7 years

    configuration:
      enabled: true
      destination: s3://regulus-backups/config/
      schedule: "0 * * * *"  # Hourly
```

### Failover Configuration

```yaml
regulus:
  llm:
    primary:
      provider: gemini
      location: europe-west2
    fallback:
      provider: azure-openai
      region: uksouth
    failover:
      enabled: true
      threshold: 3
      window: 60s
```

## Security Checklist

- [ ] OAuth2/OIDC authentication enabled
- [ ] RBAC configured for all endpoints
- [ ] TLS 1.3 only, strong ciphers
- [ ] Secrets in Vault/KMS, not config
- [ ] Input validation on all endpoints
- [ ] Prompt injection detection enabled
- [ ] Rate limiting configured
- [ ] Circuit breakers enabled
- [ ] Network policies applied
- [ ] Pod security context configured
- [ ] Audit logging enabled
- [ ] Alerting configured
- [ ] Backup procedures tested
- [ ] Disaster recovery plan documented

## Performance Checklist

- [ ] Connection pools sized appropriately
- [ ] HTTP client timeouts configured
- [ ] Response caching enabled where safe
- [ ] JVM garbage collection tuned
- [ ] Resource limits set
- [ ] HPA configured
- [ ] Database indexes optimized
- [ ] Query performance monitored
- [ ] Memory usage monitored
- [ ] Latency percentiles tracked
