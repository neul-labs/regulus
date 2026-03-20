# Regulus AI Platform - Gap Closure Plan

## Purpose

This document provides a **concrete, actionable plan** to close the gaps identified in the UK Financial Services adoption assessment. Unlike the aspirational roadmap, this document maps directly to code that needs to be written.

**Assessment Summary**: Platform is ~30% complete with solid architecture but missing critical production infrastructure.

---

## Priority Legend

| Priority | Meaning | Timeline |
|----------|---------|----------|
| 🔴 P0 | Adoption blocker - cannot demo without this | Weeks 1-4 |
| 🟠 P1 | Production blocker - cannot deploy without this | Weeks 5-8 |
| 🟡 P2 | Compliance requirement - needed for regulatory sign-off | Weeks 9-14 |
| 🟢 P3 | Enhancement - improves adoption but not blocking | Weeks 15-20 |

---

## Gap 1: No Database Persistence

**Status**: 🔴 NOT IMPLEMENTED
**Priority**: 🔴 P0
**Effort**: 2 weeks
**Impact**: Audit trail lost on restart, FCA compliance breach

### Current State
- All audit events logged to SLF4J only
- Kill switch state stored in ConcurrentHashMap
- Policy decisions not persisted
- No model inventory storage

### Target State
- PostgreSQL persistence for all compliance data
- Flyway migrations for schema versioning
- JPA repositories with audit timestamps
- Read replicas for reporting queries

### Implementation Tasks

#### Task 1.1: Create persistence module
```
New module: platform/core/regulus-ai-persistence/
```

**Files to create:**
| File | Description |
|------|-------------|
| `build.gradle.kts` | JPA, PostgreSQL driver, Flyway dependencies |
| `entity/AuditEventEntity.java` | Immutable audit log entity |
| `entity/KillSwitchStateEntity.java` | Kill switch state with versioning |
| `entity/PolicyDecisionEntity.java` | Policy evaluation results |
| `entity/ModelRegistryEntry.java` | SS1/23 model inventory |
| `entity/ConsentRecord.java` | GDPR consent tracking |
| `repository/AuditEventRepository.java` | Spring Data JPA repository |
| `repository/KillSwitchStateRepository.java` | With optimistic locking |
| `repository/PolicyDecisionRepository.java` | With time-based queries |
| `repository/ModelRegistryRepository.java` | With LEI lookup |
| `repository/ConsentRecordRepository.java` | With subject lookup |
| `config/PersistenceAutoConfiguration.java` | Auto-wire repositories |
| `config/PersistenceProperties.java` | Configuration properties |

**Migrations to create:**
```
src/main/resources/db/migration/
├── V001__create_audit_events.sql
├── V002__create_kill_switch_states.sql
├── V003__create_policy_decisions.sql
├── V004__create_model_registry.sql
├── V005__create_consent_records.sql
└── V006__create_indexes.sql
```

#### Task 1.2: Implement DatabaseAuditEventSink
```java
// Replace current stub in regulus-ai-observability
public class DatabaseAuditEventSink implements AuditEventSink {
    private final AuditEventRepository repository;

    @Override
    @Transactional
    public void publish(AuditEvent event) {
        repository.save(toEntity(event));
    }
}
```

#### Task 1.3: Implement DatabaseKillSwitchStateProvider
```java
// Replace InMemoryKillSwitchStateProvider
public class DatabaseKillSwitchStateProvider implements KillSwitchStateProvider {
    private final KillSwitchStateRepository repository;

    @Override
    @Transactional
    public void save(KillSwitchState state) {
        // Optimistic locking for concurrent updates
        repository.saveAndFlush(toEntity(state));
    }
}
```

#### Task 1.4: Update settings.gradle.kts
```kotlin
include("platform:core:regulus-ai-persistence")
```

#### Task 1.5: Update BOM
```kotlin
// In regulus-ai-bom/build.gradle.kts
api(project(":platform:core:regulus-ai-persistence"))

// Add dependencies
api("org.springframework.boot:spring-boot-starter-data-jpa:3.3.0")
api("org.postgresql:postgresql:42.7.3")
api("org.flywaydb:flyway-core:10.10.0")
api("org.flywaydb:flyway-database-postgresql:10.10.0")
```

### Tests Required
| Test | Coverage |
|------|----------|
| `AuditEventRepositoryTest.java` | CRUD, time-range queries, pagination |
| `KillSwitchStateRepositoryTest.java` | Optimistic locking, concurrent updates |
| `DatabaseAuditEventSinkIntegrationTest.java` | Full flow with Testcontainers |
| `DatabaseKillSwitchStateProviderIntegrationTest.java` | Cluster sync simulation |

