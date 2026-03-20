# Security Hardening Guide

Comprehensive security hardening guide for production deployment of Regulus-based AI agents in UK financial services environments.

---

## Security Checklist

### Pre-Deployment

- [ ] All secrets in secure vault (not in code/config files)
- [ ] TLS 1.3 enabled for all endpoints
- [ ] Authentication configured for all APIs
- [ ] Rate limiting enabled
- [ ] Kill switch tested and operational
- [ ] Data residency enforcement verified
- [ ] Audit logging to secure, immutable store
- [ ] Network segmentation in place
- [ ] Penetration testing completed
- [ ] Security review sign-off obtained

---

## Authentication & Authorization

### API Authentication

#### JWT Token Validation

```yaml
regulus:
  ai:
    security:
      authentication:
        type: JWT
        jwt:
          issuer: https://auth.yourbank.com
          audience: regulus-agents
          jwks-uri: https://auth.yourbank.com/.well-known/jwks.json
          clock-skew-seconds: 30
```

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/agent/**").hasRole("AGENT_USER")
                .requestMatchers("/mcp/**").hasRole("MCP_CLIENT")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )
            .build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter converter = new JwtGrantedAuthoritiesConverter();
        converter.setAuthoritiesClaimName("roles");
        converter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(converter);
        return jwtConverter;
    }
}
```

#### mTLS for Service-to-Service

```yaml
server:
  ssl:
    enabled: true
    protocol: TLS
    enabled-protocols: TLSv1.3
    client-auth: need  # Require client certificates
    key-store: classpath:keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    trust-store: classpath:truststore.p12
    trust-store-password: ${TRUSTSTORE_PASSWORD}
```

### Authorization Controls

#### Role-Based Access Control (RBAC)

```java
public enum RegulusRole {
    // Operational roles
    AGENT_USER,        // Can invoke agents
    AGENT_ADMIN,       // Can configure agents
    MCP_CLIENT,        // Can call MCP endpoints

    // Safety roles
    KILL_SWITCH_OPERATOR,   // Can request kill switch actions
    KILL_SWITCH_APPROVER,   // Can approve kill switch requests

    // Audit roles
    AUDIT_VIEWER,      // Can view audit logs
    AUDIT_ADMIN,       // Can export/manage audit data

    // Admin roles
    PLATFORM_ADMIN     // Full platform access
}
```

```java
@Service
public class AuthorizationService {

    @PreAuthorize("hasRole('KILL_SWITCH_OPERATOR')")
    public String requestKillSwitchActivation(String reason, String requestedBy) {
        // Only operators can initiate
        return dualControlKillSwitch.requestGlobalActivation(reason, requestedBy, false);
    }

    @PreAuthorize("hasRole('KILL_SWITCH_APPROVER') and #approverEmail != authentication.name")
    public boolean approveKillSwitch(String requestId, String approverEmail) {
        // Approver must be different from requester
        return dualControlKillSwitch.approve(requestId, approverEmail);
    }
}
```

---

## Secrets Management

### Environment Variables (Development Only)

```bash
# NOT for production - development only
export OPENAI_API_KEY="sk-..."
export GCP_PROJECT_ID="your-project"
```

### HashiCorp Vault Integration

```yaml
spring:
  cloud:
    vault:
      uri: https://vault.yourbank.com:8200
      authentication: KUBERNETES  # or TOKEN, APPROLE
      kubernetes:
        role: regulus-agent
        service-account-token-file: /var/run/secrets/kubernetes.io/serviceaccount/token
      kv:
        enabled: true
        backend: secret
        default-context: regulus

regulus:
  ai:
    llm:
      openai:
        api-key: ${vault.openai-api-key}
      gemini:
        project-id: ${vault.gcp-project-id}
```

### GCP Secret Manager

```java
@Configuration
public class GcpSecretsConfig {

    @Bean
    public SecretManagerTemplate secretManagerTemplate() {
        return new SecretManagerTemplate(
            SecretManagerServiceClient.create()
        );
    }

    @Bean
    public String openAiApiKey(SecretManagerTemplate secretManager) {
        return secretManager.getSecretString(
            "projects/your-project/secrets/openai-api-key/versions/latest"
        );
    }
}
```

### AWS Secrets Manager

```java
@Configuration
public class AwsSecretsConfig {

    @Bean
    public SecretsManagerClient secretsManagerClient() {
        return SecretsManagerClient.builder()
            .region(Region.EU_WEST_2)  // London
            .build();
    }

    @Bean
    public String openAiApiKey(SecretsManagerClient client) {
        GetSecretValueRequest request = GetSecretValueRequest.builder()
            .secretId("regulus/openai-api-key")
            .build();
        return client.getSecretValue(request).secretString();
    }
}
```

---

## Network Security

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
      - TLS_CHACHA20_POLY1305_SHA256
```

### Rate Limiting

```yaml
regulus:
  ai:
    security:
      rate-limiting:
        enabled: true
        default-limit: 100       # requests per minute
        burst-capacity: 150
        endpoints:
          "/api/agent/**": 50    # Lower limit for agent calls
          "/mcp/**": 200         # Higher for MCP
          "/actuator/**": 10     # Minimal for actuator
```

```java
@Configuration
public class RateLimitingConfig {

    @Bean
    public RateLimiter agentRateLimiter() {
        return RateLimiter.create(50);  // 50 requests/second
    }

    @Bean
    public RateLimiterAspect rateLimiterAspect(RateLimiter rateLimiter) {
        return new RateLimiterAspect(rateLimiter);
    }
}

@Aspect
@Component
public class RateLimiterAspect {

    @Around("@annotation(RateLimited)")
    public Object rateLimit(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!rateLimiter.tryAcquire(Duration.ofSeconds(1))) {
            throw new RateLimitExceededException("Rate limit exceeded");
        }
        return joinPoint.proceed();
    }
}
```

### Network Segmentation

```yaml
# Kubernetes NetworkPolicy example
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: regulus-agent-policy
  namespace: regulus
spec:
  podSelector:
    matchLabels:
      app: regulus-agent
  policyTypes:
    - Ingress
    - Egress
  ingress:
    - from:
        - namespaceSelector:
            matchLabels:
              name: api-gateway
      ports:
        - port: 8080
  egress:
    # Allow LLM provider endpoints
    - to:
        - ipBlock:
            cidr: 0.0.0.0/0
      ports:
        - port: 443
    # Allow internal services
    - to:
        - namespaceSelector:
            matchLabels:
              name: core-banking
      ports:
        - port: 8443
```

---

## Input Validation & Sanitization

### Prompt Injection Prevention

```java
@Component
public class PromptSanitizer {

    private static final List<Pattern> INJECTION_PATTERNS = List.of(
        Pattern.compile("(?i)ignore\\s+(previous|above|all)\\s+instructions"),
        Pattern.compile("(?i)you\\s+are\\s+now\\s+a"),
        Pattern.compile("(?i)system\\s*:\\s*"),
        Pattern.compile("(?i)\\[INST\\]"),
        Pattern.compile("(?i)<<SYS>>")
    );

    public String sanitize(String input) {
        String sanitized = input;

        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(sanitized).find()) {
                auditLogger.logSecurityEvent("PROMPT_INJECTION_ATTEMPT", input);
                sanitized = pattern.matcher(sanitized).replaceAll("[FILTERED]");
            }
        }

        return sanitized;
    }

    public void validateInput(String input) {
        // Length limits
        if (input.length() > 10000) {
            throw new InputValidationException("Input exceeds maximum length");
        }

        // Character validation
        if (!input.matches("^[\\p{L}\\p{N}\\p{P}\\p{Z}]+$")) {
            throw new InputValidationException("Input contains invalid characters");
        }
    }
}
```

### MCP Tool Input Validation

```java
@Component
@McpTool(name = "transfer_funds")
public class TransferFundsTool implements McpToolHandler {

    @Override
    public McpToolResult execute(Map<String, Object> arguments) {
        // Validate all inputs
        String fromAccount = validateAccountId(arguments.get("from_account"));
        String toAccount = validateAccountId(arguments.get("to_account"));
        BigDecimal amount = validateAmount(arguments.get("amount"));
        String currency = validateCurrency(arguments.get("currency"));

        // Validate business rules
        if (fromAccount.equals(toAccount)) {
            throw new ValidationException("Cannot transfer to same account");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Amount must be positive");
        }

        // Execute with validated inputs
        return executeTransfer(fromAccount, toAccount, amount, currency);
    }

    private String validateAccountId(Object value) {
        if (value == null) {
            throw new ValidationException("Account ID required");
        }
        String accountId = value.toString();
        if (!accountId.matches("^[A-Z]{2}\\d{14}$")) {
            throw new ValidationException("Invalid account ID format");
        }
        return accountId;
    }

    private BigDecimal validateAmount(Object value) {
        try {
            BigDecimal amount = new BigDecimal(value.toString());
            if (amount.scale() > 2) {
                throw new ValidationException("Amount cannot have more than 2 decimal places");
            }
            return amount;
        } catch (NumberFormatException e) {
            throw new ValidationException("Invalid amount format");
        }
    }
}
```

---

## Audit Logging Security

### Immutable Audit Trail

```yaml
regulus:
  ai:
    audit:
      storage:
        type: IMMUTABLE  # S3 Object Lock, Azure Immutable Blob, GCS Object Lock
        retention-days: 2555  # 7 years for financial services
        encryption: AES_256
      integrity:
        hash-algorithm: SHA-256
        chain-validation: true  # Hash chain for tamper detection
```

```java
@Service
public class SecureAuditLogger {

    private final AuditEventRepository repository;
    private String previousHash = "GENESIS";

    @Transactional
    public void logEvent(AuditEvent event) {
        // Add integrity hash chain
        String eventHash = calculateHash(event, previousHash);
        event.setHash(eventHash);
        event.setPreviousHash(previousHash);

        // Persist to immutable storage
        repository.save(event);

        // Update chain
        previousHash = eventHash;
    }

    private String calculateHash(AuditEvent event, String previousHash) {
        String content = String.join("|",
            event.getTimestamp().toString(),
            event.getEventType(),
            event.getActorId(),
            event.getDetails(),
            previousHash
        );
        return DigestUtils.sha256Hex(content);
    }

    public boolean validateChainIntegrity() {
        List<AuditEvent> events = repository.findAllOrderByTimestamp();
        String expectedPrevious = "GENESIS";

        for (AuditEvent event : events) {
            if (!event.getPreviousHash().equals(expectedPrevious)) {
                return false;  // Chain broken
            }
            String recalculatedHash = calculateHash(event, expectedPrevious);
            if (!event.getHash().equals(recalculatedHash)) {
                return false;  // Event tampered
            }
            expectedPrevious = event.getHash();
        }
        return true;
    }
}
```

### Sensitive Data in Logs

```java
@Aspect
@Component
public class AuditRedactionAspect {

    private static final List<String> SENSITIVE_FIELDS = List.of(
        "password", "apiKey", "token", "secret",
        "nationalInsuranceNumber", "sortCode", "accountNumber",
        "cardNumber", "cvv", "pin"
    );

    @Around("@annotation(AuditLogged)")
    public Object redactSensitiveData(ProceedingJoinPoint joinPoint) throws Throwable {
        // Redact sensitive fields from arguments before logging
        Object[] redactedArgs = redactArguments(joinPoint.getArgs());

        auditLogger.logMethodEntry(
            joinPoint.getSignature().getName(),
            redactedArgs
        );

        Object result = joinPoint.proceed();

        // Redact sensitive fields from result
        Object redactedResult = redactResult(result);
        auditLogger.logMethodExit(
            joinPoint.getSignature().getName(),
            redactedResult
        );

        return result;
    }

    private Object[] redactArguments(Object[] args) {
        // Deep redaction of sensitive fields
        return Arrays.stream(args)
            .map(this::redactObject)
            .toArray();
    }

    private Object redactObject(Object obj) {
        if (obj == null) return null;
        if (obj instanceof String) return obj;
        if (obj instanceof Number) return obj;

        // Use reflection to find and redact sensitive fields
        // Returns copy with [REDACTED] for sensitive values
        return RedactionUtils.redact(obj, SENSITIVE_FIELDS);
    }
}
```

---

## LLM-Specific Security

### Response Validation

```java
@Service
public class LlmResponseValidator {

    public void validateResponse(AiMessage response) {
        String content = response.text();

        // Check for PII leakage in response
        if (piiDetector.containsPII(content)) {
            auditLogger.logSecurityEvent("PII_IN_RESPONSE", "[REDACTED]");
            throw new SecurityException("Response contains PII");
        }

        // Check for harmful content
        if (harmfulContentDetector.isHarmful(content)) {
            auditLogger.logSecurityEvent("HARMFUL_CONTENT", "[REDACTED]");
            throw new SecurityException("Response contains harmful content");
        }

        // Check for prompt leakage (system prompt appearing in output)
        if (promptLeakageDetector.detectsLeakage(content)) {
            auditLogger.logSecurityEvent("PROMPT_LEAKAGE", "[REDACTED]");
            throw new SecurityException("Response contains system prompt");
        }
    }
}
```

### Token Limits

```yaml
regulus:
  ai:
    llm:
      security:
        max-input-tokens: 8000
        max-output-tokens: 4000
        max-context-window: 32000
        cost-limit-per-request-gbp: 0.50
        cost-limit-daily-gbp: 1000.00
```

### Model Access Control

```java
@Service
public class ModelAccessControl {

    private final Map<String, Set<String>> modelPermissions = Map.of(
        "gemini-1.5-pro", Set.of("AGENT_USER", "AGENT_ADMIN"),
        "gpt-4", Set.of("AGENT_ADMIN"),  // Premium model
        "claude-3-opus", Set.of("PLATFORM_ADMIN")  // Restricted
    );

    public void validateModelAccess(String model, Authentication auth) {
        Set<String> allowedRoles = modelPermissions.get(model);
        if (allowedRoles == null) {
            throw new UnknownModelException("Model not registered: " + model);
        }

        boolean hasAccess = auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(allowedRoles::contains);

        if (!hasAccess) {
            auditLogger.logSecurityEvent("MODEL_ACCESS_DENIED",
                Map.of("model", model, "user", auth.getName()));
            throw new AccessDeniedException("Not authorized for model: " + model);
        }
    }
}
```

---

## Container Security

### Dockerfile Best Practices

```dockerfile
# Use distroless base image
FROM gcr.io/distroless/java21-debian12:nonroot

# Run as non-root user
USER nonroot:nonroot

# Copy only necessary artifacts
COPY --chown=nonroot:nonroot target/regulus-agent.jar /app/app.jar

# No shell access
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD ["java", "-cp", "/app/app.jar", "HealthCheck"]
```

### Kubernetes Security Context

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: regulus-agent
spec:
  template:
    spec:
      securityContext:
        runAsNonRoot: true
        runAsUser: 65534
        fsGroup: 65534
        seccompProfile:
          type: RuntimeDefault
      containers:
        - name: agent
          securityContext:
            allowPrivilegeEscalation: false
            readOnlyRootFilesystem: true
            capabilities:
              drop:
                - ALL
          resources:
            limits:
              memory: "2Gi"
              cpu: "1000m"
            requests:
              memory: "1Gi"
              cpu: "500m"
```

---

## Security Monitoring

### Security Events to Monitor

| Event | Severity | Alert Threshold |
|-------|----------|-----------------|
| `PROMPT_INJECTION_ATTEMPT` | High | Any occurrence |
| `PII_IN_RESPONSE` | Critical | Any occurrence |
| `RATE_LIMIT_EXCEEDED` | Medium | >10/minute per IP |
| `AUTHENTICATION_FAILURE` | Medium | >5/minute per user |
| `MODEL_ACCESS_DENIED` | Medium | Any occurrence |
| `KILL_SWITCH_ACTIVATED` | Critical | Any occurrence |
| `DATA_RESIDENCY_VIOLATION` | Critical | Any occurrence |

### Prometheus Alerts

```yaml
groups:
  - name: regulus-security
    rules:
      - alert: PromptInjectionAttempt
        expr: increase(regulus_security_events_total{type="PROMPT_INJECTION_ATTEMPT"}[5m]) > 0
        labels:
          severity: high
        annotations:
          summary: "Prompt injection attempt detected"

      - alert: AuthenticationFailureSpike
        expr: increase(regulus_auth_failures_total[5m]) > 10
        labels:
          severity: high
        annotations:
          summary: "Authentication failure spike detected"

      - alert: DataResidencyViolation
        expr: increase(regulus_data_residency_violations_total[5m]) > 0
        labels:
          severity: critical
        annotations:
          summary: "Data residency violation attempted"
```

---

## Compliance Mapping

| Security Control | PRA PS21/3 | FCA SYSC | UK GDPR | DORA |
|------------------|------------|----------|---------|------|
| Authentication | ✓ | ✓ | Art 32 | Art 9 |
| Authorization | ✓ | SYSC 13.9 | Art 32 | Art 9 |
| Encryption (TLS) | ✓ | ✓ | Art 32 | Art 9 |
| Audit logging | ✓ | ✓ | Art 30 | Art 12 |
| Input validation | ✓ | ✓ | Art 25 | Art 9 |
| Rate limiting | ✓ | - | - | Art 9 |
| Network segmentation | ✓ | - | Art 32 | Art 9 |
| Secrets management | ✓ | ✓ | Art 32 | Art 9 |

---

## Related Documentation

- [Troubleshooting Guide](./troubleshooting.md)
- [Kill Switch Design](../governance/kill-switch.md)
- [Data Residency Guide](./data-residency.md)
- [Regulatory Reference](../references/regulatory-reference.md)
