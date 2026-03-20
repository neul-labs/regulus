# Custom Policy Guards

Creating custom policy guards for specialized compliance requirements.

## Overview

While Regulus provides built-in policy guards, you may need custom guards for specific business rules or regulatory requirements. This guide covers implementing custom guards.

## Guard Interface

```java
public interface PolicyGuard {

    /**
     * Enforce the policy on the given context.
     * @param context The policy context
     * @return Empty Mono if allowed, error Mono if violated
     */
    Mono<Void> enforce(PolicyContext context);

    /**
     * Get the execution order (lower = earlier).
     * @return Order value
     */
    default int getOrder() {
        return 0;
    }

    /**
     * Get the guard name for logging.
     * @return Guard name
     */
    default String getName() {
        return getClass().getSimpleName();
    }
}
```

## Simple Custom Guard

### Time-Based Access Control

```java
@Component
public class BusinessHoursGuard implements PolicyGuard {

    private static final ZoneId UK_ZONE = ZoneId.of("Europe/London");
    private static final LocalTime OPEN = LocalTime.of(8, 0);
    private static final LocalTime CLOSE = LocalTime.of(18, 0);

    @Override
    public Mono<Void> enforce(PolicyContext context) {
        LocalTime now = LocalTime.now(UK_ZONE);

        if (now.isBefore(OPEN) || now.isAfter(CLOSE)) {
            return Mono.error(new PolicyViolationException(
                "Service available 8am-6pm UK time only"
            ));
        }

        return Mono.empty();
    }

    @Override
    public int getOrder() {
        return 10; // Execute early
    }
}
```

### Customer Segment Guard

```java
@Component
public class CustomerSegmentGuard implements PolicyGuard {

    private final CustomerService customerService;
    private final Set<String> allowedSegments;

    public CustomerSegmentGuard(
            CustomerService customerService,
            @Value("${regulus.policy.allowed-segments}") Set<String> segments) {
        this.customerService = customerService;
        this.allowedSegments = segments;
    }

    @Override
    public Mono<Void> enforce(PolicyContext context) {
        String customerId = context.userId();

        if (customerId == null) {
            return Mono.error(new PolicyViolationException("Customer ID required"));
        }

        return customerService.getSegment(customerId)
            .flatMap(segment -> {
                if (!allowedSegments.contains(segment)) {
                    return Mono.error(new PolicyViolationException(
                        "Service not available for segment: " + segment
                    ));
                }
                return Mono.empty();
            });
    }
}
```

## Async Guards

### External Service Validation

```java
@Component
public class FraudCheckGuard implements PolicyGuard {

    private final FraudDetectionService fraudService;
    private final CircuitBreaker circuitBreaker;

    @Override
    public Mono<Void> enforce(PolicyContext context) {
        return Mono.defer(() -> circuitBreaker.run(
            fraudService.checkRisk(context.userId())
                .flatMap(risk -> {
                    if (risk.score() > 0.8) {
                        return Mono.error(new PolicyViolationException(
                            "High fraud risk detected"
                        ));
                    }
                    return Mono.empty();
                }),
            throwable -> {
                // Fallback: allow if fraud service unavailable
                log.warn("Fraud service unavailable, allowing request");
                return Mono.empty();
            }
        ));
    }

    @Override
    public int getOrder() {
        return 100; // Execute later (expensive check)
    }
}
```

### Rate Limiting Guard

```java
@Component
public class PerUserRateLimitGuard implements PolicyGuard {

    private final ReactiveRedisTemplate<String, String> redis;
    private final int maxRequests;
    private final Duration window;

    public PerUserRateLimitGuard(
            ReactiveRedisTemplate<String, String> redis,
            @Value("${regulus.policy.rate-limit.max-requests:100}") int maxRequests,
            @Value("${regulus.policy.rate-limit.window:1m}") Duration window) {
        this.redis = redis;
        this.maxRequests = maxRequests;
        this.window = window;
    }

    @Override
    public Mono<Void> enforce(PolicyContext context) {
        String key = "ratelimit:" + context.userId();

        return redis.opsForValue().increment(key)
            .flatMap(count -> {
                if (count == 1) {
                    // First request, set expiry
                    return redis.expire(key, window).thenReturn(count);
                }
                return Mono.just(count);
            })
            .flatMap(count -> {
                if (count > maxRequests) {
                    return Mono.error(new PolicyViolationException(
                        "Rate limit exceeded: " + maxRequests + " per " + window
                    ));
                }
                return Mono.empty();
            });
    }
}
```

## Conditional Guards

### Purpose-Based Guard