### Acceptance Criteria
- [ ] Audit events persist across application restarts
- [ ] Kill switch state survives deployment
- [ ] Query for last 1M audit events completes in <5 seconds
- [ ] Migrations run automatically on startup

---

## Gap 2: No LLM Integration

**Status**: 🔴 NOT IMPLEMENTED
**Priority**: 🔴 P0
**Effort**: 2 weeks
**Impact**: Platform has no AI capability - just governance shell

### Current State
- LangChain4j declared in BOM but not wired
- No prompt templates
- No provider abstraction
- No token counting or cost tracking

### Target State
- Multi-provider support (OpenAI, Anthropic, Bedrock, Azure)
- Provider abstraction with fallback
- Prompt template system
- Token/cost tracking per request

### Implementation Tasks

#### Task 2.1: Create LLM module
```
New module: platform/core/regulus-ai-llm/
```

**Files to create:**
| File | Description |
|------|-------------|
| `build.gradle.kts` | LangChain4j dependencies |
| `LlmClient.java` | Provider-agnostic interface |
| `LlmRequest.java` | Request with model, prompt, parameters |
| `LlmResponse.java` | Response with tokens, cost, content |
| `provider/OpenAiLlmClient.java` | OpenAI GPT implementation |
| `provider/AnthropicLlmClient.java` | Claude implementation |
| `provider/BedrockLlmClient.java` | AWS Bedrock implementation |
| `provider/AzureOpenAiLlmClient.java` | Azure OpenAI implementation |
| `router/LlmRouter.java` | Provider selection and fallback |
| `router/RoutingStrategy.java` | Cost-based, latency-based, capability-based |
| `template/PromptTemplate.java` | Variable substitution |
| `template/PromptTemplateRegistry.java` | Named template lookup |
| `cost/TokenCounter.java` | Tiktoken-based counting |
| `cost/CostCalculator.java` | Provider pricing tables |
| `config/LlmProperties.java` | All provider configs |
| `config/LlmAutoConfiguration.java` | Auto-wire based on config |

#### Task 2.2: Wire into McpClient
```java
// Update HttpMcpClient to use LlmClient for tool execution
@RequiredArgsConstructor
public class McpAgentExecutor {
    private final LlmClient llmClient;
    private final McpClient mcpClient;

    public CompletableFuture<AgentResponse> execute(AgentRequest request) {
        // 1. Apply policy guards
        // 2. Apply privacy filters
        // 3. Call LLM via LlmClient
        // 4. Parse tool calls from response
        // 5. Execute tools via McpClient
        // 6. Return final response with audit
    }
}
```

#### Task 2.3: Implement token counting
```java
public class TiktokenCounter implements TokenCounter {
    private final Map<String, Encoding> encodings;

    @Override
    public int countTokens(String text, String model) {
        Encoding enc = encodings.get(modelToEncoding(model));
        return enc.encode(text).size();
    }
}
```

### Configuration Example
```yaml
regulus:
  ai:
    llm:
      default-provider: openai
      providers:
        openai:
          enabled: true
          api-key: ${OPENAI_API_KEY}
          model: gpt-4o
          timeout: 30s
          max-tokens: 4096
        anthropic:
          enabled: true
          api-key: ${ANTHROPIC_API_KEY}
          model: claude-3-5-sonnet
        bedrock:
          enabled: false
          region: eu-west-2
          model: anthropic.claude-3-sonnet
      routing:
        strategy: cost-optimized
        fallback-order: [openai, anthropic, bedrock]
      cost-tracking:
        enabled: true
        alert-threshold-daily: 100.00
```

### Tests Required
| Test | Coverage |
|------|----------|
| `OpenAiLlmClientTest.java` | Mock HTTP, error handling |
| `LlmRouterTest.java` | Fallback scenarios |
| `TokenCounterTest.java` | Accuracy vs reference |
| `CostCalculatorTest.java` | All providers pricing |
| `LlmClientIntegrationTest.java` | Real API (optional, CI skip) |

### Acceptance Criteria
- [ ] Can invoke OpenAI, Anthropic, Bedrock via unified API
- [ ] Token count accurate within 5% of provider's count
- [ ] Fallback triggers when primary provider returns 5xx
- [ ] Cost tracked and audited per request

---

## Gap 3: Security Disabled by Default

**Status**: 🔴 NOT IMPLEMENTED (configuration exists, no real implementation)
**Priority**: 🔴 P0
**Effort**: 2 weeks
**Impact**: Unacceptable for regulated environment

### Current State
- API key filter exists but keys stored in plain text
- OAuth2 properties exist but no resource server
- mTLS properties exist but not implemented
- Security explicitly disabled in quickstart

