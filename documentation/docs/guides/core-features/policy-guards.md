# Policy Guards

Policy guards enforce compliance rules before processing requests. They validate purpose codes, consent status, and other regulatory requirements.

## Overview

Policy guards act as gatekeepers that ensure every AI interaction complies with:

- **UK GDPR** - Lawful basis, consent, purpose limitation
- **FCA Consumer Duty** - Fair treatment, consumer outcomes
- **Internal policies** - Business rules, risk controls

## Configuration

### Basic Configuration

```yaml title="application.yml"
regulus:
  policy:
    enabled: true
    require-purpose-code: true
    require-consent: true
    allowed-purpose-codes:
      - CUSTOMER_SUPPORT
      - ACCOUNT_INQUIRY
      - FRAUD_DETECTION
      - COMPLAINT_HANDLING
```

### Full Configuration

```yaml title="application.yml"
regulus:
  policy:
    enabled: true

    # Purpose code enforcement
    require-purpose-code: true
    allowed-purpose-codes:
      - CUSTOMER_SUPPORT
      - ACCOUNT_INQUIRY
      - FRAUD_DETECTION
      - COMPLAINT_HANDLING
      - PRODUCT_RECOMMENDATION

    # Consent enforcement
    require-consent: true
    consent-types:
      - EXPLICIT
      - LEGITIMATE_INTEREST

    # LEI validation (Legal Entity Identifier)
    require-lei: false
    lei-validation:
      enabled: true
      cache-ttl: 24h

    # Additional guards
    guards:
      - type: rate-limit
        requests-per-minute: 100
        per-user: true
      - type: content-filter
        block-categories:
          - FINANCIAL_ADVICE
          - INVESTMENT_RECOMMENDATION

    # Audit all violations
    audit:
      log-violations: true
      alert-on-violation: true
```

## Using Policy Guards

### Basic Usage

```java
@Service
public class AgentService {

    private final PolicyGuard policyGuard;
    private final LlmClient llmClient;

    public Mono<String> process(AgentRequest request) {
        PolicyContext context = PolicyContext.builder()
            .purposeCode(request.purposeCode())
            .hasConsent(request.hasConsent())
            .userId(request.userId())
            .build();

        return policyGuard.enforce(context)
            .then(llmClient.chat(request.message()))
            .map(ChatResponse::content);
    }
}
```

### Handling Violations

```java
public Mono<String> processWithErrorHandling(AgentRequest request) {
    return policyGuard.enforce(buildContext(request))
        .then(llmClient.chat(request.message()))
        .onErrorResume(PolicyViolationException.class, e -> {
            auditLogger.logViolation(request.userId(), e);
            return Mono.error(new ForbiddenException(
                "Request cannot be processed: " + e.getMessage()
            ));
        });
}
```

## Built-in Guards

### Purpose Code Guard

Validates that requests have an allowed purpose code:

```java
@Component
public class PurposeCodeGuard implements PolicyGuard {

    private final Set<String> allowedCodes;

    @Override
    public Mono<Void> enforce(PolicyContext context) {
        if (!allowedCodes.contains(context.purposeCode())) {
            return Mono.error(new PolicyViolationException(
                "Invalid purpose code: " + context.purposeCode()
            ));
        }
        return Mono.empty();
    }
}
```

### Consent Guard

Ensures consent has been obtained:

```java
@Component
public class ConsentGuard implements PolicyGuard {

    @Override
    public Mono<Void> enforce(PolicyContext context) {
        if (!context.hasConsent()) {
            return Mono.error(new PolicyViolationException(
                "Consent required for this operation"
            ));
        }
        return Mono.empty();
    }
}
```

### LEI Validation Guard

Validates Legal Entity Identifiers:

```java
@Component
public class LeiGuard implements PolicyGuard {

    private final LeiValidationService leiService;

    @Override
    public Mono<Void> enforce(PolicyContext context) {
        if (context.lei() == null) {
            return Mono.empty(); // LEI not required for all operations
        }

        return leiService.validate(context.lei())
            .flatMap(valid -> {
                if (!valid) {
                    return Mono.error(new PolicyViolationException(
                        "Invalid LEI: " + context.lei()
                    ));
                }
                return Mono.empty();
            });
    }
}
```

## Custom Guards

### Creating a Custom Guard