```java
@Component
public class FinancialAdviceGuard implements PolicyGuard {

    private final Set<String> advicePurposeCodes = Set.of(
        "INVESTMENT_ADVICE",
        "PENSION_ADVICE",
        "MORTGAGE_ADVICE"
    );

    @Override
    public Mono<Void> enforce(PolicyContext context) {
        if (!advicePurposeCodes.contains(context.purposeCode())) {
            return Mono.empty(); // Not applicable
        }

        // Additional checks for financial advice
        if (!context.hasQualifiedAdviser()) {
            return Mono.error(new PolicyViolationException(
                "Financial advice requires qualified adviser supervision"
            ));
        }

        if (!context.hasRiskAssessment()) {
            return Mono.error(new PolicyViolationException(
                "Risk assessment required for financial advice"
            ));
        }

        return Mono.empty();
    }
}
```

### Environment-Aware Guard

```java
@Component
@Profile("prod")
public class ProductionOnlyGuard implements PolicyGuard {

    @Override
    public Mono<Void> enforce(PolicyContext context) {
        // Only enforced in production
        if (!context.hasMandatoryDisclosure()) {
            return Mono.error(new PolicyViolationException(
                "Mandatory disclosure required in production"
            ));
        }
        return Mono.empty();
    }
}
```

## Composing Guards

### Guard Chain

```java
@Configuration
public class PolicyGuardConfig {

    @Bean
    @Primary
    public PolicyGuard compositeGuard(List<PolicyGuard> guards) {
        return new CompositeGuard(guards);
    }
}

public class CompositeGuard implements PolicyGuard {

    private final List<PolicyGuard> guards;

    public CompositeGuard(List<PolicyGuard> guards) {
        this.guards = guards.stream()
            .sorted(Comparator.comparingInt(PolicyGuard::getOrder))
            .toList();
    }

    @Override
    public Mono<Void> enforce(PolicyContext context) {
        return Flux.fromIterable(guards)
            .concatMap(guard -> guard.enforce(context)
                .doOnSubscribe(s -> log.debug("Enforcing {}", guard.getName()))
                .doOnError(e -> log.debug("{} violated: {}",
                    guard.getName(), e.getMessage()))
            )
            .then();
    }
}
```

### Conditional Composition

```java
@Component
public class TieredGuard implements PolicyGuard {

    private final List<PolicyGuard> standardGuards;
    private final List<PolicyGuard> enhancedGuards;

    @Override
    public Mono<Void> enforce(PolicyContext context) {
        List<PolicyGuard> applicableGuards =
            context.riskLevel() == RiskLevel.HIGH
                ? enhancedGuards
                : standardGuards;

        return Flux.fromIterable(applicableGuards)
            .concatMap(guard -> guard.enforce(context))
            .then();
    }
}
```

## Testing Custom Guards

```java
@SpringBootTest
class BusinessHoursGuardTest {

    @Autowired
    private BusinessHoursGuard guard;

    @Test
    void shouldAllowDuringBusinessHours() {
        // Mock time to be during business hours
        try (MockedStatic<LocalTime> mocked = mockStatic(LocalTime.class)) {
            mocked.when(() -> LocalTime.now(any()))
                .thenReturn(LocalTime.of(12, 0));

            PolicyContext context = PolicyContext.builder().build();

            StepVerifier.create(guard.enforce(context))
                .verifyComplete();
        }
    }

    @Test
    void shouldRejectOutsideBusinessHours() {
        try (MockedStatic<LocalTime> mocked = mockStatic(LocalTime.class)) {
            mocked.when(() -> LocalTime.now(any()))
                .thenReturn(LocalTime.of(22, 0));

            PolicyContext context = PolicyContext.builder().build();

            StepVerifier.create(guard.enforce(context))
                .expectError(PolicyViolationException.class)
                .verify();
        }
    }
}
```

## Metrics

Add custom metrics to guards:

```java
@Component
public class MeteredGuard implements PolicyGuard {

    private final PolicyGuard delegate;
    private final Counter enforcements;
    private final Counter violations;
    private final Timer latency;

    public MeteredGuard(PolicyGuard delegate, MeterRegistry registry) {
        this.delegate = delegate;
        this.enforcements = registry.counter(
            "regulus.policy.guard.enforcements",
            "guard", delegate.getName()
        );
        this.violations = registry.counter(
            "regulus.policy.guard.violations",
            "guard", delegate.getName()
        );
        this.latency = registry.timer(
            "regulus.policy.guard.latency",
            "guard", delegate.getName()
        );
    }

    @Override
    public Mono<Void> enforce(PolicyContext context) {
        return Mono.fromCallable(latency::start)
            .flatMap(sample -> delegate.enforce(context)
                .doOnSuccess(v -> {
                    enforcements.increment();
                    sample.stop(latency);
                })
                .doOnError(e -> {
                    enforcements.increment();
                    violations.increment();
                    sample.stop(latency);
                })
            );
    }
}
```

## Best Practices

1. **Keep guards focused** - One responsibility per guard
2. **Order by cost** - Cheap/fast guards first
3. **Handle failures gracefully** - Use circuit breakers for external calls
4. **Log appropriately** - Debug logs for enforcement, warn for violations
5. **Test thoroughly** - Unit test all conditions
6. **Monitor performance** - Track latency and violation rates