### Target State
- OAuth2 Resource Server with JWT validation
- API keys hashed with bcrypt
- mTLS for inter-service communication
- RBAC with method-level security
- Security enabled by default

### Implementation Tasks

#### Task 3.1: Implement OAuth2 Resource Server
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class OAuth2SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/health").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/**").hasRole("AGENT_INVOKE")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthConverter())
                )
            );
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        // Support Azure AD, Keycloak, Auth0
        return JwtDecoders.fromIssuerLocation(issuerUri);
    }
}
```

#### Task 3.2: Implement secure API key storage
```java
@Entity
public class ApiKeyEntity {
    @Id
    private String keyId; // Public identifier

    @Column(nullable = false)
    private String keyHash; // BCrypt hash

    @Column(nullable = false)
    private String clientId;

    @ElementCollection
    private Set<String> roles;

    private Instant expiresAt;
    private boolean revoked;
}

@Service
public class SecureApiKeyService {
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    public ApiKeyPair generate(String clientId, Set<String> roles) {
        String rawKey = generateSecureKey();
        String keyId = generateKeyId();
        String hash = encoder.encode(rawKey);

        repository.save(new ApiKeyEntity(keyId, hash, clientId, roles));

        // Return raw key ONCE - cannot be retrieved again
        return new ApiKeyPair(keyId, rawKey);
    }

    public boolean validate(String keyId, String rawKey) {
        return repository.findById(keyId)
            .filter(k -> !k.isRevoked())
            .filter(k -> k.getExpiresAt() == null || k.getExpiresAt().isAfter(Instant.now()))
            .map(k -> encoder.matches(rawKey, k.getKeyHash()))
            .orElse(false);
    }
}
```

#### Task 3.3: Implement mTLS
```java
@Configuration
@ConditionalOnProperty("regulus.ai.security.mtls.enabled")
public class MtlsConfig {

    @Bean
    public WebClient mtlsWebClient(MtlsProperties props) {
        SslContext sslContext = SslContextBuilder.forClient()
            .keyManager(loadKeyStore(props.getKeyStore()))
            .trustManager(loadTrustStore(props.getTrustStore()))
            .build();

        HttpClient httpClient = HttpClient.create()
            .secure(spec -> spec.sslContext(sslContext));

        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }
}
```

#### Task 3.4: Implement RBAC
```java
public enum Permission {
    AGENT_INVOKE,      // Can call agents
    AGENT_ADMIN,       // Can manage agent config
    KILL_SWITCH_VIEW,  // Can view kill switch status
    KILL_SWITCH_ADMIN, // Can toggle kill switch
    AUDIT_READ,        // Can read audit logs
    POLICY_ADMIN,      // Can modify policies
    MODEL_ADMIN        // Can manage model registry
}

@Service
@PreAuthorize("hasPermission('AGENT_INVOKE')")
public class AgentService {

    @PreAuthorize("hasPermission('KILL_SWITCH_ADMIN')")
    public void toggleKillSwitch(String scope, boolean enabled) {
        // Requires elevated permission
    }
}
```

### Configuration Example
```yaml
regulus:
  ai:
    security:
      enabled: true  # DEFAULT: true (not false!)
      oauth2:
        enabled: true
        issuer-uri: https://login.microsoftonline.com/{tenant}/v2.0
        audience: api://regulus-platform
      api-key:
        enabled: true
        header-name: X-API-Key
        # Keys stored in database, not config!
      mtls:
        enabled: true
        key-store: classpath:keystore.p12
        key-store-password: ${KEYSTORE_PASSWORD}
        trust-store: classpath:truststore.p12
      rbac:
        enabled: true
        admin-roles: [PLATFORM_ADMIN]
```

### Tests Required
| Test | Coverage |
|------|----------|
| `OAuth2SecurityTest.java` | JWT validation, role extraction |
| `ApiKeyAuthenticationTest.java` | Valid/invalid/expired keys |
| `MtlsConnectionTest.java` | Certificate validation |
| `RbacAuthorizationTest.java` | Permission enforcement |
| `SecurityBypassAttemptTest.java` | Attack vectors |

### Acceptance Criteria
- [ ] All endpoints require authentication (except health)
- [ ] JWT validated against JWKS endpoint
- [ ] API keys hashed, never logged or returned
- [ ] mTLS enforced for MCP/A2A communication
- [ ] Method-level authorization working

---

## Gap 4: Kafka Audit Sink is Empty Stub

**Status**: 🔴 NOT IMPLEMENTED
**Priority**: 🟠 P1
**Effort**: 1 week
**Impact**: Audit events not durable, compliance risk

### Current State
```java
// Current implementation - does nothing!
public class KafkaAuditEventSink implements AuditEventSink {
    @Override
    public void publish(AuditEvent event) {
        // TODO: Implement Kafka publishing
    }
}
```

### Target State
- Kafka producer with exactly-once semantics
- JSON/Avro serialization with schema registry
- Dead letter queue for failed events
- Batch publishing for performance

### Implementation Tasks

#### Task 4.1: Implement KafkaAuditEventSink properly
```java
@RequiredArgsConstructor
public class KafkaAuditEventSink implements AuditEventSink {
    private final KafkaTemplate<String, AuditEvent> kafkaTemplate;
    private final KafkaAuditProperties properties;