```java
@Component
public class BusinessHoursGuard implements PolicyGuard {

    @Override
    public Mono<Void> enforce(PolicyContext context) {
        LocalTime now = LocalTime.now(ZoneId.of("Europe/London"));
        LocalTime start = LocalTime.of(8, 0);
        LocalTime end = LocalTime.of(18, 0);

        if (now.isBefore(start) || now.isAfter(end)) {
            return Mono.error(new PolicyViolationException(
                "Service available 8am-6pm UK time only"
            ));
        }
        return Mono.empty();
    }

    @Override
    public int getOrder() {
        return 100; // Lower numbers execute first
    }
}
```

### Rate Limiting Guard

```java
@Component
public class RateLimitGuard implements PolicyGuard {

    private final RateLimiter rateLimiter;

    public RateLimitGuard(
            @Value("${regulus.policy.rate-limit.requests-per-minute:100}")
            int requestsPerMinute) {
        this.rateLimiter = RateLimiter.create(requestsPerMinute / 60.0);
    }

    @Override
    public Mono<Void> enforce(PolicyContext context) {
        if (!rateLimiter.tryAcquire()) {
            return Mono.error(new PolicyViolationException(
                "Rate limit exceeded"
            ));
        }
        return Mono.empty();
    }
}
```

### Content Classification Guard

```java
@Component
public class ContentClassificationGuard implements PolicyGuard {

    private final ContentClassifier classifier;
    private final Set<String> blockedCategories;

    @Override
    public Mono<Void> enforce(PolicyContext context) {
        return classifier.classify(context.content())
            .flatMap(categories -> {
                Set<String> violations = categories.stream()
                    .filter(blockedCategories::contains)
                    .collect(Collectors.toSet());

                if (!violations.isEmpty()) {
                    return Mono.error(new PolicyViolationException(
                        "Content blocked: " + violations
                    ));
                }
                return Mono.empty();
            });
    }
}
```

## Composing Guards

### Guard Chain

```java
@Configuration
public class PolicyConfig {

    @Bean
    public PolicyGuard compositeGuard(List<PolicyGuard> guards) {
        return new CompositeGuard(guards);
    }
}

public class CompositeGuard implements PolicyGuard {

    private final List<PolicyGuard> guards;

    @Override
    public Mono<Void> enforce(PolicyContext context) {
        return Flux.fromIterable(guards)
            .sort(Comparator.comparingInt(PolicyGuard::getOrder))
            .concatMap(guard -> guard.enforce(context))
            .then();
    }
}
```

### Conditional Guards

```java
@Component
public class ConditionalGuard implements PolicyGuard {

    private final HighRiskGuard highRiskGuard;
    private final StandardGuard standardGuard;

    @Override
    public Mono<Void> enforce(PolicyContext context) {
        if (context.riskLevel() == RiskLevel.HIGH) {
            return highRiskGuard.enforce(context);
        }
        return standardGuard.enforce(context);
    }
}
```

## Testing Guards

```java
@SpringBootTest
class PolicyGuardTest {

    @Autowired
    private PolicyGuard policyGuard;

    @Test
    void shouldRejectMissingPurposeCode() {
        PolicyContext context = PolicyContext.builder()
            .hasConsent(true)
            // No purpose code
            .build();

        StepVerifier.create(policyGuard.enforce(context))
            .expectError(PolicyViolationException.class)
            .verify();
    }

    @Test
    void shouldAcceptValidContext() {
        PolicyContext context = PolicyContext.builder()
            .purposeCode("CUSTOMER_SUPPORT")
            .hasConsent(true)
            .userId("user123")
            .build();

        StepVerifier.create(policyGuard.enforce(context))
            .verifyComplete();
    }
}
```

## Metrics and Monitoring

Policy guard metrics are automatically collected:

- `regulus.policy.enforcements.total` - Total enforcement attempts
- `regulus.policy.violations.total` - Violations by type
- `regulus.policy.latency` - Enforcement latency

```promql
# Violation rate
rate(regulus_policy_violations_total[5m])

# Most common violations
topk(5, sum by (violation_type) (regulus_policy_violations_total))
```

## Best Practices

1. **Fail closed** - If a guard cannot determine compliance, reject the request
2. **Log all violations** - Maintain audit trail for regulatory evidence
3. **Order guards efficiently** - Put fast, cheap guards first
4. **Test edge cases** - Ensure guards handle null values, empty strings
5. **Monitor guard latency** - Guards shouldn't significantly impact response time