    @Override
    public void publish(AuditEvent event) {
        ProducerRecord<String, AuditEvent> record = new ProducerRecord<>(
            properties.getTopic(),
            event.correlationId(),
            event
        );

        // Add headers for routing/filtering
        record.headers()
            .add("event-type", event.eventType().getBytes())
            .add("timestamp", String.valueOf(event.timestamp()).getBytes());

        kafkaTemplate.send(record)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    handlePublishFailure(event, ex);
                }
            });
    }

    private void handlePublishFailure(AuditEvent event, Throwable ex) {
        // Send to DLQ
        kafkaTemplate.send(properties.getDlqTopic(), event);
        log.error("Failed to publish audit event, sent to DLQ", ex);
    }
}
```

#### Task 4.2: Add Kafka configuration
```java
@Configuration
@ConditionalOnProperty("regulus.ai.observability.audit.kafka.enabled")
public class KafkaAuditConfig {

    @Bean
    public ProducerFactory<String, AuditEvent> auditProducerFactory(
            KafkaAuditProperties props) {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, props.getBootstrapServers());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, AuditEvent> auditKafkaTemplate(
            ProducerFactory<String, AuditEvent> factory) {
        return new KafkaTemplate<>(factory);
    }
}
```

### Configuration Example
```yaml
regulus:
  ai:
    observability:
      audit:
        kafka:
          enabled: true
          bootstrap-servers: kafka:9092
          topic: regulus.audit.events
          dlq-topic: regulus.audit.events.dlq
          acks: all
          retries: 3
          batch-size: 16384
          linger-ms: 100
```

### Tests Required
| Test | Coverage |
|------|----------|
| `KafkaAuditEventSinkTest.java` | Publish, serialization |
| `KafkaAuditFailureTest.java` | DLQ routing |
| `KafkaAuditIntegrationTest.java` | Testcontainers Kafka |

### Acceptance Criteria
- [ ] All audit events published to Kafka
- [ ] Events consumable by downstream systems
- [ ] Failed publishes routed to DLQ
- [ ] Exactly-once semantics verified

---

## Gap 5: Empty Governance and Safety Starters

**Status**: 🔴 NOT IMPLEMENTED
**Priority**: 🟠 P1
**Effort**: 1.5 weeks
**Impact**: False advertising, incomplete platform

### Current State
```java
// governance-starter - completely empty!
@AutoConfiguration
public class GovernanceAutoConfiguration {
}

// safety-starter - completely empty!
@AutoConfiguration
public class SafetyAutoConfiguration {
}
```

### Target State
- Governance starter auto-wires policy guards, model registry
- Safety starter auto-wires kill switch, privacy filters, safety classifiers

### Implementation Tasks

#### Task 5.1: Implement GovernanceAutoConfiguration
```java
@AutoConfiguration
@ConditionalOnProperty(prefix = "regulus.ai.governance", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(GovernanceProperties.class)
@Import({
    PolicyAutoConfiguration.class,
    ModelRegistryAutoConfiguration.class,
    ComplianceReportingAutoConfiguration.class
})
public class GovernanceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public PolicyEnforcer policyEnforcer(List<PolicyGuard> guards) {
        return new PolicyEnforcer(guards);
    }

    @Bean
    @ConditionalOnMissingBean
    public ModelRegistryClient modelRegistryClient(GovernanceProperties props) {
        return new HttpModelRegistryClient(props.getModelRegistry());
    }

    @Bean
    @ConditionalOnMissingBean
    public GovernanceInterceptor governanceInterceptor(
            PolicyEnforcer enforcer,
            ModelRegistryClient registry) {
        return new GovernanceInterceptor(enforcer, registry);
    }
}
```

#### Task 5.2: Implement SafetyAutoConfiguration
```java
@AutoConfiguration
@ConditionalOnProperty(prefix = "regulus.ai.safety", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(SafetyProperties.class)
@Import({
    KillSwitchAutoConfiguration.class,
    PrivacyAutoConfiguration.class,
    SafetyClassifierAutoConfiguration.class
})
public class SafetyAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SafetyGuard safetyGuard(
            KillSwitchManager killSwitch,
            PrivacyFilterChain privacyFilter,
            Optional<SafetyClassifier> classifier) {
        return new SafetyGuard(killSwitch, privacyFilter, classifier.orElse(null));
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty("regulus.ai.safety.classifier.enabled")
    public SafetyClassifier safetyClassifier(SafetyProperties props) {
        return new LlamaGuardSafetyClassifier(props.getClassifier());
    }

    @Bean
    @ConditionalOnMissingBean
    public PromptInjectionDetector promptInjectionDetector() {
        return new RuleBasedPromptInjectionDetector();
    }
}
```

#### Task 5.3: Implement Prompt Injection Detection
```java
public class RuleBasedPromptInjectionDetector implements PromptInjectionDetector {

    private static final List<Pattern> INJECTION_PATTERNS = List.of(
        Pattern.compile("ignore (previous|all|above) instructions", CASE_INSENSITIVE),
        Pattern.compile("system:\\s*", CASE_INSENSITIVE),
        Pattern.compile("\\[INST\\]|\\[/INST\\]", CASE_INSENSITIVE),
        Pattern.compile("you are now", CASE_INSENSITIVE),
        Pattern.compile("disregard", CASE_INSENSITIVE),
        Pattern.compile("forget (everything|what)", CASE_INSENSITIVE)
    );

    @Override
    public DetectionResult detect(String input) {
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(input).find()) {
                return DetectionResult.detected(pattern.pattern());
            }
        }
        return DetectionResult.clean();
    }
}
```

### Tests Required
| Test | Coverage |
|------|----------|
| `GovernanceAutoConfigurationTest.java` | Bean wiring, conditionals |
| `SafetyAutoConfigurationTest.java` | Bean wiring, conditionals |
| `PromptInjectionDetectorTest.java` | All patterns |
| `SafetyGuardIntegrationTest.java` | Full safety chain |

### Acceptance Criteria
- [ ] Adding governance-starter auto-wires all policy guards
- [ ] Adding safety-starter auto-wires kill switch and privacy
- [ ] Prompt injection blocked before reaching LLM
- [ ] Configuration conditionals work correctly

---

## Gap 6: No Integration Tests

**Status**: 🔴 NOT IMPLEMENTED
**Priority**: 🟠 P1
**Effort**: 2 weeks
**Impact**: Cannot prove components work together

### Current State
- 10 unit tests with mocks
- No Spring Boot context tests
- No database integration tests
- No Kafka integration tests

### Target State
- Testcontainers for PostgreSQL, Kafka, Redis
- Spring Boot test slices
- Full end-to-end agent invocation test
- Contract tests for MCP/A2A

### Implementation Tasks

#### Task 6.1: Create test infrastructure
```java
@TestConfiguration
public class TestContainersConfig {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("regulus_test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
}
```

#### Task 6.2: Create integration tests per module
```java
@SpringBootTest
@Testcontainers
class PolicyEnforcerIntegrationTest {

    @Autowired
    private PolicyEnforcer policyEnforcer;

    @Autowired
    private PolicyDecisionRepository repository;

    @Test
    void shouldPersistPolicyDecision() {
        PolicyContext context = PolicyContext.builder()
            .legalEntityIdentifier("529900T8BM49AURSDO55")
            .purposeCode("SERVICE_DELIVERY")
            .build();

        PolicyResult result = policyEnforcer.enforceAll(context);

        assertThat(result.isAllowed()).isTrue();

        // Verify persisted
        List<PolicyDecisionEntity> decisions = repository.findByCorrelationId(
            context.getCorrelationId());
        assertThat(decisions).hasSize(1);
    }
}
```

#### Task 6.3: Create end-to-end test
```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class AgentInvocationE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldInvokeAgentWithFullGovernance() {
        // Given: Valid request with all governance data
        var request = Map.of(
            "prompt", "What is the balance for account 12345?",
            "lei", "529900T8BM49AURSDO55",
            "purposeCode", "SERVICE_DELIVERY",
            "consentId", "CONSENT123"
        );

        // When: Invoke agent
        var response = restTemplate.postForEntity(
            "/api/agent/invoke",
            request,
            AgentResponse.class
        );

        // Then: Success with audit trail
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getAuditId()).isNotNull();

        // Verify audit persisted
        // Verify policy decision logged
        // Verify privacy filter applied
    }
}
```

### Tests to Create
| Test File | Module | Coverage |
|-----------|--------|----------|
| `PolicyEnforcerIntegrationTest.java` | policy | DB persistence |
| `PrivacyFilterIntegrationTest.java` | privacy | Filter chain |
| `KillSwitchIntegrationTest.java` | kill-switch | State persistence |
| `AuditKafkaIntegrationTest.java` | observability | Kafka publish |
| `LlmClientIntegrationTest.java` | llm | Provider calls |
| `McpServerIntegrationTest.java` | starter | Full MCP flow |
| `A2aServerIntegrationTest.java` | starter | Full A2A flow |
| `SecurityIntegrationTest.java` | starter | OAuth2 + RBAC |
| `AgentE2ETest.java` | quickstart | Full agent flow |

### Acceptance Criteria
- [ ] All integration tests pass with Testcontainers
- [ ] CI pipeline runs integration tests
- [ ] Test coverage >60% (up from 12%)
- [ ] E2E test demonstrates full governance flow

---

## Gap 7: No Global Exception Handler

**Status**: 🟡 PARTIAL (some exception classes exist)
**Priority**: 🟠 P1
**Effort**: 3 days
**Impact**: Inconsistent error responses, poor debugging

### Implementation Tasks

#### Task 7.1: Create GlobalExceptionHandler
```java
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(PolicyViolationException.class)
    public ResponseEntity<ErrorResponse> handlePolicyViolation(
            PolicyViolationException ex, WebRequest request) {
        log.warn("Policy violation: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ErrorResponse.builder()
                .errorCode("POLICY_VIOLATION")
                .message(ex.getMessage())
                .violations(ex.getViolations())
                .correlationId(getCorrelationId(request))
                .timestamp(Instant.now())
                .build());
    }

    @ExceptionHandler(KillSwitchActiveException.class)
    public ResponseEntity<ErrorResponse> handleKillSwitch(
            KillSwitchActiveException ex, WebRequest request) {
        log.error("Kill switch active: {}", ex.getReason());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(ErrorResponse.builder()
                .errorCode("KILL_SWITCH_ACTIVE")
                .message("Service temporarily disabled")
                .reason(ex.getReason())
                .correlationId(getCorrelationId(request))
                .timestamp(Instant.now())
                .build());
    }

    @ExceptionHandler(LlmProviderException.class)
    public ResponseEntity<ErrorResponse> handleLlmError(
            LlmProviderException ex, WebRequest request) {
        log.error("LLM provider error", ex);

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(ErrorResponse.builder()
                .errorCode("LLM_ERROR")
                .message("AI service temporarily unavailable")
                .correlationId(getCorrelationId(request))
                .timestamp(Instant.now())
                .build());
    }
}
```

#### Task 7.2: Create error code registry
```java
public enum ErrorCode {
    // Policy errors (4xx)
    POLICY_VIOLATION("POL001", "Policy check failed"),
    INVALID_LEI("POL002", "Invalid Legal Entity Identifier"),
    CONSENT_REQUIRED("POL003", "User consent not provided"),
    PURPOSE_NOT_ALLOWED("POL004", "Purpose code not permitted"),

    // Safety errors (5xx)
    KILL_SWITCH_ACTIVE("SAF001", "Kill switch is active"),
    PROMPT_INJECTION_DETECTED("SAF002", "Potential prompt injection detected"),

    // Infrastructure errors (5xx)
    LLM_UNAVAILABLE("INF001", "LLM provider unavailable"),
    DATABASE_ERROR("INF002", "Database operation failed"),
    KAFKA_PUBLISH_FAILED("INF003", "Audit event publish failed"),

    // Auth errors (4xx)
    UNAUTHORIZED("AUTH001", "Authentication required"),
    FORBIDDEN("AUTH002", "Insufficient permissions"),
    TOKEN_EXPIRED("AUTH003", "Token has expired");

    private final String code;
    private final String defaultMessage;
}
```

### Acceptance Criteria
- [ ] All exceptions return consistent JSON structure
- [ ] Correlation ID in all error responses
- [ ] No stack traces in production responses
- [ ] All error codes documented

---

## Gap 8: Circuit Breakers Not Wired

**Status**: 🟡 DECLARED BUT NOT IMPLEMENTED
**Priority**: 🟠 P1
**Effort**: 1 week
**Impact**: Cascading failures, no resilience

### Implementation Tasks

#### Task 8.1: Configure Resilience4j
```java
@Configuration
@EnableConfigurationProperties(ResilienceProperties.class)
public class ResilienceConfiguration {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(ResilienceProperties props) {
        CircuitBreakerConfig defaultConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .slowCallRateThreshold(50)
            .slowCallDurationThreshold(Duration.ofSeconds(2))
            .waitDurationInOpenState(Duration.ofSeconds(60))
            .slidingWindowType(SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .build();

        return CircuitBreakerRegistry.of(defaultConfig);
    }

    @Bean
    public RetryRegistry retryRegistry(ResilienceProperties props) {
        RetryConfig defaultConfig = RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(500))
            .exponentialBackoffMultiplier(2)
            .retryExceptions(IOException.class, TimeoutException.class)
            .ignoreExceptions(PolicyViolationException.class)
            .build();

        return RetryRegistry.of(defaultConfig);
    }
}
```

#### Task 8.2: Apply to LLM client
```java
@Service
public class ResilientLlmClient implements LlmClient {

    private final LlmClient delegate;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    @Override
    public LlmResponse invoke(LlmRequest request) {
        return Decorators.ofSupplier(() -> delegate.invoke(request))
            .withCircuitBreaker(circuitBreaker)
            .withRetry(retry)
            .withFallback(List.of(
                CallNotPermittedException.class,
                TimeoutException.class
            ), ex -> fallbackResponse(request, ex))
            .get();
    }

    private LlmResponse fallbackResponse(LlmRequest request, Throwable ex) {
        log.warn("LLM fallback triggered for request {}", request.getId(), ex);
        // Try secondary provider or return graceful error
        return trySecondaryProvider(request)
            .orElse(LlmResponse.unavailable(ex.getMessage()));
    }
}
```

### Configuration Example
```yaml
resilience4j:
  circuitbreaker:
    instances:
      llm-openai:
        failureRateThreshold: 50
        slowCallDurationThreshold: 5s
        waitDurationInOpenState: 30s
      llm-anthropic:
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
      mcp-client:
        failureRateThreshold: 30
        waitDurationInOpenState: 60s
  retry:
    instances:
      llm-provider:
        maxAttempts: 3
        waitDuration: 1s
        exponentialBackoffMultiplier: 2
```

### Acceptance Criteria
- [ ] Circuit breaker trips after 50% failure rate
- [ ] Retry with exponential backoff working
- [ ] Fallback to secondary provider functional
- [ ] Circuit breaker metrics exposed to Prometheus

---

## Gap 9: No Secrets Management

**Status**: 🔴 NOT IMPLEMENTED
**Priority**: 🟠 P1
**Effort**: 1 week
**Impact**: Plain text secrets in config files

### Implementation Tasks

#### Task 9.1: Integrate HashiCorp Vault
```java
@Configuration
@ConditionalOnProperty("regulus.ai.secrets.vault.enabled")
@EnableConfigurationProperties(VaultProperties.class)
public class VaultSecretsConfig {

    @Bean
    public VaultTemplate vaultTemplate(VaultProperties props) {
        VaultEndpoint endpoint = VaultEndpoint.from(props.getUri());

        ClientAuthentication auth = props.getAuthentication().equals("kubernetes")
            ? new KubernetesAuthentication(props.getKubernetes(), restOperations())
            : new TokenAuthentication(props.getToken());

        return new VaultTemplate(endpoint, auth);
    }

    @Bean
    public SecretProvider secretProvider(VaultTemplate vault, VaultProperties props) {
        return new VaultSecretProvider(vault, props.getSecretPath());
    }
}
```

#### Task 9.2: Create secret rotation service
```java
@Service
@RequiredArgsConstructor
public class SecretRotationService {

    private final VaultTemplate vault;
    private final List<SecretConsumer> consumers;

    @Scheduled(cron = "${regulus.ai.secrets.rotation-cron:0 0 2 * * ?}")
    public void rotateSecrets() {
        log.info("Starting scheduled secret rotation");

        for (SecretConsumer consumer : consumers) {
            try {
                String newSecret = vault.opsForTransit()
                    .rotate(consumer.getSecretName());
                consumer.onSecretRotated(newSecret);
                log.info("Rotated secret for {}", consumer.getSecretName());
            } catch (Exception e) {
                log.error("Failed to rotate secret for {}", consumer.getSecretName(), e);
            }
        }
    }
}
```

### Configuration Example
```yaml
regulus:
  ai:
    secrets:
      provider: vault  # or aws-secrets-manager, azure-keyvault
      vault:
        enabled: true
        uri: https://vault.internal:8200
        authentication: kubernetes
        kubernetes:
          role: regulus-platform
          service-account-token-path: /var/run/secrets/kubernetes.io/serviceaccount/token
        secret-path: secret/data/regulus
        rotation-cron: "0 0 2 * * ?"
```

### Acceptance Criteria
- [ ] No secrets in application.yaml
- [ ] Secrets loaded from Vault at startup
- [ ] Automatic rotation working
- [ ] Lease renewal for dynamic secrets

---

## Gap 10: GDPR Compliance Missing

**Status**: 🔴 NOT IMPLEMENTED
**Priority**: 🟡 P2
**Effort**: 2 weeks
**Impact**: GDPR Article 17 (right to erasure) not supported

### Implementation Tasks

#### Task 10.1: Create GDPR module
```
New module: platform/core/regulus-ai-gdpr/
```

#### Task 10.2: Implement Right to Erasure
```java
@Service
@RequiredArgsConstructor
public class RightToErasureService {

    private final List<DataStore> dataStores;
    private final AuditLogger auditLogger;

    @Transactional
    public ErasureResult eraseSubjectData(String subjectId, ErasureRequest request) {
        log.info("Processing erasure request for subject {}", subjectId);

        List<ErasureRecord> records = new ArrayList<>();

        for (DataStore store : dataStores) {
            try {
                int deleted = store.deleteBySubjectId(subjectId);
                records.add(new ErasureRecord(store.getName(), deleted, true));
            } catch (Exception e) {
                records.add(new ErasureRecord(store.getName(), 0, false, e.getMessage()));
            }
        }

        ErasureResult result = new ErasureResult(subjectId, records, Instant.now());

        auditLogger.logGdprErasure(result);

        return result;
    }
}
```

#### Task 10.3: Implement DSAR Export
```java
@Service
@RequiredArgsConstructor
public class DsarExportService {

    private final List<DataStore> dataStores;

    public DsarExport exportSubjectData(String subjectId) {
        Map<String, Object> data = new LinkedHashMap<>();

        for (DataStore store : dataStores) {
            List<?> storeData = store.findBySubjectId(subjectId);
            if (!storeData.isEmpty()) {
                data.put(store.getName(), storeData);
            }
        }

        return DsarExport.builder()
            .subjectId(subjectId)
            .exportedAt(Instant.now())
            .data(data)
            .format("JSON")
            .build();
    }
}
```

### Acceptance Criteria
- [ ] Right to erasure deletes all subject data
- [ ] DSAR export returns all data for subject
- [ ] Consent records tracked with purpose codes
- [ ] Data retention policies enforced

---

## Summary: Implementation Order

### Sprint 1 (Weeks 1-2): Foundation
| Task | Priority | Owner |
|------|----------|-------|
| Gap 1: Database persistence | 🔴 P0 | Backend |
| Gap 2: LLM integration (skeleton) | 🔴 P0 | AI Engineer |

### Sprint 2 (Weeks 3-4): Security & Audit
| Task | Priority | Owner |
|------|----------|-------|
| Gap 3: OAuth2 + RBAC | 🔴 P0 | Security |
| Gap 4: Kafka audit sink | 🟠 P1 | Backend |

### Sprint 3 (Weeks 5-6): Completeness
| Task | Priority | Owner |
|------|----------|-------|
| Gap 2: LLM integration (complete) | 🔴 P0 | AI Engineer |
| Gap 5: Governance/Safety starters | 🟠 P1 | Backend |
| Gap 7: Exception handler | 🟠 P1 | Backend |

### Sprint 4 (Weeks 7-8): Resilience
| Task | Priority | Owner |
|------|----------|-------|
| Gap 8: Circuit breakers | 🟠 P1 | Backend |
| Gap 9: Secrets management | 🟠 P1 | DevOps |
| Gap 6: Integration tests (phase 1) | 🟠 P1 | All |

### Sprint 5 (Weeks 9-10): Testing
| Task | Priority | Owner |
|------|----------|-------|
| Gap 6: Integration tests (phase 2) | 🟠 P1 | All |
| Security penetration testing | 🟠 P1 | Security |

### Sprint 6 (Weeks 11-14): Compliance
| Task | Priority | Owner |
|------|----------|-------|
| Gap 10: GDPR compliance | 🟡 P2 | Backend + Compliance |
| SS1/23 model registry | 🟡 P2 | Backend |
| PS21/3 dual-control kill switch | 🟡 P2 | Backend |

### Sprint 7+ (Weeks 15-20): Polish
| Task | Priority | Owner |
|------|----------|-------|
| Documentation & runbooks | 🟢 P3 | Tech Writer |
| Monitoring dashboards | 🟢 P3 | DevOps |
| Performance testing | 🟢 P3 | QA |
| Domain-specific starters | 🟢 P3 | Backend |

---

## Definition of Done

Each gap is considered **closed** when:

1. **Code**: Implementation merged to main branch
2. **Tests**: Unit + integration tests passing, coverage >80%
3. **Config**: Configuration documented in application.yaml
4. **Docs**: README updated, API documented
5. **Review**: Code reviewed by 2+ engineers
6. **Demo**: Working demonstration in quickstart

---

## Tracking

Track progress in GitHub Issues with labels:
- `gap-closure` - All tasks from this plan
- `p0-blocker` - Adoption blockers
- `p1-production` - Production requirements
- `p2-compliance` - Regulatory requirements
- `p3-enhancement` - Nice to have

---

*Document Version: 1.0*
*Created: 2024-12-09*
*Owner: Platform Team*
